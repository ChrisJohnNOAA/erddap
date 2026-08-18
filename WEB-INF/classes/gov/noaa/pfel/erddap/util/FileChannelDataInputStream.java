/*
 * FileChannelDataInputStream Copyright 2025, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.util.String2;
import java.io.DataInputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

/**
 * FileChannelDataInputStream extends DataInputStream to wrap an underlying FileChannel.
 * PrimitiveArray.readDis detects instances of this class and delegates to
 * PrimitiveArray.readFromChannel for high-performance native byte order reading.
 */
public class FileChannelDataInputStream extends DataInputStream {

  private final FileChannel channel;

  /**
   * Constructs a FileChannelDataInputStream wrapping the specified FileChannel.
   *
   * @param channel the FileChannel to read from
   */
  public FileChannelDataInputStream(FileChannel channel) {
    super(Channels.newInputStream(channel));
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
