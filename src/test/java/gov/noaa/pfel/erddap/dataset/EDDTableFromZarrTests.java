package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.*;

import com.cohort.array.Attributes;
import com.cohort.util.File2;
import dev.zarr.zarrjava.store.FilesystemStore;
import dev.zarr.zarrjava.store.HttpStore;
import dev.zarr.zarrjava.store.S3Store;
import dev.zarr.zarrjava.store.Store;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
