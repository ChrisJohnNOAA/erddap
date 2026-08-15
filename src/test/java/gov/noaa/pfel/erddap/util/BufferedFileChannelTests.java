package gov.noaa.pfel.erddap.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BufferedFileChannelTests {

  @Test
  void testConstructorNull() {
    assertThrows(IllegalArgumentException.class, () -> new BufferedFileChannel(null));
  }

  @Test
  void testWriteSmallAndFlush(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("test_small.bin");
    try (FileChannel fc = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         BufferedFileChannel bfc = new BufferedFileChannel(fc)) {
      assertEquals(fc, bfc.fileChannel());
      assertEquals(fc, bfc.getChannel());

      ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder());
      buf.putDouble(123.45);
      buf.putDouble(678.90);
      buf.flip();

      int written = bfc.write(buf);
      assertEquals(16, written);

      // Before flush, FileChannel size should be 0 because 16 bytes fit in 8 KB buffer
      assertEquals(0, fc.size());

      bfc.flush();
      assertEquals(16, fc.size());
    }

    // Read back and verify
    try (FileChannel fc = FileChannel.open(file, StandardOpenOption.READ)) {
      ByteBuffer readBuf = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder());
      fc.read(readBuf);
      readBuf.flip();
      assertEquals(123.45, readBuf.getDouble(), 1e-6);
      assertEquals(678.90, readBuf.getDouble(), 1e-6);
    }
  }

  @Test
  void testWriteLargeDirect(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("test_large.bin");
    int size = 16384; // 16 KB > 8 KB
    try (FileChannel fc = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         BufferedFileChannel bfc = new BufferedFileChannel(fc)) {

      ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
      for (int i = 0; i < size / 4; i++) {
        buf.putInt(i);
      }
      buf.flip();

      int written = bfc.write(buf);
      assertEquals(size, written);
      assertEquals(size, fc.size());
    }

    // Verify content
    try (FileChannel fc = FileChannel.open(file, StandardOpenOption.READ)) {
      ByteBuffer readBuf = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
      fc.read(readBuf);
      readBuf.flip();
      for (int i = 0; i < size / 4; i++) {
        assertEquals(i, readBuf.getInt());
      }
    }
  }

  @Test
  void testNullWriteBuffer(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("test_null.bin");
    try (FileChannel fc = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         BufferedFileChannel bfc = new BufferedFileChannel(fc)) {
      assertThrows(IllegalArgumentException.class, () -> bfc.write(null));
    }
  }
}
