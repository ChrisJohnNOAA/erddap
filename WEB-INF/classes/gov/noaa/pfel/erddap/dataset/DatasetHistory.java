package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PrimitiveArray;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tracks durable dataset state fingerprints and "last actual edit" timestamps outside the volatile
 * dataset cache directory.
 */
public class DatasetHistory {

  private static final int SCHEMA_VERSION = 1;
  private static final ConcurrentHashMap<String, Object> DATASET_LOCKS = new ConcurrentHashMap<>();

  public record Record(
      String datasetID, String fingerprint, long lastActualEditMillis, String lastActualEditIso) {}

  /** Resolves the durable history file path outside bigParentDirectory/cache/. */
  public static Path historyFilePath(String datasetID) {
    String safeId = String2.modifyToBeFileNameSafe(datasetID);
    return Paths.get(EDStatic.config.bigParentDirectory, "history", safeId, "datasetHistory.json");
  }

  /**
   * Reconciles the freshly loaded dataset state against durable history. Updates
   * lastActualEditMillis only if the canonical state fingerprint changed.
   */
  public static Record reconcile(
      String datasetID, String newFingerprint, long fallbackInitialEditMillis) {
    Object lock = DATASET_LOCKS.computeIfAbsent(datasetID, k -> new Object());
    synchronized (lock) {
      Path file = historyFilePath(datasetID);
      JSONObject existing = readJsonQuietly(file);
      long nowMillis = System.currentTimeMillis();

      if (existing != null) {
        String savedFingerprint = existing.optString("fingerprint", "");
        long savedEditMillis = existing.optLong("lastActualEditMillis", 0L);

        // No-op reload or cold start after cache wipe with identical underlying state
        if (!savedFingerprint.isEmpty()
            && savedFingerprint.equals(newFingerprint)
            && savedEditMillis > 0L) {
          String iso = existing.optString("lastActualEditIso", formatIsoUtc(savedEditMillis));
          return new Record(datasetID, savedFingerprint, savedEditMillis, iso);
        }

        // Actual edit detected
        String iso = formatIsoUtc(nowMillis);
        JSONArray events = existing.optJSONArray("events");
        if (events == null) {
          events = new JSONArray();
        }
        writeJsonAtomic(file, datasetID, newFingerprint, nowMillis, iso, events);
        return new Record(datasetID, newFingerprint, nowMillis, iso);
      }

      // First time this dataset has been recorded
      long initialMillis = fallbackInitialEditMillis > 0L ? fallbackInitialEditMillis : nowMillis;
      String iso = formatIsoUtc(initialMillis);
      writeJsonAtomic(file, datasetID, newFingerprint, initialMillis, iso, new JSONArray());
      return new Record(datasetID, newFingerprint, initialMillis, iso);
    }
  }

  /**
   * Builds a deterministic SHA-256 fingerprint across public metadata, variable bounds, and
   * (optionally) sorted fileTable inventory rows.
   */
  public static String computeFingerprint(
      LocalizedAttributes globalAttributes, EDV[] dataVariables, Table fileTable) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");

      // 1. Canonical global attributes (sorted by key)
      updateDigestWithAttributes(md, "GLOBAL", globalAttributes);

      // 2. Canonical data variables (order, type, attributes, actual_range)
      if (dataVariables != null) {
        for (EDV edv : dataVariables) {
          if (edv == null) continue;
          updateString(md, "VAR:" + edv.sourceName() + "->" + edv.destinationName());
          updateString(md, "TYPE:" + edv.destinationDataType());
          updateDigestWithAttributes(
              md, "VAR_ATTR:" + edv.destinationName(), edv.combinedAttributes());

          // Captures data changes for non-file datasets (SQL, OPeNDAP, remote ERDDAP)
          if (edv.destinationMin() != null && edv.destinationMax() != null) {
            updateString(md, "RANGE:" + edv.destinationMin() + ".." + edv.destinationMax());
          }
        }
      }

      // 3. Canonical fileTable rows (sorted by directory + name)
      if (fileTable != null && fileTable.nRows() > 0) {
        appendSortedFileTable(md, fileTable);
      }

      return HexFormat.of().formatHex(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable in JVM", e);
    }
  }

  private static void appendSortedFileTable(MessageDigest md, Table fileTable) {
    int dirCol = fileTable.findColumnNumber("directory");
    int nameCol = fileTable.findColumnNumber("name");
    int modCol = fileTable.findColumnNumber("lastModified");
    int sizeCol = fileTable.findColumnNumber("size");
    if (nameCol < 0 || modCol < 0 || sizeCol < 0) {
      return;
    }

    int nRows = fileTable.nRows();
    List<String> canonicalRows = new ArrayList<>(nRows);
    for (int r = 0; r < nRows; r++) {
      String dir = dirCol >= 0 ? fileTable.getStringData(dirCol, r) : "";
      String name = fileTable.getStringData(nameCol, r);
      long lastMod = fileTable.getLongData(modCol, r);
      long size = fileTable.getLongData(sizeCol, r);
      canonicalRows.add(dir + "\t" + name + "\t" + lastMod + "\t" + size);
    }
    canonicalRows.sort(Comparator.naturalOrder());

    updateString(md, "FILE_COUNT:" + nRows);
    for (String row : canonicalRows) {
      updateString(md, row);
    }
  }

  private static void updateDigestWithAttributes(
      MessageDigest md, String scope, LocalizedAttributes atts) {
    updateString(md, scope);
    if (atts == null) {
      return;
    }
    Map<String, PrimitiveArray> attrMap = atts.toMap(EDMessages.DEFAULT_LANGUAGE);
    String[] names = attrMap.keySet().stream().toArray(String[]::new);
    Arrays.sort(names);
    for (String name : names) {
      // Skip attributes that ERDDAP may dynamically stamp on every load
      if ("date_created".equals(name)) {
        continue;
      }
      PrimitiveArray pa = atts.get(EDMessages.DEFAULT_LANGUAGE, name);
      updateString(md, name + "=" + (pa == null ? "" : pa.toString()));
    }
  }

  private static void updateString(MessageDigest md, String value) {
    md.update(value.getBytes(StandardCharsets.UTF_8));
    md.update((byte) '\n');
  }

  public static String formatIsoUtc(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis).truncatedTo(ChronoUnit.SECONDS).toString();
  }

  public static String formatRfc1123(long epochMillis) {
    return DateTimeFormatter.RFC_1123_DATE_TIME.format(
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC));
  }

  private static JSONObject readJsonQuietly(Path file) {
    try {
      if (!Files.exists(file)) {
        return null;
      }
      String content = Files.readString(file, StandardCharsets.UTF_8);
      return new JSONObject(content);
    } catch (Exception e) {
      String2.log("Warning: failed to read " + file + ": " + MustBe.throwableToString(e));
      return null;
    }
  }

  private static void writeJsonAtomic(
      Path target,
      String datasetID,
      String fingerprint,
      long lastActualEditMillis,
      String lastActualEditIso,
      JSONArray events) {
    try {
      Path parent = target.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      JSONObject json = new JSONObject();
      json.put("schemaVersion", SCHEMA_VERSION);
      json.put("datasetID", datasetID);
      json.put("fingerprint", fingerprint);
      json.put("lastActualEditMillis", lastActualEditMillis);
      json.put("lastActualEditIso", lastActualEditIso);
      json.put("events", events);

      Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
      Files.writeString(tmp, json.toString(2), StandardCharsets.UTF_8);
      Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (Exception e) {
      String2.log("Warning: failed to write " + target + ": " + MustBe.throwableToString(e));
    }
  }
}
