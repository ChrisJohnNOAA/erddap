package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

public class EDDTableAggregateRowsDerivedTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  private static String getDerivedXml() throws Exception {
    String miniNdbcPath =
        Path.of(EDDTestDataset.class.getResource("/data/miniNdbc/").toURI()).toString();

    return "<dataset type=\"EDDTableAggregateRows\" datasetID=\"miniNdbcDerived\">\n"
        + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
        + "    <updateEveryNMillis>10</updateEveryNMillis>\n"
        + "    <addAttributes>\n"
        + "        <att name=\"title\">Aggregated NDBC Data</att>\n"
        + "        <att name=\"summary\">Aggregated NDBC Data Summary</att>\n"
        + "        <att name=\"cdm_data_type\">TimeSeries</att>\n"
        + "        <att name=\"cdm_timeseries_variables\">station,longitude,latitude</att>\n"
        + "        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n"
        + "        <att name=\"institution\">NOAA NDBC</att>\n"
        + "        <att name=\"sourceUrl\">(local files)</att>\n"
        + "    </addAttributes>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>station</sourceName>\n"
        + "        <destinationName>station</destinationName>\n"
        + "        <dataType>String</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Identifier</att>\n"
        + "            <att name=\"cf_role\">timeseries_id</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>longitude</sourceName>\n"
        + "        <destinationName>longitude</destinationName>\n"
        + "        <dataType>float</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Location</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>latitude</sourceName>\n"
        + "        <destinationName>latitude</destinationName>\n"
        + "        <dataType>float</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Location</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>time</sourceName>\n"
        + "        <destinationName>time</destinationName>\n"
        + "        <dataType>double</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Time</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>wspd</sourceName>\n"
        + "        <destinationName>wspd</destinationName>\n"
        + "        <dataType>float</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Wind</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>=row.columnDouble(\"wspd\")*2</sourceName>\n"
        + "        <destinationName>wspd2</destinationName>\n"
        + "        <dataType>float</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Wind</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>global:title</sourceName>\n"
        + "        <destinationName>global_title</destinationName>\n"
        + "        <dataType>String</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Identifier</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>variable:wspd:units</sourceName>\n"
        + "        <destinationName>wspd_units</destinationName>\n"
        + "        <dataType>String</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Identifier</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "\n"
        + "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"miniNdbc4102\">\n"
        + "    <fileDir>"
        + miniNdbcPath
        + "</fileDir>\n"
        + "    <fileNameRegex>NDBC_4102._met\\.nc</fileNameRegex>\n"
        + "    <metadataFrom>last</metadataFrom>\n"
        + "    <preExtractRegex>^NDBC_</preExtractRegex>\n"
        + "    <postExtractRegex>_met\\.nc$</postExtractRegex>\n"
        + "    <extractRegex>.*</extractRegex>\n"
        + "    <columnNameForExtract>station</columnNameForExtract>\n"
        + "    <addAttributes>\n"
        + "        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n"
        + "        <att name=\"institution\">NOAA NDBC</att>\n"
        + "        <att name=\"title\">NDBC 4102</att>\n"
        + "        <att name=\"summary\">NDBC 4102 Summary</att>\n"
        + "        <att name=\"cdm_data_type\">TimeSeries</att>\n"
        + "        <att name=\"cdm_timeseries_variables\">station,longitude,latitude</att>\n"
        + "        <att name=\"sourceUrl\">(local files)</att>\n"
        + "    </addAttributes>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>station</sourceName>\n"
        + "        <destinationName>station</destinationName>\n"
        + "        <dataType>String</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Identifier</att>\n"
        + "            <att name=\"cf_role\">timeseries_id</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>LON</sourceName>\n"
        + "        <destinationName>longitude</destinationName>\n"
        + "        <dataType>float</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"units\">degrees_east</att>\n"
        + "            <att name=\"standard_name\">longitude</att>\n"
        + "            <att name=\"ioos_category\">Location</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>LAT</sourceName>\n"
        + "        <destinationName>latitude</destinationName>\n"
        + "        <dataType>float</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"units\">degrees_north</att>\n"
        + "            <att name=\"standard_name\">latitude</att>\n"
        + "            <att name=\"ioos_category\">Location</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>TIME</sourceName>\n"
        + "        <destinationName>time</destinationName>\n"
        + "        <dataType>double</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
        + "            <att name=\"standard_name\">time</att>\n"
        + "            <att name=\"ioos_category\">Time</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>WSPD</sourceName>\n"
        + "        <destinationName>wspd</destinationName>\n"
        + "        <dataType>float</dataType>\n"
        + "        <addAttributes>\n"
        + "            <att name=\"units\">m/s</att>\n"
        + "            <att name=\"ioos_category\">Wind</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + "</dataset>\n"
        + "</dataset>\n";
  }

  @org.junit.jupiter.api.Test
  public void testDerivedVariables() throws Throwable {
    String xml = getDerivedXml();
    EDDTable tedd = (EDDTable) EDDTableAggregateRows.oneFromXmlFragment(null, xml);

    Test.ensureNotNull(tedd, "tedd is null");
    String dir = EDStatic.config.fullTestCacheDirectory;
    int language = 0;

    // Query derived variables
    String query = "station,time,wspd,wspd2,global_title,wspd_units";
    String tName =
        tedd.makeNewFileForDapQuery(language, null, null, query, dir, "derivedTest", ".csv");
    String results = File2.directReadFrom88591File(dir + tName);

    String2.log(results.substring(0, Math.min(results.length(), 1000)));

    String[] lines = results.split("\n");
    Test.ensureTrue(lines.length > 2, "Should have more than 2 lines. Results:\n" + results);

    String header = lines[0];
    String[] headerParts = header.split(",");

    int nToTest = Math.min(lines.length, 50);
    int validRowsChecked = 0;
    for (int i = 2; i < nToTest; i++) {
      String[] parts = lines[i].split(",");
      String wspdS = parts[Part.indexOf(headerParts, "wspd")];
      String wspd2S = parts[Part.indexOf(headerParts, "wspd2")];

      if (!wspdS.equals("NaN") && !wspd2S.equals("NaN")) {
        float wspd = Float.parseFloat(wspdS);
        float wspd2 = Float.parseFloat(wspd2S);
        Test.ensureEqual(wspd * 2.0f, wspd2, "wspd2 should be wspd * 2 at row " + i);
        validRowsChecked++;
      }

      Test.ensureEqual(
          parts[Part.indexOf(headerParts, "global_title")],
          "Aggregated NDBC Data",
          "global_title should match aggregate title");
      Test.ensureEqual(
          parts[Part.indexOf(headerParts, "wspd_units")], "m/s", "wspd_units should match");
    }
    Test.ensureTrue(validRowsChecked > 0, "Should have checked at least one valid row");
  }

  // Helper class/method for finding index in array
  private static class Part {
    public static int indexOf(String[] arr, String val) {
      for (int i = 0; i < arr.length; i++) {
        if (arr[i].equals(val)) return i;
      }
      return -1;
    }
  }
}
