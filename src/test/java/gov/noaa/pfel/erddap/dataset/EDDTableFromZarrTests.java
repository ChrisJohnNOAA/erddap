package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.*;

import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import dev.zarr.zarrjava.store.FilesystemStore;
import dev.zarr.zarrjava.store.HttpStore;
import dev.zarr.zarrjava.store.S3Store;
import dev.zarr.zarrjava.store.Store;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import testDataset.Initialization;

class EDDTableFromZarrTests {

  @Test
  void testZarrAttributeConversion() {
    dev.zarr.zarrjava.core.Attributes zattrs = new dev.zarr.zarrjava.core.Attributes();
    zattrs.set("title", "Test Table Zarr Dataset");
    zattrs.set("summary", "Summary of test dataset");
    zattrs.set("value", 123.45);
    zattrs.set("count", 50);
    zattrs.set("isActive", true);

    Attributes erddapAtts = new Attributes();
    EDDTableFromZarr.populateAttributesFromZarr(zattrs, erddapAtts);

    assertEquals("Test Table Zarr Dataset", erddapAtts.getString("title"));
    assertEquals("Summary of test dataset", erddapAtts.getString("summary"));
    assertEquals(123.45, erddapAtts.getDouble("value"), 1e-6);
    assertEquals(50, erddapAtts.getInt("count"));
    assertEquals("true", erddapAtts.getString("isActive"));
  }

  @Test
  void testCreateZarrStoreTypes() throws Exception {
    Store localStore = EDDTableFromZarr.createZarrStore("/tmp/test.zarr", null, null);
    assertInstanceOf(FilesystemStore.class, localStore);

    Store httpStore = EDDTableFromZarr.createZarrStore("https://example.com/test.zarr", null, null);
    assertInstanceOf(HttpStore.class, httpStore);

    Store s3Store = EDDTableFromZarr.createZarrStore("s3://mybucket/test.zarr", "us-west-2", null);
    assertInstanceOf(S3Store.class, s3Store);
  }

  @Test
  void testFromXmlParsingAndConstructor() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_table_test_store");
    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      String xml =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<dataset type=\"EDDTableFromZarr\" datasetID=\"test_table_zarr\">\n"
              + "  <zarrStorePath>"
              + tempDir.toString()
              + "</zarrStorePath>\n"
              + "  <zarrGroupName></zarrGroupName>\n"
              + "  <rowDimensionName>obs</rowDimensionName>\n"
              + "  <reloadEveryNMinutes>60</reloadEveryNMinutes>\n"
              + "  <addAttributes>\n"
              + "    <att name=\"title\">Test Zarr Table</att>\n"
              + "    <att name=\"summary\">A test zarr table dataset.</att>\n"
              + "    <att name=\"institution\">NOAA</att>\n"
              + "    <att name=\"infoUrl\">https://erddap.github.io</att>\n"
              + "    <att name=\"cdm_data_type\">Other</att>\n"
              + "  </addAttributes>\n"
              + "  <dataVariable>\n"
              + "    <sourceName>temp</sourceName>\n"
              + "    <destinationName>temperature</destinationName>\n"
              + "    <dataType>float</dataType>\n"
              + "    <addAttributes>\n"
              + "      <att name=\"ioos_category\">Temperature</att>\n"
              + "      <att name=\"units\">degree_C</att>\n"
              + "    </addAttributes>\n"
              + "  </dataVariable>\n"
              + "</dataset>";

      SimpleXMLReader xmlReader =
          new SimpleXMLReader(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "dataset");

