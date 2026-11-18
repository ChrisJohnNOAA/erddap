package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.*;

import com.cohort.array.LongArray;
import com.cohort.array.StringArray;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatasetHistoryTests {

  @TempDir Path tempDir;

  private Table sampleFileTable(long lastMod, long size) {
    Table t = new Table();
    t.addColumn("directory", new StringArray(new String[] {"/data/"}));
    t.addColumn("name", new StringArray(new String[] {"obs_2026.nc"}));
    t.addColumn("lastModified", new LongArray(new long[] {lastMod}));
    t.addColumn("size", new LongArray(new long[] {size}));
    return t;
  }

  @Test
  void preservesTimestampAcrossCacheWipesAndNoOpReloads() throws Exception {
    EDStatic.config.bigParentDirectory = tempDir.toString() + "/";
    String id = "test_sst_dataset";

    LocalizedAttributes atts = new LocalizedAttributes();
    atts.set(EDMessages.DEFAULT_LANGUAGE, "title", "SST Observations");

    Table ft1 = sampleFileTable(1_700_000_000_000L, 4096L);
    String fp1 = DatasetHistory.computeFingerprint(atts, null, ft1);

    // 1. Initial load uses file's max lastModified
    DatasetHistory.Record r1 = DatasetHistory.reconcile(id, fp1, 1_700_000_000_000L);
    assertEquals(1_700_000_000_000L, r1.lastActualEditMillis());

    // 2. Simulated server restart + cleared fileTable.nc cache with unchanged files
    String fp2 =
        DatasetHistory.computeFingerprint(atts, null, sampleFileTable(1_700_000_000_000L, 4096L));
    DatasetHistory.Record r2 = DatasetHistory.reconcile(id, fp2, System.currentTimeMillis());
    assertEquals(r1.lastActualEditMillis(), r2.lastActualEditMillis());

    // 3. Metadata edit in datasets.xml advances lastActualEditMillis
    Thread.sleep(15);
    atts.set(EDMessages.DEFAULT_LANGUAGE, "summary", "Updated QC description");
    String fp3 = DatasetHistory.computeFingerprint(atts, null, ft1);
    DatasetHistory.Record r3 = DatasetHistory.reconcile(id, fp3, 0L);
    assertTrue(r3.lastActualEditMillis() > r1.lastActualEditMillis());
  }
}
