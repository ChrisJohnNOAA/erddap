package gov.noaa.pfel.erddap.dataset;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.text.MessageFormat;

import com.cohort.util.File2;
import com.cohort.util.Image2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.erddap.util.EDStatic;
import tags.TagRequiresContent;

class EDDGridTests {
  /**
   * Test saveAsImage, specifically to make sure a transparent png that's
   * partially outside of the range of the dataset still returns the image for
   * the part that is within range.
   */
  @org.junit.jupiter.api.Test
  @TagRequiresContent
  void testSaveAsImage() throws Throwable {
    System.setProperty("erddapContentDirectory", System.getProperty("user.dir") + "\\content\\erddap");
    System.setProperty("doSetupValidation", String.valueOf(false));
    String2.log("\n*** EDDGrid.testSaveAsImage()");
    EDDGrid eddGrid = (EDDGrid) EDDGrid.oneFromDatasetsXml(null, "erdMHchla8day");
    int language = 0;
    String dir = EDStatic.fullTestCacheDirectory;
    // String requestUrl = "/erddap/griddap/erdMHchla8day.transparentPng";
    String userDapQueryTemplate = "MWchla%5B(2022-01-16T12:00:00Z):1:(2022-01-16T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B({0,number,#.##########}):1:({1,number,#.##########})%5D%5B({2,number,#.##########}):1:({3,number,#.##########})%5D";
    String baseName, tName;

    String expectedHashForInvalidInput = "9b750d93bf5cc5f356e7b159facec812dc09c20050d38d6362280def580bc62e";

    // Make fully valid image
    baseName = "EDDGrid_testSaveAsImage_fullyValid";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 210, 220), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // Invalid min y.
    baseName = "EDDGrid_testSaveAsImage_invalidMinY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, -100, 40, 210, 220), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // 2020-08-03 For tests below, some generated images have data, some don't,
    // but results seem inconsistent.
    // The images in erddapTest/images are old and I'm not sure appropriate.
    // I'm not sure what they should be. Leave this for Chris John.

    // Invalid max y.
    baseName = "EDDGrid_testSaveAsImage_invalidMaxY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 100, 210, 220), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // All invalid.
    baseName = "EDDGrid_testSaveAsImage_allInvalid";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, -100, 100, -200, 370), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // Out of range min x.
    baseName = "EDDGrid_testSaveAsImage_OORMinX";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 200, 210), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // Out of range max x.
    baseName = "EDDGrid_testSaveAsImage_OORMaxX";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 250, 260), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // Out of range min y.
    baseName = "EDDGrid_testSaveAsImage_OORMinY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 20, 30, 210, 220), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // Out of range max y.
    baseName = "EDDGrid_testSaveAsImage_OORMaxY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 50, 60, 210, 220), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // Fully out of range min x.
    baseName = "EDDGrid_testSaveAsImage_FOORMinX";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 190, 200), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // Fully out of range max x.
    baseName = "EDDGrid_testSaveAsImage_FOORMaxX";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 260, 270), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // Fully out of range min y.
    baseName = "EDDGrid_testSaveAsImage_FOORMinY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 10, 20, 210, 220), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

    // Fully out of range max y.
    baseName = "EDDGrid_testSaveAsImage_FOORMaxY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 60, 70, 210, 220), // #'s are minLat, maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2.testImagesIdentical(
        dir + tName,
        String2.unitTestImagesDir() + baseName + ".png",
        File2.getSystemTempDirectory() + baseName + "_diff.png");

  }

  /**
   * Tests input for saveAsImage against the provided output. Specifically the
   * output is provided as a hash (sha-256) of the output bytes.
   * 
   * @param eddGrid      EDDGrid that saveAsImage is called on.
   * @param dir          Directory used for temporary/cache files.
   * @param requestUrl   The part of the user's request, after
   *                     EDStatic.baseUrl, before '?'.
   * @param userDapQuery An OPeNDAP DAP-style query string, still
   *                     percentEncoded (shouldn't be null). e.g.,
   *                     ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param fileTypeName File type being requested (eg: .transparentPng)
   * @param expected     The expected hash of the output of the saveAsImage
   *                     call.
   * @throws Throwable
   */
  @org.junit.jupiter.api.Test
  @TagRequiresContent
  void testSaveAsImageVsExpected(EDDGrid eddGrid, String dir,
      String requestUrl, String userDapQuery, String fileTypeName,
      String expected) throws Throwable {
    System.setProperty("erddapContentDirectory", System.getProperty("user.dir") + "\\content\\erddap");
    System.setProperty("doSetupValidation", String.valueOf(false));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStreamSourceSimple osss = new OutputStreamSourceSimple(baos);
    String filename = dir + Math2.random(Integer.MAX_VALUE) + ".png";

    eddGrid.saveAsImage(0 /* language */, null /* loggedInAs */, requestUrl,
        userDapQuery, dir, filename,
        osss /* outputStreamSource */, fileTypeName);

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(baos.toByteArray());
      StringBuilder hexString = new StringBuilder();

      for (int i = 0; i < hash.length; i++) {
        String hex = Integer.toHexString(0xff & hash[i]);
        if (hex.length() == 1)
          hexString.append('0');
        hexString.append(hex);
      }

      String results = hexString.toString();

      // String2.log(results);
      Test.ensureEqual(results.substring(0, expected.length()), expected,
          "\nresults=\n" + results.substring(0,
              Math.min(256, results.length())));
    } catch (Exception ex) {
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(baos.toByteArray());
      fos.flush();
      fos.close();
      Test.displayInBrowser("file://" + filename);
      throw new RuntimeException(ex);
    }
  }

}
