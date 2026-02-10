package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.ByteArray;
import com.cohort.util.Test;
import java.nio.file.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.io.LocalOutputFile;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.junit.jupiter.api.io.TempDir;
import testDataset.Initialization;

class TableParquetBooleansTest {

  @TempDir Path tempDir;

  @org.junit.jupiter.api.Test
  void testParquetBooleans() throws Exception {
    Initialization.edStatic();
    String fileName = tempDir.resolve("testBooleans.parquet").toString();

    // Define schema with boolean columns
    String schemaString =
        "message test {\n"
            + "  required binary id (UTF8);\n"
            + "  required boolean bool_col;\n"
            + "  optional boolean bool_opt;\n"
            + "}";
    MessageType schema = MessageTypeParser.parseMessageType(schemaString);

    // Write a Parquet file with boolean data
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
    // ERDDAP should have converted booleans to bytes: 1=true, 0=false, missing value=empty string
    String results = table.dataToString();
    String expected =
        "id,bool_col,bool_opt\n"
            + "r0,1,0\n"
            + "r1,0,1\n"
            + "r2,1,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // Verify column types are ByteArray
    Test.ensureTrue(table.getColumn("bool_col") instanceof ByteArray, "bool_col type");
    Test.ensureTrue(table.getColumn("bool_opt") instanceof ByteArray, "bool_opt type");

    // Verify raw values for null
    ByteArray boolOpt = (ByteArray) table.getColumn("bool_opt");
    Test.ensureEqual(boolOpt.get(2), (byte) 127, "raw value for null boolean");
  }
}
