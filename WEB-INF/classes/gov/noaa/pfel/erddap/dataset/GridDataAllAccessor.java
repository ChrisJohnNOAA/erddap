/*
 * GridDataAllAccessor Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.DataInputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * This class gets all of the grid data requested by a grid data query to an EDDGrid and makes it
 * accessible one variable at a time as a PrimitiveArray or FileChannel. This works with all
 * data types (even Strings).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2010-09-03
 */
public class GridDataAllAccessor implements AutoCloseable {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  // things passed into the constructor
  protected final GridDataAccessor gridDataAccessor;

  // things the constructor sets
  public String baseFileName; // to which the dv number is added
  protected PAType dataPAType[]; // 1 per data variable  e.g., float.class

  /**
   * This sets everything up (i.e., gets all the data and stores it in Files).
   *
   * @param tGridDataAccessor a rowMajor gridDataAccessor
   * @throws Throwable if trouble
   */
  public GridDataAllAccessor(GridDataAccessor tGridDataAccessor) throws Throwable {

    int nDv = 0;
    FileChannel channels[] = null;
    gridDataAccessor = tGridDataAccessor;
    try {
      if (!gridDataAccessor.rowMajor())
        throw new Exception(
            "GridDataAllAccessor.constructor requires the gridDataAccessor to be rowMajor.");

      // make the dataFiles
      // This is set up to delete the cached files when the creator/owner is done.
      // It could be changed to keep the files in the cache (which is cleared periodically).
      EDV dataVars[] = gridDataAccessor.dataVariables();
      nDv = dataVars.length;
      String tQuery = gridDataAccessor.userDapQuery();
      baseFileName =
          gridDataAccessor.eddGrid().cacheDirectory()
              + // dir created by EDD.ensureValid
              String2.md5Hex12(tQuery == null ? "" : tQuery)
              + "_"
              + Math2.random(Integer.MAX_VALUE)
              + "_"; // so two identical queries don't interfere with each other

      dataPAType = new PAType[nDv];
      channels = new FileChannel[nDv]; // 1 per data variable
      for (int dv = 0; dv < nDv; dv++) {
        dataPAType[dv] = dataVars[dv].destinationDataPAType();
        channels[dv] =
            FileChannel.open(
                Paths.get(baseFileName + dv),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
      }

      // get all the data
      while (gridDataAccessor.incrementChunk()) {
        for (int dv = 0; dv < nDv; dv++) {
          gridDataAccessor.getPartialDataValues(dv).writeToChannel(channels[dv]);
        }
      }
    } finally {
      if (channels != null) {
        for (int dv = 0; dv < nDv; dv++)
          try {
            if (channels[dv] != null) channels[dv].close();
          } catch (Exception e) {
          }
      }
      gridDataAccessor.close();
    }
  }

  /**
   * Get all of the destination values for one dataVariable as a PrimitiveArray. Note that may
   * require a lot of memory!
   *
   * @param dv a dataVariable number (within the request, not the EDD dataVariable number).
   * @return a PrimitiveArray
   * @throws Exception if trouble, e.g., if gdaTotalIndex.size() is &gt;=
   *     Integer.MAX_VALUE.
   */
  public PrimitiveArray getPrimitiveArray(int dv) throws Exception {
    long n = gridDataAccessor.totalIndex.size();
    Math2.ensureArraySizeOkay(n, "GridDataAllAccessor");
    PrimitiveArray pa = PrimitiveArray.factory(dataPAType[dv], (int) n, false);
    try (FileChannel channel = FileChannel.open(Paths.get(baseFileName + dv), StandardOpenOption.READ)) {
      pa.readFromChannel(channel, (int) n);
    }
    return pa;
  }

  /**
   * Reads a chunk of a grid data variable from a FileChannel.
   *
   * @param dv 0.. the data variable index
   * @param channel the open FileChannel to read from
   * @param destBuffer the PrimitiveArray buffer to store results
   * @param startElement the logical starting element index
   * @param maxElements the maximum number of elements to read
   * @return the actual number of elements read
   */
  public int readDataChunk(int dv, FileChannel channel, PrimitiveArray destBuffer, long startElement, int maxElements) throws Exception {
    destBuffer.clear();
    long totalElements = gridDataAccessor.totalIndex.size();
    if (startElement >= totalElements) {
      return 0;
    }
    int numToRead = (int) Math.min(maxElements, totalElements - startElement);
    if (numToRead <= 0) {
      return 0;
    }
    if (destBuffer.elementType() == PAType.STRING) {
      if (startElement == 0) {
        channel.position(0);
      } else if (channel.position() == 0) {
        DataInputStream dis = new DataInputStream(Channels.newInputStream(channel));
        for (long i = 0; i < startElement; i++) {
          dis.readUTF();
        }
      }
      destBuffer.readFromChannel(channel, numToRead);
    } else {
      long byteOffset = startElement * destBuffer.elementSize();
      channel.position(byteOffset);
      destBuffer.readFromChannel(channel, numToRead);
    }
    return numToRead;
  }

  public void releaseGetResources() {
    try {
      if (gridDataAccessor != null) gridDataAccessor.close();
    } catch (Throwable t) {
    }
  }

  /**
   * This releases all resources (e.g., files and threads). It is recommended, but not required,
   * that users of this class call this when they are done using this instance. This won't throw an
   * Exception.
   */
  @Override
  public void close() {
    releaseGetResources();
    try {
      if (dataPAType != null) {
        int nDv = dataPAType.length;
        for (int dv = 0; dv < nDv; dv++) {
          try {
            File2.delete(baseFileName + dv);
          } catch (Throwable t2) {
            String2.log(
                "ERROR in GridDataAllAccessor.deleteFiles: " + MustBe.throwableToString(t2));
          }
        }
        dataPAType = null;
      }
    } catch (Throwable t) {
    }
  }
}
