/*
 * EDDTableFromZarr Copyright 2026, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;
import dev.zarr.zarrjava.ZarrException;
import dev.zarr.zarrjava.core.Array;
import dev.zarr.zarrjava.core.ArrayMetadata;
import dev.zarr.zarrjava.core.Group;
import dev.zarr.zarrjava.core.Node;
import dev.zarr.zarrjava.store.FilesystemStore;
import dev.zarr.zarrjava.store.HttpStore;
import dev.zarr.zarrjava.store.ReadOnlyZipStore;
import dev.zarr.zarrjava.store.S3Store;
import dev.zarr.zarrjava.store.Store;
import dev.zarr.zarrjava.store.StoreHandle;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.handlers.EDDTableFromZarrHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import gov.noaa.pfel.erddap.variable.EDVDepth;
import gov.noaa.pfel.erddap.variable.EDVLat;
import gov.noaa.pfel.erddap.variable.EDVLon;
import gov.noaa.pfel.erddap.variable.EDVTime;
import gov.noaa.pfel.erddap.variable.EDVTimeStamp;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * This class represents a tabular dataset backed by a Zarr store using the zarr-java library.
 *
 * @author ERDDAP Development Team
 */
@SaxHandlerClass(EDDTableFromZarrHandler.class)
public class EDDTableFromZarr extends EDDTable {

  // Private instance variables for Zarr configuration and handles
  private String zarrStorePath;
  private String zarrGroupName;
  private String rowDimensionName;
  private long chunkCacheSize;
  private String awsRegion;
  private String awsEndpoint;

  private Store zarrStore;
  private Group zarrGroup;
  private long numRows = -1;

