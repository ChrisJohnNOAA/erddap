package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.*;

import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.SimpleException;
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
import java.nio.file.Paths;
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
  void testGenerateDatasetsXmlV2AndV3() throws Throwable {
    Initialization.edStatic();
    Path tempDirV2 = Files.createTempDirectory("zarr_v2_table_xml_test");
    Path tempDirV3 = Files.createTempDirectory("zarr_v3_table_xml_test");
    try {
      // Create Zarr v2 store
      Files.createDirectories(tempDirV2);
      Files.writeString(tempDirV2.resolve(".zgroup"), "{\"zarr_format\": 2}");
      Files.writeString(
          tempDirV2.resolve(".zattrs"),
          "{\"title\": \"V2 Test Table\", \"featureType\": \"trajectory\"}");

      Files.createDirectories(tempDirV2.resolve("salinity"));
      Files.writeString(
          tempDirV2.resolve("salinity/.zarray"),
          "{\"zarr_format\": 2, \"shape\": [5], \"chunks\": [5], \"dtype\": \"<f8\", \"compressor\": null, \"fill_value\": null, \"filters\": null, \"order\": \"C\"}");
      Files.writeString(
          tempDirV2.resolve("salinity/.zattrs"),
          "{\"units\": \"PSU\", \"standard_name\": \"sea_water_salinity\"}");

      String xmlV2 =
          EDDTableFromZarr.generateDatasetsXml(
              tempDirV2.toString(), "", "obs", "v2_pref", 60, null, null, null, null);
      assertNotNull(xmlV2);
      assertTrue(xmlV2.contains("<dataset type=\"EDDTableFromZarr\""));
      assertTrue(xmlV2.contains("datasetID=\"v2_pref_"));
      assertTrue(xmlV2.contains("<zarrStorePath>" + tempDirV2.toString() + "</zarrStorePath>"));
      assertTrue(xmlV2.contains("<rowDimensionName>obs</rowDimensionName>"));
      assertTrue(xmlV2.contains("<sourceName>salinity</sourceName>"));
      assertTrue(xmlV2.contains("cdm_data_type\">Trajectory</att>"));

      // Create Zarr v3 store
      dev.zarr.zarrjava.store.FilesystemStore storeV3 =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDirV3);
      dev.zarr.zarrjava.v3.Group.create(
          storeV3.resolve(),
          new dev.zarr.zarrjava.core.Attributes()
              .set("title", "V3 Test Table")
              .set("featureType", "point"));

      dev.zarr.zarrjava.v3.Array.create(
          storeV3.resolve("temp"),
          mb ->
              mb.withShape(5)
                  .withDataType(dev.zarr.zarrjava.v3.DataType.FLOAT64)
                  .withDimensionNames("obs"),
          true);

      String xmlV3 =
          EDDTableFromZarr.generateDatasetsXml(
              tempDirV3.toString(), "", "obs", "v3_pref", 60, null, null, null, null);
      assertNotNull(xmlV3);
      assertTrue(xmlV3.contains("<dataset type=\"EDDTableFromZarr\""));
      assertTrue(xmlV3.contains("<sourceName>temp</sourceName>"));
      assertTrue(xmlV3.contains("cdm_data_type\">Point</att>"));

    } finally {
      File2.deleteAllFiles(tempDirV2.toString(), true, true);
      File2.deleteAllFiles(tempDirV3.toString(), true, true);
    }
  }

  @Test
  void testSparseAndUnwrittenChunks() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_table_sparse_chunk_test");
    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      // 10 rows across 2 chunks of size 5
      // Write chunk 0 (rows 0-4), omit writing chunk 1 (rows 5-9) on disk
      dev.zarr.zarrjava.v3.Array arr =
          dev.zarr.zarrjava.v3.Array.create(
              store.resolve("temperature"),
              mb -> mb.withShape(10).withChunkShape(5).withDataType(float64).withDimensionNames("obs"),
              true);

      // Write chunk 0 only: [20.0, 21.0, 22.0, 23.0, 24.0]
      arr.write(
          new long[] {0},
          ucar.ma2.Array.factory(
              ucar.ma2.DataType.DOUBLE, new int[] {5}, new double[] {20.0, 21.0, 22.0, 23.0, 24.0}));

      LocalizedAttributes globalAtts = new LocalizedAttributes();
      globalAtts.set(0, "title", "Sparse Chunk Test");
      globalAtts.set(0, "summary", "Test sparse/unwritten chunks");
      globalAtts.set(0, "institution", "NOAA");
      globalAtts.set(0, "infoUrl", "https://example.org");
      globalAtts.set(0, "cdm_data_type", "Other");

      EDDTableFromZarr dataset =
          new EDDTableFromZarr(
              "sparse_chunk_test",
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
              "obs",
              -1,
              null,
              null);

      TableWriterAllWithMetadata twawm =
          new TableWriterAllWithMetadata(
              0,
              dataset,
              "",
              dataset.cacheDirectory(),
              "sparse_test.twawm");

      dataset.getDataForDapQuery(0, null, "", "temperature", twawm);
      gov.noaa.pfel.coastwatch.pointdata.Table resultTable = twawm.cumulativeTable();

      assertNotNull(resultTable);
      assertEquals(10, resultTable.nRows());

      // Rows 0-4 should be 20.0 to 24.0
      assertEquals(20.0, resultTable.getColumn("temperature").getDouble(0), 1e-5);
      assertEquals(24.0, resultTable.getColumn("temperature").getDouble(4), 1e-5);

      // Rows 5-9 (unwritten chunk) should return NaN / missing value
      assertTrue(Double.isNaN(resultTable.getColumn("temperature").getDouble(5)));
      assertTrue(Double.isNaN(resultTable.getColumn("temperature").getDouble(9)));

    } finally {
      File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testEndToEndTableWriterStreaming() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_table_streaming_test");
    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      dev.zarr.zarrjava.core.Attributes timeAtts = new dev.zarr.zarrjava.core.Attributes();
      timeAtts.set("units", "seconds since 1970-01-01T00:00:00Z");
      timeAtts.set("standard_name", "time");

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("time"),
              mb -> mb.withShape(3).withDataType(float64).withDimensionNames("obs").withAttributes(timeAtts),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {3}, new double[] {100.0, 101.0, 102.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("longitude"),
              mb -> mb.withShape(3).withDataType(float64).withDimensionNames("obs"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {3}, new double[] {-120.0, -121.0, -122.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("latitude"),
              mb -> mb.withShape(3).withDataType(float64).withDimensionNames("obs"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {3}, new double[] {30.0, 31.0, 32.0}));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("temperature"),
              mb -> mb.withShape(3).withDataType(float64).withDimensionNames("obs"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {3}, new double[] {15.0, 16.0, 17.0}));

      LocalizedAttributes globalAtts = new LocalizedAttributes();
      globalAtts.set(0, "title", "Streaming Test");
      globalAtts.set(0, "summary", "Test streaming table writers");
      globalAtts.set(0, "institution", "NOAA");
      globalAtts.set(0, "infoUrl", "https://example.org");
      globalAtts.set(0, "cdm_data_type", "Point");

      EDDTableFromZarr dataset =
          new EDDTableFromZarr(
              "streaming_test",
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
              "obs",
              -1,
              null,
              null);

      String testDir = File2.addSlash(tempDir.toString());

      // 1. CSV
      String csvFileName =
          dataset.makeNewFileForDapQuery(
              0, null, null, "longitude,latitude,time,temperature", testDir, "test_out", ".csv");
      Path csvPath = Paths.get(testDir, csvFileName);
      assertTrue(Files.exists(csvPath));
      String csvContent = Files.readString(csvPath);
      assertTrue(csvContent.contains("longitude,latitude,time,temperature"));
      assertTrue(csvContent.contains("-120.0"));
      assertTrue(csvContent.contains("30.0"));
      assertTrue(csvContent.contains("15.0"));

      // 2. JSON
      String jsonFileName =
          dataset.makeNewFileForDapQuery(
              0, null, null, "longitude,latitude,time,temperature", testDir, "test_out", ".json");
      Path jsonPath = Paths.get(testDir, jsonFileName);
      assertTrue(Files.exists(jsonPath));
      String jsonContent = Files.readString(jsonPath);
      assertTrue(jsonContent.contains("columnNames"));
      assertTrue(jsonContent.contains("-120"));

      // 3. HTML Table
      String htmlFileName =
          dataset.makeNewFileForDapQuery(
              0, null, null, "longitude,latitude,time,temperature", testDir, "test_out", ".htmlTable");
      Path htmlPath = Paths.get(testDir, htmlFileName);
      assertTrue(Files.exists(htmlPath));
      String htmlContent = Files.readString(htmlPath);
      assertTrue(htmlContent.toLowerCase().contains("<table"));
      assertTrue(htmlContent.contains("15.0"));

      // 4. NetCDF (.nc)
      String ncFileName =
          dataset.makeNewFileForDapQuery(
              0, null, null, "longitude,latitude,time,temperature", testDir, "test_out", ".nc");
      Path ncPath = Paths.get(testDir, ncFileName);
      assertTrue(Files.exists(ncPath));
      assertTrue(Files.size(ncPath) > 0);

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
      assertEquals("station_id", dataset.combinedGlobalAttributes().getString(0, "cdm_timeseries_variables"));
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

  @Test
  void testAuxiliaryAndMismatchedArrayFiltering() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_table_auxiliary_filtering_test");
    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      // 1D Time array (dataset row dimension = obs = 5)
      dev.zarr.zarrjava.core.Attributes timeZattrs = new dev.zarr.zarrjava.core.Attributes();
      timeZattrs.set("units", "seconds since 1970-01-01T00:00:00Z");
      timeZattrs.set("standard_name", "time");

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("time"),
              mb -> mb.withShape(5).withDataType(float64).withDimensionNames("obs").withAttributes(timeZattrs),
              true);

      // Salinity data variable
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("salinity"),
              mb -> mb.withShape(5).withDataType(float64).withDimensionNames("obs"),
              true);

      // Auxiliary bounds array (time_bnds)
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("time_bnds"),
              mb -> mb.withShape(5, 2).withDataType(float64).withDimensionNames("obs", "nv"),
              true);

      // Auxiliary QC array (salinity_qc)
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("salinity_qc"),
              mb -> mb.withShape(5).withDataType(dev.zarr.zarrjava.v3.DataType.INT8).withDimensionNames("obs"),
              true);

      // Auxiliary flags array (temp_flags)
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("temp_flags"),
              mb -> mb.withShape(5).withDataType(dev.zarr.zarrjava.v3.DataType.INT8).withDimensionNames("obs"),
              true);

      // Mismatched 1D array length (100 != 5)
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("unrelated_1d"),
              mb -> mb.withShape(100).withDataType(float64).withDimensionNames("other_dim"),
              true);

      LocalizedAttributes globalAtts = new LocalizedAttributes();
      globalAtts.set(0, "title", "Auxiliary Array Filtering Test");
      globalAtts.set(0, "summary", "Test auxiliary and mismatched array filtering");
      globalAtts.set(0, "institution", "NOAA");
      globalAtts.set(0, "infoUrl", "https://example.org");
      globalAtts.set(0, "cdm_data_type", "Other");

      EDDTableFromZarr dataset =
          new EDDTableFromZarr(
              "auxiliary_filtering_test",
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
      assertNotNull(dataset.findDataVariableByDestinationName("time"));
      assertNotNull(dataset.findDataVariableByDestinationName("salinity"));

      // Confirm auxiliary and mismatched arrays were filtered out
      String[] dvNames = dataset.dataVariableDestinationNames();
      assertTrue(com.cohort.util.String2.indexOf(dvNames, "time_bnds") < 0);
      assertTrue(com.cohort.util.String2.indexOf(dvNames, "salinity_qc") < 0);
      assertTrue(com.cohort.util.String2.indexOf(dvNames, "temp_flags") < 0);
      assertTrue(com.cohort.util.String2.indexOf(dvNames, "unrelated_1d") < 0);

    } finally {
      File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testNestedGroupDiscoveryAnd2DByteMatrixFiltering() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_table_nested_group_test");
    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group rootGroup = dev.zarr.zarrjava.v3.Group.create(store.resolve());
      dev.zarr.zarrjava.v3.Group.create(store.resolve("subgroup"));

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      // Array in root group
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("temp"),
              mb -> mb.withShape(5).withDataType(float64).withDimensionNames("obs"),
              true);

      // Array in nested child group
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("subgroup/salinity"),
              mb -> mb.withShape(5).withDataType(float64).withDimensionNames("obs"),
              true);

      // 2D Byte QC matrix (shape 5, 10, dimensions obs, level - NOT string length)
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("qc_matrix"),
              mb -> mb.withShape(5, 10).withDataType(dev.zarr.zarrjava.v3.DataType.INT8).withDimensionNames("obs", "level"),
              true);

      LocalizedAttributes globalAtts = new LocalizedAttributes();
      globalAtts.set(0, "title", "Nested Group Test");
      globalAtts.set(0, "summary", "Test nested group auto-discovery");
      globalAtts.set(0, "institution", "NOAA");
      globalAtts.set(0, "infoUrl", "https://example.org");
      globalAtts.set(0, "cdm_data_type", "Other");

      EDDTableFromZarr dataset =
          new EDDTableFromZarr(
              "nested_group_test",
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
      assertNotNull(dataset.findDataVariableByDestinationName("temp"));
      assertNotNull(dataset.findDataVariableByDestinationName("subgroup_salinity"));

      // 2D byte QC matrix without string length dimension should be filtered out
      String[] dvNames = dataset.dataVariableDestinationNames();
      assertTrue(com.cohort.util.String2.indexOf(dvNames, "qc_matrix") < 0);

    } finally {
      File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testGetDataForDapQuery() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_table_query_test");
    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;
      dev.zarr.zarrjava.v3.DataType int16 = dev.zarr.zarrjava.v3.DataType.INT16;

      // 1D Time, Lat, Lon arrays (shape = 5)
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

      // Salinity with scale_factor=0.1 and add_offset=30.0
      dev.zarr.zarrjava.core.Attributes salZattrs = new dev.zarr.zarrjava.core.Attributes();
      salZattrs.set("scale_factor", 0.1);
      salZattrs.set("add_offset", 30.0);

      // raw values: [50, 50, 60, 60, 50] -> unpacked: [35.0, 35.0, 36.0, 36.0, 35.0]
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("salinity"),
              mb -> mb.withShape(5).withDataType(int16).withDimensionNames("obs").withAttributes(salZattrs),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.SHORT, new int[] {5}, new short[] {50, 50, 60, 60, 50}));

      // 0D Scalar station_id
      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("station_id"),
              mb -> mb.withShape().withDataType(dev.zarr.zarrjava.v3.DataType.INT32),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.INT, new int[] {}, new int[] {999}));

      LocalizedAttributes globalAtts = new LocalizedAttributes();
      globalAtts.set(0, "title", "Query Test Dataset");
      globalAtts.set(0, "summary", "Query test summary");
      globalAtts.set(0, "institution", "NOAA");
      globalAtts.set(0, "infoUrl", "https://example.org");
      globalAtts.set(0, "cdm_data_type", "Other");

      EDDTableFromZarr dataset =
          new EDDTableFromZarr(
              "query_test_dataset",
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

      // Test 1: Query with constraint salinity >= 36.0
      // Expected rows: index 2 and 3 (unpacked salinity = 36.0)
      TableWriterAllWithMetadata twawm1 =
          new TableWriterAllWithMetadata(
              0,
              dataset,
              "",
              dataset.cacheDirectory(),
              "query_test1.twawm");

      dataset.getDataForDapQuery(0, null, "", "longitude,latitude,time,salinity,station_id&salinity>=36.0", twawm1);
      gov.noaa.pfel.coastwatch.pointdata.Table resultTable1 = twawm1.cumulativeTable();

      assertNotNull(resultTable1);
      assertEquals(2, resultTable1.nRows());
      assertEquals(36.0, resultTable1.getColumn("salinity").getDouble(0), 1e-5);
      assertEquals(36.0, resultTable1.getColumn("salinity").getDouble(1), 1e-5);
      assertEquals("999", resultTable1.getColumn("station_id").getString(0));
      assertEquals("999", resultTable1.getColumn("station_id").getString(1));

      // Test 2: Query with constraint matching 0 rows (TableWriterAll throws SimpleException on finish for 0 rows)
      TableWriterAllWithMetadata twawm2 =
          new TableWriterAllWithMetadata(
              0,
              dataset,
              "",
              dataset.cacheDirectory(),
              "query_test2.twawm");

      SimpleException se =
          assertThrows(
              SimpleException.class,
              () ->
                  dataset.getDataForDapQuery(
                      0, null, "", "longitude,latitude,salinity&salinity>100.0", twawm2));
      assertTrue(se.getMessage().contains("no matching results") || se.getMessage().contains("nRows = 0"));

    } finally {
      File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }

  @Test
  void testRenamedDestinationVariablesAndMultiChunkFlush() throws Throwable {
    Initialization.edStatic();
    Path tempDir = Files.createTempDirectory("zarr_table_renamed_vars_test");
    try {
      dev.zarr.zarrjava.store.FilesystemStore store =
          new dev.zarr.zarrjava.store.FilesystemStore(tempDir);
      dev.zarr.zarrjava.v3.Group.create(store.resolve());

      dev.zarr.zarrjava.v3.DataType float64 = dev.zarr.zarrjava.v3.DataType.FLOAT64;

      // 10 rows of data across 2 chunks
      double[] rawTemp = new double[] {15.0, 18.0, 21.0, 22.0, 25.0, 12.0, 14.0, 23.0, 24.0, 26.0};
      double[] rawSal = new double[] {34.0, 34.5, 35.0, 35.2, 35.5, 33.0, 33.5, 35.8, 36.0, 36.2};

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("raw_temp"),
              mb -> mb.withShape(10).withChunkShape(5).withDataType(float64).withDimensionNames("obs"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {10}, rawTemp));

      dev.zarr.zarrjava.v3.Array.create(
              store.resolve("raw_sal"),
              mb -> mb.withShape(10).withChunkShape(5).withDataType(float64).withDimensionNames("obs"),
              true)
          .write(
              ucar.ma2.Array.factory(
                  ucar.ma2.DataType.DOUBLE, new int[] {10}, rawSal));

      List<DataVariableInfo> dvis = new ArrayList<>();
      LocalizedAttributes tempAddAtts = new LocalizedAttributes();
      tempAddAtts.set(0, "units", "degree_C");
      tempAddAtts.set(0, "ioos_category", "Temperature");
      dvis.add(new DataVariableInfo("raw_temp", "temperature", tempAddAtts, "double"));

      LocalizedAttributes salAddAtts = new LocalizedAttributes();
      salAddAtts.set(0, "units", "PSU");
      salAddAtts.set(0, "ioos_category", "Salinity");
      dvis.add(new DataVariableInfo("raw_sal", "salinity", salAddAtts, "double"));

      LocalizedAttributes globalAtts = new LocalizedAttributes();
      globalAtts.set(0, "title", "Renamed Vars Test");
      globalAtts.set(0, "summary", "Test renamed variables and multi-chunk flush");
      globalAtts.set(0, "institution", "NOAA");
      globalAtts.set(0, "infoUrl", "https://example.org");
      globalAtts.set(0, "cdm_data_type", "Other");

      EDDTableFromZarr dataset =
          new EDDTableFromZarr(
              "renamed_vars_test",
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
              dvis,
              60,
              tempDir.toString(),
              "",
              "obs",
              -1,
              null,
              null);

      TableWriterAllWithMetadata twawm =
          new TableWriterAllWithMetadata(
              0,
              dataset,
              "",
              dataset.cacheDirectory(),
              "renamed_test.twawm");

      dataset.getDataForDapQuery(0, null, "", "temperature,salinity&temperature>=20.0", twawm);
      gov.noaa.pfel.coastwatch.pointdata.Table resultTable = twawm.cumulativeTable();

      assertNotNull(resultTable);
      // Expected matching rows: rawTemp >= 20.0 -> indices 2, 3, 4, 7, 8, 9 (6 matching rows)
      assertEquals(6, resultTable.nRows());

      // Verify destination column names
      assertEquals("temperature", resultTable.getColumnName(0));
      assertEquals("salinity", resultTable.getColumnName(1));

      // Verify column values
      assertEquals(21.0, resultTable.getColumn("temperature").getDouble(0), 1e-5);
      assertEquals(22.0, resultTable.getColumn("temperature").getDouble(1), 1e-5);
      assertEquals(25.0, resultTable.getColumn("temperature").getDouble(2), 1e-5);
      assertEquals(23.0, resultTable.getColumn("temperature").getDouble(3), 1e-5);

    } finally {
      File2.deleteAllFiles(tempDir.toString(), true, true);
    }
  }
}
