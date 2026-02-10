package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.ByteArray;
import com.cohort.array.PAOne;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.parquet.ParquetWriterBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import testDataset.Initialization;

class TableParquetBooleansTest {

  @TempDir Path tempDir;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  @org.junit.jupiter.api.Test
  void testParquetBooleans() throws Exception {
    String fileName = tempDir.resolve("testBooleans.parquet").toString();

    // Define schema with boolean columns
    String schemaString =
        "message test {\n"
            + "  required binary id (UTF8);\n"
            + "  required boolean bool_col;\n"
            + "  optional boolean bool_opt;\n"
            + "}";
    MessageType schema = MessageTypeParser.parseMessageType(schemaString);

    // Write a Parquet file with boolean data using standard Parquet example writer
    Configuration conf = new Configuration();
    try (ParquetWriter<Group> writer =
        ExampleParquetWriter.builder(new LocalOutputFile(java.nio.file.Path.of(fileName)))
            .withType(schema)
            .withConf(conf)
            .build()) {
      SimpleGroupFactory factory = new SimpleGroupFactory(schema);

      // Row 0: id="r0", bool_col=true, bool_opt=false
      writer.write(factory.newGroup().append("id", "r0").append("bool_col", true).append("bool_opt", false));

      // Row 1: id="r1", bool_col=false, bool_opt=true
      writer.write(factory.newGroup().append("id", "r1").append("bool_col", false).append("bool_opt", true));

      // Row 2: id="r2", bool_col=true, bool_opt is missing (null)
      writer.write(factory.newGroup().append("id", "r2").append("bool_col", true));
    }

    // Read back with ERDDAP Table
    Table table = new Table();
    table.readParquet(fileName, null, null, true);

    // Verify results
    // ERDDAP should have converted booleans to bytes: 1=true, 0=false, missing value=127 (empty string in CSV)
    String results = table.dataToString();
    String expected =
        "id,bool_col,bool_opt\n"
            + "r0,1,0\n"
            + "r1,0,1\n"
            + "r2,1,\n";
    Test.ensureEqual(results, expected, "Initial read results=\n" + results);

    // Verify column types are ByteArray
    Test.ensureTrue(table.getColumn("bool_col") instanceof ByteArray, "bool_col type");
    Test.ensureTrue(table.getColumn("bool_opt") instanceof ByteArray, "bool_opt type");

    // Verify raw values for null
    ByteArray boolOpt = (ByteArray) table.getColumn("bool_opt");
    Test.ensureEqual(boolOpt.get(2), (byte) 127, "raw value for null boolean");

    // NOW: Test writing back to Parquet using ERDDAP's CustomWriteSupport
    String roundTripFileName = tempDir.resolve("testBooleansRoundTrip.parquet").toString();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("column_names", "id,bool_col,bool_opt");
    metadata.put("column_units", ",,");

    try (ParquetWriter<List<PAOne>> writer =
        new ParquetWriterBuilder(
                schema,
                new LocalOutputFile(java.nio.file.Path.of(roundTripFileName)),
                metadata)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withConf(new Configuration())
            .build()) {

      for (int row = 0; row < table.nRows(); row++) {
        ArrayList<PAOne> record = new ArrayList<>();
        for (int col = 0; col < table.nColumns(); col++) {
          record.add(table.getPAOneData(col, row));
        }
        writer.write(record);
      }
    }

    // VERIFY THE PARQUET FILE SCHEMA DIRECTLY
    try (ParquetFileReader reader = ParquetFileReader.open(new LocalInputFile(java.nio.file.Path.of(roundTripFileName)))) {
        MessageType rtSchema = reader.getFileMetaData().getSchema();
        Test.ensureEqual(rtSchema.getType("bool_col").asPrimitiveType().getPrimitiveTypeName(), PrimitiveTypeName.BOOLEAN, "rtSchema bool_col");
        Test.ensureEqual(rtSchema.getType("bool_opt").asPrimitiveType().getPrimitiveTypeName(), PrimitiveTypeName.BOOLEAN, "rtSchema bool_opt");
    }

    // Read round-trip file back
    Table table2 = new Table();
    table2.readParquet(roundTripFileName, null, null, true);
    String roundTripResults = table2.dataToString();
    Test.ensureEqual(roundTripResults, expected, "Round-trip read results=\n" + roundTripResults);

    // Final check that types are still ByteArray
    Test.ensureTrue(table2.getColumn("bool_col") instanceof ByteArray, "bool_col type round-trip");
    Test.ensureTrue(table2.getColumn("bool_opt") instanceof ByteArray, "bool_opt type round-trip");
  }
}
