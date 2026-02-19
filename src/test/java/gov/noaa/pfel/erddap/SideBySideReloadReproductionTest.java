package gov.noaa.pfel.erddap;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.cohort.array.StringArray;
import gov.noaa.pfel.erddap.handlers.SaxHandler;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;

public class SideBySideReloadReproductionTest {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @Test
  void testSbsReloadFailureReproduction() throws Throwable {
    // Use unique IDs to avoid any potential conflict with other tests
    String suffix = "_" + System.currentTimeMillis();
    String parentId = "sbs_repro_parent" + suffix;
    String child1Id = "sbs_repro_child1" + suffix;
    String child2Id = "sbs_repro_child2" + suffix;

    String xml =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n"
            + "<erddapDatasets>\n"
            + "    <dataset type=\"EDDGridSideBySide\" datasetID=\""
            + parentId
            + "\" active=\"true\">\n"
            + "        <dataset type=\"EDDGridFromNcFiles\" datasetID=\""
            + child1Id
            + "\" active=\"true\">\n"
            + "            <altitudeMetersPerSourceUnit>1.0</altitudeMetersPerSourceUnit>\n"
            + "            <fileDir>src/test/resources/datasets/</fileDir>\n"
            + "            <fileNameRegex>.*\\.nc</fileNameRegex>\n"
            + "            <recursive>true</recursive>\n"
            + "            <pathRegex>.*</pathRegex>\n"
            + "            <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "            <updateEveryNMillis>0</updateEveryNMillis>\n"
            + "            <axisVariable>\n"
            + "                <sourceName>time</sourceName>\n"
            + "                <destinationName>time</destinationName>\n"
            + "            </axisVariable>\n"
            + "            <axisVariable>\n"
            + "                <sourceName>latitude</sourceName>\n"
            + "                <destinationName>latitude</destinationName>\n"
            + "            </axisVariable>\n"
            + "            <axisVariable>\n"
            + "                <sourceName>longitude</sourceName>\n"
            + "                <destinationName>longitude</destinationName>\n"
            + "            </axisVariable>\n"
            + "            <dataVariable>\n"
            + "                <sourceName>sst</sourceName>\n"
            + "                <destinationName>sst</destinationName>\n"
            + "                <dataType>float</dataType>\n"
            + "            </dataVariable>\n"
            + "        </dataset>\n"
            + "        <dataset type=\"EDDGridFromEtopo\" datasetID=\""
            + child2Id
            + "\" active=\"true\">\n"
            + "        </dataset>\n"
            + "    </dataset>\n"
            + "</erddapDatasets>\n";

    Erddap erddap = new Erddap();
    int[] nTryAndDatasets = new int[2];
    StringArray changedDatasetIDs = new StringArray();
    HashSet<String> orphanIDSet = new HashSet<>();
    HashSet<String> datasetIDSet = new HashSet<>();
    StringArray duplicateDatasetIDs = new StringArray();
    StringBuilder datasetsThatFailedToLoadSB = new StringBuilder();
    StringBuilder failedDatasetsWithErrorsSB = new StringBuilder();
    StringBuilder warningsFromLoadDatasets = new StringBuilder();
    HashMap<String, Object[]> tUserHashMap = new HashMap<>();

    SaxHandler.parse(
        new ByteArrayInputStream(xml.getBytes(StandardCharsets.ISO_8859_1)),
        nTryAndDatasets,
        changedDatasetIDs,
        orphanIDSet,
        datasetIDSet,
        duplicateDatasetIDs,
        datasetsThatFailedToLoadSB,
        failedDatasetsWithErrorsSB,
        warningsFromLoadDatasets,
        tUserHashMap,
        true, // majorLoad
        erddap,
        System.currentTimeMillis(),
        ".*", // datasetsRegex
        true // reallyVerbose
        );

    // Check if child2Id was added as a top-level dataset
    // It should NOT be there because it's a child of parentId and its sibling (child1Id) failed to
    // load
    // (Wait, child1Id might NOT fail if the file exists. Let's make sure it fails.)
    // In my previous run it failed because I gave it a non-existent fileDir or something?
    // src/test/resources/datasets/ should exist and have .nc files.

    // To ensure failure, let's use a non-existent fileDir for child1
    String failingXml = xml.replace("src/test/resources/datasets/", "/non/existent/path/");

    erddap = new Erddap(); // Reset erddap
    SaxHandler.parse(
        new ByteArrayInputStream(failingXml.getBytes(StandardCharsets.ISO_8859_1)),
        nTryAndDatasets,
        changedDatasetIDs,
        orphanIDSet,
        datasetIDSet,
        duplicateDatasetIDs,
        datasetsThatFailedToLoadSB,
        failedDatasetsWithErrorsSB,
        warningsFromLoadDatasets,
        tUserHashMap,
        true,
        erddap,
        System.currentTimeMillis(),
        ".*",
        true);

    assertFalse(
        erddap.gridDatasetHashMap.containsKey(child2Id),
        child2Id + " should NOT be in gridDatasetHashMap as a top-level dataset");
  }
}
