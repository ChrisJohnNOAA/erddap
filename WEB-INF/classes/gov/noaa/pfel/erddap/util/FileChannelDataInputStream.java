/*
 * FileChannelDataInputStream Copyright 2025, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.util.String2;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * FileChannelDataInputStream extends DataInputStream to wrap an underlying FileChannel.
 *
 * <p>This wrapper is used for temp cache files written with native-endian PrimitiveArray channel
 * writes. The wrapper itself stays synchronized with the underlying channel, and numeric array
 * readers use the channel fast path so they can decode values in native byte order without going
 * through DataInputStream's always-big-endian primitive methods.
 */
public class FileChannelDataInputStream extends DataInputStream {

  private final FileChannel channel;

  /**
   * Constructs a FileChannelDataInputStream wrapping the specified FileChannel.
   *
   * @param channel the FileChannel to read from
   */
  public FileChannelDataInputStream(FileChannel channel) {
    super(
        new InputStream() {
          @Override
          public int read() throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            int bytesRead = channel.read(buffer);
            if (bytesRead < 0) return -1;
            buffer.flip();
            return buffer.get() & 0xFF;
          }

          @Override
          public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            ByteBuffer buffer = ByteBuffer.wrap(b, off, len);
            int bytesRead = channel.read(buffer);
            return bytesRead < 0 ? -1 : bytesRead;
          }

          @Override
          public int available() throws IOException {
            return (int) Math.max(0, channel.size() - channel.position());
          }

          @Override
          public void close() throws IOException {
            channel.close();
          }
        });
    if (channel == null) {
      throw new IllegalArgumentException(
          String2.ERROR + " in FileChannelDataInputStream constructor: FileChannel is null.");
    }
    this.channel = channel;
  }

  /**
   * Returns the underlying FileChannel.
   *
   * @return the underlying FileChannel
   */
  public FileChannel getChannel() {
    return channel;
  }
}