      EDDTableFromZarr dataset = EDDTableFromZarr.fromXml(null, xmlReader);
      assertNotNull(dataset);
      assertEquals("test_table_zarr", dataset.datasetID());
      assertEquals(tempDir.toString(), dataset.zarrStorePath());
      assertEquals("", dataset.zarrGroupName());
      assertEquals("obs", dataset.rowDimensionName());
      assertNotNull(dataset.zarrStore());
      assertNotNull(dataset.zarrGroup());
    } finally {
      File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testTableAutoDiscoveryAndDSG() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_table_autodiscovery_test");
    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;
      dev.zarr.zarrjava.v3.DataType uint16 = dev.zarr.zarrjava.v3.DataType.UINT16;

      // 1D Time, Lat, Lon arrays
      dev.zarr.zarrjava.core.Attributes timeZattrs = new dev.zarr.zarrjava.core.Attributes();
      timeZattrs.set("units", "seconds since 1970-01-01T00:00:00Z");
      timeZattrs.set("standard_name", "time");

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("time"),
              mb -> mb.withShape(5).withDataType(float64).withDimensionNames("obs").withAttributes(timeZattrs),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {5}, new double[] {100.0, 101.0, 102.0, 103.0, 104.0}));

      dev.zarr.zarrjava.core.Attributes latZattrs = new dev.zarr.zarrjava.core.Attributes();
      latZattrs.set("units", "degrees_north");
      latZattrs.set("standard_name", "latitude");

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("latitude"),
              mb -> mb.withShape(5).withDataType(float64).withDimensionNames("obs").withAttributes(latZattrs),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {5}, new double[] {30.0, 31.0, 32.0, 33.0, 34.0}));

      dev.zarr.zarrjava.core.Attributes lonZattrs = new dev.zarr.zarrjava.core.Attributes();
      lonZattrs.set("units", "degrees_east");
      lonZattrs.set("standard_name", "longitude");

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("longitude"),
              mb -> mb.withShape(5).withDataType(float64).withDimensionNames("obs").withAttributes(lonZattrs),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {5}, new double[] {-120.0, -121.0, -122.0, -123.0, -124.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("salinity"),
              mb -> mb.withShape(5).withDataType(uint16).withDimensionNames("obs"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.SHORT, new int[] {5}, new short[] {35, 35, 36, 36, 35}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("station_id"),
              mb -> mb.withShape(5).withDataType(dev.zarr.zarrjava.v3.DataType.INT8).withDimensionNames("obs"),
              true);

      LocalizedAttributes globalAtts = new LocalizedAttributes();
      globalAtts.set(0, "title", "DSG Test");
      globalAtts.set(0, "summary", "DSG Test Summary");
      globalAtts.set(0, "institution", "NOAA");
      globalAtts.set(0, "infoUrl", "https://example.org");
      globalAtts.set(0, "featureType", "TimeSeries");

      EDDTableFromZarr dataset =
          new EDDTableFromZarr(
              "dsg_auto_discovery",
              null,
              null,
              new StringArray(),
              null,
              null,
              null,
              null,
              null,
              null,
              globalAtts,
              null, // auto-discovery
              60,
              tempDir.toString(),
              "",
              "obs",
              -1,
              null,
              null);

      assertNotNull(dataset);
      assertEquals("TimeSeries", dataset.cdmDataType(0));
      assertTrue(dataset.lonIndex() >= 0);
      assertTrue(dataset.latIndex() >= 0);
      assertTrue(dataset.timeIndex() >= 0);

      EDV salVar = dataset.findDataVariableByDestinationName("salinity");
      assertNotNull(salVar);
      assertEquals("true", salVar.sourceAttributes().getString("_Unsigned"));

      EDV stationVar = dataset.findDataVariableByDestinationName("station_id");
      assertNotNull(stationVar);
      assertEquals("timeseries_id", stationVar.combinedAttributes().getString(0, "cf_role"));

    } finally {
      File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testXmlOverridesAnd2DStringArray() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_table_xml_2dstring_test");
    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("temp"),
              mb -> mb.withShape(4).withDataType(float64).withDimensionNames("row"),
              true);

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("station_name"),
              mb -> mb.withShape(4, 16).withDataType(dev.zarr.zarrjava.v3.DataType.INT8).withDimensionNames("row", "str_len"),
              true);

      // Auto-discovery test with 2D string array
      LocalizedAttributes globalAtts = new LocalizedAttributes();
      globalAtts.set(0, "title", "2D String Test");
      globalAtts.set(0, "summary", "Test 2D string mapping");
      globalAtts.set(0, "institution", "NOAA");
      globalAtts.set(0, "infoUrl", "https://example.org");
      globalAtts.set(0, "cdm_data_type", "Other");

      EDDTableFromZarr autoDataset =
          new EDDTableFromZarr(
              "2d_string_auto",
              null,
              null,
              new StringArray(),
              null,
              null,
              null,
              null,
              null,
              null,
              globalAtts,
              null,
              60,
              tempDir.toString(),
              "",
              "row",
              -1,
              null,
              null);

      assertNotNull(autoDataset);
      EDV stationNameVar = autoDataset.findDataVariableByDestinationName("station_name");
      assertNotNull(stationNameVar);

      // XML override test
      List<DataVariableInfo> xmlDvis = new ArrayList<>();
      LocalizedAttributes tempAddAtts = new LocalizedAttributes();
      tempAddAtts.set(0, "units", "degree_C");
      xmlDvis.add(new DataVariableInfo("temp", "temperature", tempAddAtts, "float"));

      EDDTableFromZarr xmlDataset =
          new EDDTableFromZarr(
              "xml_override_test",
              null,
              null,
              new StringArray(),
              null,
              null,
              null,
              null,
              null,
              null,
              globalAtts,
              xmlDvis,
              60,
              tempDir.toString(),
              "",
              "row",
              -1,
              null,
              null);

      assertNotNull(xmlDataset);
      EDV tempVar = xmlDataset.findDataVariableByDestinationName("temperature");
      assertNotNull(tempVar);
      assertEquals("degree_C", tempVar.combinedAttributes().getString(0, "units"));

    } finally {
      File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }
}
