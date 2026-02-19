package gov.noaa.pfel.erddap;

import static org.junit.jupiter.api.Assertions.assertFalse;

import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;

public class SideBySideReloadReproductionTest {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @Test
  @SuppressWarnings("DoNotCall")
  void testSbsReloadFailureReproduction() throws Throwable {
    String xml =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n"
            + "<erddapDatasets>\n"
            + "    <dataset type=\"EDDGridSideBySide\" datasetID=\"parentSbs\" active=\"true\">\n"
            + "        <dataset type=\"EDDGridFromNcFiles\" datasetID=\"child1\" active=\"true\">\n"
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
            + "        <dataset type=\"EDDGridFromEtopo\" datasetID=\"etopo180\" active=\"true\">\n"
            + "        </dataset>\n"
            + "    </dataset>\n"
            + "</erddapDatasets>\n";

    Erddap erddap = new Erddap();
    LoadDatasets loadDatasets =
        new LoadDatasets(
            erddap,
            EDStatic.config.datasetsRegex,
            new java.io.ByteArrayInputStream(
                xml.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1)),
            true);
    loadDatasets.run();

    // Check if etopo180 was added as a top-level dataset
    // It should NOT be there because it's a child of parentSbs.
    assertFalse(
        erddap.gridDatasetHashMap.containsKey("etopo180"),
        "etopo180 should NOT be in gridDatasetHashMap as a top-level dataset");
  }
}