  /**
   * Static factory method to construct an EDDTableFromZarr dataset from an XML configuration.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader SimpleXMLReader pointing to the dataset XML element
   * @return EDDTableFromZarr dataset instance
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromZarr fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromZarr(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    LocalizedAttributes tGlobalAttributes = null;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tSosOfferingPrefix = null;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    String tAddVariablesWhere = null;
    ArrayList<DataVariableInfo> tDataVariables = new ArrayList<>();
    int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    String tZarrStorePath = null;
    String tZarrGroupName = "";
    String tRowDimensionName = null;
    long tChunkCacheSize = -1;
    String tAwsRegion = null;
    String tAwsEndpoint = null;

    int startOfTagsN = xmlReader.stackSize();
    String startOfTags = xmlReader.allTags();
    int startOfTagsLength = startOfTags.length();

    while (true) {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      String content = xmlReader.content();
      if (xmlReader.stackSize() == startOfTagsN) break;
      String localTags = tags.substring(startOfTagsLength);

      switch (localTags) {
        case "<addAttributes>" -> tGlobalAttributes = getAttributesFromXml(xmlReader);
        case "<dataVariable>" -> tDataVariables.add(getSDADVariableFromXml(xmlReader));
        case "<accessibleTo>",
            "<graphsAccessibleTo>",
            "<onChange>",
            "<fgdcFile>",
            "<iso19115File>",
            "<sosOfferingPrefix>",
            "<defaultDataQuery>",
            "<defaultGraphQuery>",
            "<addVariablesWhere>",
            "<reloadEveryNMinutes>",
            "<zarrStorePath>",
            "<sourceUrl>",
            "<zarrGroupName>",
            "<groupName>",
            "<rowDimensionName>",
            "<rowDimension>",
            "<chunkCacheSize>",
            "<awsRegion>",
            "<awsEndpoint>" -> {}
        case "</accessibleTo>" -> tAccessibleTo = content;
        case "</graphsAccessibleTo>" -> tGraphsAccessibleTo = content;
        case "</onChange>" -> tOnChange.add(content);
        case "</fgdcFile>" -> tFgdcFile = content;
        case "</iso19115File>" -> tIso19115File = content;
        case "</sosOfferingPrefix>" -> tSosOfferingPrefix = content;
        case "</defaultDataQuery>" -> tDefaultDataQuery = content;
        case "</defaultGraphQuery>" -> tDefaultGraphQuery = content;
        case "</addVariablesWhere>" -> tAddVariablesWhere = content;
        case "</reloadEveryNMinutes>" -> tReloadEveryNMinutes = String2.parseInt(content);
        case "</zarrStorePath>", "</sourceUrl>" -> tZarrStorePath = content;
        case "</zarrGroupName>", "</groupName>" -> tZarrGroupName = content;
        case "</rowDimensionName>", "</rowDimension>" -> tRowDimensionName = content;
        case "</chunkCacheSize>" -> tChunkCacheSize = String2.parseLong(content);
        case "</awsRegion>" -> tAwsRegion = content;
        case "</awsEndpoint>" -> tAwsEndpoint = content;
        default -> xmlReader.unexpectedTagException();
      }
    }

    return new EDDTableFromZarr(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tGlobalAttributes,
        tDataVariables,
        tReloadEveryNMinutes,
        tZarrStorePath,
        tZarrGroupName,
        tRowDimensionName,
        tChunkCacheSize,
        tAwsRegion,
        tAwsEndpoint);
  }

  /**
   * Primary constructor for EDDTableFromZarr.
   *
   * @param tDatasetID unique dataset ID
   * @param tAccessibleTo roles allowed to access
   * @param tGraphsAccessibleTo roles allowed to access graphs
   * @param tOnChange onChange triggers
   * @param tFgdcFile FGDC metadata file path
   * @param tIso19115File ISO 19115 metadata file path
   * @param tSosOfferingPrefix SOS offering prefix
   * @param tDefaultDataQuery default data query string
   * @param tDefaultGraphQuery default graph query string
   * @param tAddVariablesWhere addVariablesWhere CSV string
   * @param tAddGlobalAttributes attributes to add to source global attributes
   * @param tDataVariables list of data variables
   * @param tReloadEveryNMinutes reload interval in minutes
   * @param tZarrStorePath Zarr store URI or file path
   * @param tZarrGroupName Zarr subgroup path
   * @param tRowDimensionName name of main row dimension
   * @param tChunkCacheSize chunk cache size
   * @param tAwsRegion AWS region name
   * @param tAwsEndpoint AWS custom endpoint
   * @throws Throwable if initialization fails
   */
  public EDDTableFromZarr(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      String tAddVariablesWhere,
      LocalizedAttributes tAddGlobalAttributes,
      List<DataVariableInfo> tDataVariables,
      int tReloadEveryNMinutes,
      String tZarrStorePath,
      String tZarrGroupName,
      String tRowDimensionName,
      long tChunkCacheSize,
      String tAwsRegion,
      String tAwsEndpoint)
      throws Throwable {

    super();

    if (verbose) String2.log("\n*** constructing EDDTableFromZarr " + tDatasetID);
    String errorInMethod = "Error in EDDTableFromZarr(" + tDatasetID + ") constructor:\n";
    int language = 0;

    className = "EDDTableFromZarr";
    datasetID = tDatasetID;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    if (tAddGlobalAttributes == null) tAddGlobalAttributes = new LocalizedAttributes();
    addGlobalAttributes = tAddGlobalAttributes;
    setReloadEveryNMinutes(tReloadEveryNMinutes);

    sourceCanConstrainNumericData = CONSTRAIN_NO;
    sourceCanConstrainStringData = CONSTRAIN_NO;
    sourceCanConstrainStringRegex = "";

    this.zarrStorePath = tZarrStorePath;
    this.zarrGroupName = tZarrGroupName == null ? "" : tZarrGroupName.trim();
    this.rowDimensionName = tRowDimensionName;
    this.chunkCacheSize = tChunkCacheSize;
    this.awsRegion = tAwsRegion;
    this.awsEndpoint = tAwsEndpoint;

    if (!String2.isSomething(this.zarrStorePath)) {
      throw new IllegalArgumentException(errorInMethod + "zarrStorePath wasn't specified.");
    }

    addGlobalAttributes.set(language, "sourceUrl", convertToPublicSourceUrl(this.zarrStorePath));
    localSourceUrl = this.zarrStorePath;

    // 1. Initialize zarr-java Store reader for local, HTTP, or S3 URIs
    try {
      this.zarrStore = createZarrStore(this.zarrStorePath, this.awsRegion, this.awsEndpoint);
    } catch (Exception e) {
      throw new RuntimeException(
          errorInMethod + "Failed to initialize Zarr store at path: " + this.zarrStorePath, e);
    }

    // 2. Open specified Zarr root or subgroup
    try {
      this.zarrGroup = openZarrGroup(this.zarrStore, this.zarrGroupName);
    } catch (Exception e) {
      throw new RuntimeException(
          errorInMethod
              + "Failed to open Zarr group '"
              + this.zarrGroupName
              + "' in store: "
              + this.zarrStorePath,
          e);
    }

    // 3. Extract global attributes into sourceGlobalAttributes and combinedGlobalAttributes
    sourceGlobalAttributes = new Attributes();
    try {
      dev.zarr.zarrjava.core.Attributes zattrs = this.zarrGroup.metadata().attributes();
      if (zattrs != null) {
        populateAttributesFromZarr(zattrs, sourceGlobalAttributes);
      }
    } catch (ZarrException ze) {
      String2.log(errorInMethod + "Warning: Could not read .zattrs metadata: " + ze.getMessage());
    }

    combinedGlobalAttributes =
        new LocalizedAttributes(addGlobalAttributes, sourceGlobalAttributes);
    String tLicense = combinedGlobalAttributes.getString(language, "license");
    if (tLicense != null) {
      combinedGlobalAttributes.set(
          language,
          "license",
          String2.replaceAll(tLicense, "[standard]", EDStatic.messages.standardLicense));
    }
    combinedGlobalAttributes.removeValue("\"null\"");

    // 4. Determine main row dimension name and array size
    try {
      determineRowDimensionAndSize();
    } catch (Exception e) {
      String2.log(errorInMethod + "Warning when determining row dimension: " + e.getMessage());
    }

    // 5. Initialize dataVariables if provided
    if (tDataVariables != null && !tDataVariables.isEmpty()) {
      int ndv = tDataVariables.size();
      dataVariables = new EDV[ndv];
      for (int dv = 0; dv < ndv; dv++) {
        DataVariableInfo dvi = tDataVariables.get(dv);
        String tSourceName = dvi.sourceName();
        String tDestName = dvi.destinationName();
        if (!String2.isSomething(tDestName)) tDestName = tSourceName;
        LocalizedAttributes tAddAtt = dvi.attributes();
        String tSourceType = dvi.dataType();
        Attributes tSourceAtt = new Attributes();

        try {
          if (zarrGroup != null) {
            StoreHandle childHandle = zarrGroup.storeHandle.resolve(tSourceName);
            Array arr = Array.open(childHandle);
            if (arr != null && arr.metadata() != null) {
              populateAttributesFromZarr(arr.metadata().attributes(), tSourceAtt);
            }
          }
        } catch (Exception e) {
          // ignore if array doesn't exist or metadata unreadable
        }

        if (EDV.LON_NAME.equals(tDestName)) {
          dataVariables[dv] =
              new EDVLon(
                  datasetID,
                  tSourceName,
                  tSourceAtt,
                  tAddAtt,
                  tSourceType,
                  PAOne.fromDouble(Double.NaN),
                  PAOne.fromDouble(Double.NaN));
          lonIndex = dv;
        } else if (EDV.LAT_NAME.equals(tDestName)) {
          dataVariables[dv] =
              new EDVLat(
                  datasetID,
                  tSourceName,
                  tSourceAtt,
                  tAddAtt,
                  tSourceType,
                  PAOne.fromDouble(Double.NaN),
                  PAOne.fromDouble(Double.NaN));
          latIndex = dv;
        } else if (EDV.ALT_NAME.equals(tDestName)) {
          dataVariables[dv] =
              new EDVAlt(
                  datasetID,
                  tSourceName,
                  tSourceAtt,
                  tAddAtt,
                  tSourceType,
                  PAOne.fromDouble(Double.NaN),
                  PAOne.fromDouble(Double.NaN));
          altIndex = dv;
        } else if (EDV.DEPTH_NAME.equals(tDestName)) {
          dataVariables[dv] =
              new EDVDepth(
                  datasetID,
                  tSourceName,
                  tSourceAtt,
                  tAddAtt,
                  tSourceType,
                  PAOne.fromDouble(Double.NaN),
                  PAOne.fromDouble(Double.NaN));
          depthIndex = dv;
        } else if (EDV.TIME_NAME.equals(tDestName)) {
          dataVariables[dv] =
              new EDVTime(datasetID, tSourceName, tSourceAtt, tAddAtt, tSourceType);
          timeIndex = dv;
        } else if (EDVTimeStamp.hasTimeUnits(language, tSourceAtt, tAddAtt)) {
          dataVariables[dv] =
              new EDVTimeStamp(
                  datasetID, tSourceName, tDestName, tSourceAtt, tAddAtt, tSourceType);
        } else {
          dataVariables[dv] =
              new EDV(datasetID, tSourceName, tDestName, tSourceAtt, tAddAtt, tSourceType);
        }
      }
    } else {
      dataVariables = new EDV[0];
    }

    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);
    ensureValid();
  }

  /**
   * Helper method to instantiate appropriate zarr-java Store for local, HTTP, or S3 URIs.
   *
   * @param path store URI or path
   * @param awsRegion AWS region name if applicable
   * @param awsEndpoint custom AWS S3 endpoint if applicable
   * @return initialized Store
   * @throws IOException if error
   */
  public static Store createZarrStore(String path, String awsRegion, String awsEndpoint)
      throws IOException {
    if (path == null) throw new IllegalArgumentException("Zarr store path cannot be null.");
    if (path.startsWith("http://") || path.startsWith("https://")) {
      return new HttpStore(path);
    } else if (path.startsWith("s3://") || path.startsWith("s3a://")) {
      String s3Path = path.substring(path.indexOf("://") + 3);
      int firstSlash = s3Path.indexOf('/');
      String bucket = firstSlash > 0 ? s3Path.substring(0, firstSlash) : s3Path;
      String keyPrefix = firstSlash > 0 ? s3Path.substring(firstSlash + 1) : "";

      S3ClientBuilder builder = S3Client.builder();
      if (String2.isSomething(awsRegion)) {
        builder.region(Region.of(awsRegion));
      }
      if (String2.isSomething(awsEndpoint)) {
        try {
          builder.endpointOverride(new java.net.URI(awsEndpoint));
        } catch (Exception e) {
          throw new IOException("Invalid awsEndpoint URI: " + awsEndpoint, e);
        }
      }
      S3Client s3Client = builder.build();
      return new S3Store(s3Client, bucket, keyPrefix);
    } else if (path.toLowerCase().endsWith(".zip")) {
      return new ReadOnlyZipStore(Paths.get(path));
    } else {
      return new FilesystemStore(Paths.get(path));
    }
  }

  /**
   * Opens the targeted Zarr root or sub-group.
   *
   * @param store initialized Zarr store
   * @param groupName sub-group path or empty string for root
   * @return opened Group handle
   * @throws Exception if group opening fails
   */
  public static Group openZarrGroup(Store store, String groupName) throws Exception {
    StoreHandle handle;
    if (groupName == null || groupName.isEmpty() || "/".equals(groupName.trim())) {
      handle = store.resolve();
      try {
        return Group.open(handle);
      } catch (Exception e) {
        if (store instanceof Store.ListableStore listable) {
          boolean rootHasZarrDescriptor =
              handle.resolve(".zgroup").exists()
                  || handle.resolve("zarr.json").exists()
                  || handle.resolve(".zarray").exists();
          if (!rootHasZarrDescriptor) {
            Set<String> topDirs = new HashSet<>();
            listable
                .listChildren()
                .forEach(
                    c -> {
                      String clean = c.startsWith("/") ? c.substring(1) : c;
                      int slash = clean.indexOf('/');
                      String top = slash > 0 ? clean.substring(0, slash) : clean;
                      if (!top.isEmpty()) topDirs.add(top);
                    });
            if (topDirs.size() == 1) {
              String singleChild = topDirs.iterator().next();
              StoreHandle childHandle = handle.resolve(singleChild);
              if (childHandle.resolve(".zgroup").exists()
                  || childHandle.resolve("zarr.json").exists()
                  || childHandle.resolve(".zarray").exists()) {
                return Group.open(childHandle);
              }
            }
          }
        }
        throw e;
      }
    } else {
      String cleanGroup = groupName.trim();
      if (cleanGroup.startsWith("/")) cleanGroup = cleanGroup.substring(1);
      if (cleanGroup.endsWith("/")) cleanGroup = cleanGroup.substring(0, cleanGroup.length() - 1);
      handle = store.resolve(cleanGroup);
      return Group.open(handle);
    }
  }

  /**
   * Populates ERDDAP Attributes object from Zarr metadata attributes map.
   *
   * @param zattrs Zarr attributes map
   * @param erddapAtts target ERDDAP Attributes object
   */
  public static void populateAttributesFromZarr(
      dev.zarr.zarrjava.core.Attributes zattrs, Attributes erddapAtts) {
    if (zattrs == null || erddapAtts == null) return;
    for (Map.Entry<String, Object> entry : zattrs.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value == null) continue;
      if (value instanceof String s) {
        erddapAtts.set(key, s);
      } else if (value instanceof Number n) {
        if (value instanceof Double || value instanceof Float) {
          erddapAtts.set(key, n.doubleValue());
        } else if (value instanceof Long) {
          erddapAtts.set(key, n.longValue());
        } else {
          erddapAtts.set(key, n.intValue());
        }
      } else if (value instanceof Boolean b) {
        erddapAtts.set(key, b ? "true" : "false");
      } else if (value instanceof List<?> list) {
        PrimitiveArray pa = PrimitiveArray.factory(list);
        erddapAtts.set(key, pa);
      } else if (value.getClass().isArray()) {
        PrimitiveArray pa = PrimitiveArray.factory(value);
        erddapAtts.set(key, pa);
      } else {
        erddapAtts.set(key, value.toString());
      }
    }
  }

  /**
   * Determines the main row dimension name and array size following the 3-step fallback priority.
   *
   * @throws Exception if error
   */
  private void determineRowDimensionAndSize() throws Exception {
    if (zarrGroup == null) return;

    Map<String, Long> dimToSizeMap = new LinkedHashMap<>();
    Map<String, Integer> dimFrequencyMap = new LinkedHashMap<>();
    Map<String, Long> array1DLengthMap = new LinkedHashMap<>();

    try {
      Node[] nodes = zarrGroup.listAsArray();
      if (nodes != null) {
        for (Node node : nodes) {
          if (node instanceof Array zarray) {
            ArrayMetadata meta = zarray.metadata();
            if (meta == null || meta.shape == null) continue;
            long[] shape = meta.shape;
            String[] dims = extractDimensionNames(meta, shape.length);
            String name = getArrayName(zarray);

            for (int d = 0; d < dims.length; d++) {
              String dimName = dims[d];
              long dimSize = d < shape.length ? shape[d] : 0;
              dimToSizeMap.putIfAbsent(dimName, dimSize);
              dimFrequencyMap.put(dimName, dimFrequencyMap.getOrDefault(dimName, 0) + 1);
            }

            if (shape.length == 1) {
              array1DLengthMap.put(name, shape[0]);
            }
          }
        }
      }
    } catch (Throwable t) {
      String2.log("Warning in determineRowDimensionAndSize: " + t.getMessage());
    }

    // Step 1: Explicit XML tag
    if (String2.isSomething(this.rowDimensionName)) {
      if (dimToSizeMap.containsKey(this.rowDimensionName)) {
        this.numRows = dimToSizeMap.get(this.rowDimensionName);
        return;
      }
    }

    // Step 2: Standard Conventions ("obs", "row", "time", "index", "record", "i")
    String[] standardCandidates = new String[] {"obs", "row", "time", "index", "record", "i"};
    for (String cand : standardCandidates) {
      if (dimToSizeMap.containsKey(cand)) {
        this.rowDimensionName = cand;
        this.numRows = dimToSizeMap.get(cand);
        return;
      }
    }

    // Step 3: Auto-Discovery
    if (!dimToSizeMap.isEmpty()) {
      String dominantDim = null;
      int maxFreq = -1;
      for (Map.Entry<String, Integer> entry : dimFrequencyMap.entrySet()) {
        if (entry.getValue() > maxFreq) {
          maxFreq = entry.getValue();
          dominantDim = entry.getKey();
        }
      }
      if (dominantDim != null) {
        this.rowDimensionName = dominantDim;
        this.numRows = dimToSizeMap.get(dominantDim);
        return;
      }
    }

    if (!array1DLengthMap.isEmpty()) {
      Map.Entry<String, Long> first1D = array1DLengthMap.entrySet().iterator().next();
      this.rowDimensionName = "obs";
      this.numRows = first1D.getValue();
      return;
    }

    this.numRows = 0;
  }

  private static String getArrayName(Array zarray) {
    if (zarray != null
        && zarray.storeHandle != null
        && zarray.storeHandle.keys != null
        && zarray.storeHandle.keys.length > 0) {
      return zarray.storeHandle.keys[zarray.storeHandle.keys.length - 1];
    }
    return "";
  }

  private static String[] extractDimensionNames(ArrayMetadata metadata, int rank) {
    if (metadata instanceof dev.zarr.zarrjava.v3.ArrayMetadata v3Meta) {
      if (v3Meta.dimensionNames != null && v3Meta.dimensionNames.length == rank) {
        boolean valid = true;
        for (String d : v3Meta.dimensionNames) {
          if (!String2.isSomething(d)) {
            valid = false;
            break;
          }
        }
        if (valid) return v3Meta.dimensionNames;
      }
    }

    try {
      dev.zarr.zarrjava.core.Attributes zattrs = metadata.attributes();
      if (zattrs != null && zattrs.containsKey("_ARRAY_DIMENSIONS")) {
        Object obj = zattrs.get("_ARRAY_DIMENSIONS");
        String[] dims = parseStringArrayObject(obj);
        if (dims != null && dims.length == rank) {
          return dims;
        }
      }
    } catch (Exception e) {
      // ignore
    }

    String[] defaultDims = new String[rank];
    for (int i = 0; i < rank; i++) {
      defaultDims[i] = "dim" + i;
    }
    return defaultDims;
  }

  private static String[] parseStringArrayObject(Object obj) {
    if (obj == null) return null;
    if (obj instanceof String[] sa) return sa;
    if (obj instanceof List<?> list) {
      String[] res = new String[list.size()];
      for (int i = 0; i < list.size(); i++) {
        Object item = list.get(i);
        res[i] = item != null ? item.toString().trim() : "";
      }
      return res;
    }
    return null;
  }

  /**
   * Discovers variables, 1D array column mappings, and CF Discrete Sampling Geometry (DSG) structure from Zarr metadata.
   *
   * @return Map of array names to metadata objects
   * @throws Throwable if error
   */
  public Map<String, Object> parseZarrMetadata() throws Throwable {
    // TODO (Prompt 2): Variable discovery and DSG column mapping
    return new LinkedHashMap<>();
  }

  /**
   * Core tabular query & constraint evaluator for EDDTableFromZarr.
   *
   * @param language the language code (0=English)
   * @param loggedInAs user authorization info
   * @param requestUrl requested URL
   * @param userDapQuery user DAP query
   * @param tableWriter destination TableWriter
   * @throws Throwable if error
   */
  @Override
  public void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      gov.noaa.pfel.erddap.dataset.TableWriter tableWriter)
      throws Throwable {
    // TODO (Prompt 3): Columnar chunk scanning and constraint evaluation
    throw new SimpleException("getDataForDapQuery is not yet implemented for EDDTableFromZarr.");
  }

  /**
   * Helper method for tabular query & constraint evaluation.
   *
   * @param userDapQuery user DAP query
   * @param loggedInAs user identity
   * @param tableWriter table writer output sink
   * @throws Throwable if error
   */
  public void getDataForQuery(
      String userDapQuery,
      String loggedInAs,
      gov.noaa.pfel.erddap.dataset.TableWriter tableWriter)
      throws Throwable {
    // TODO (Prompt 3): Columnar chunk scanning and constraint evaluation
    throw new SimpleException("getDataForQuery is not yet implemented for EDDTableFromZarr.");
  }

  // Getters for configuration and state
  public String zarrStorePath() {
    return zarrStorePath;
  }

  public String zarrGroupName() {
    return zarrGroupName;
  }

  public String rowDimensionName() {
    return rowDimensionName;
  }

  public long numRows() {
    return numRows;
  }

  public long chunkCacheSize() {
    return chunkCacheSize;
  }

  public String awsRegion() {
    return awsRegion;
  }

  public String awsEndpoint() {
    return awsEndpoint;
  }

  public Store zarrStore() {
    return zarrStore;
  }

  public Group zarrGroup() {
    return zarrGroup;
  }
}
