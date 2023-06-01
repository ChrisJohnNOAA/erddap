package gov.noaa.pfel.coastwatch.griddata;

 import static org.junit.jupiter.api.Assertions.assertArrayEquals;
 import static org.junit.jupiter.api.Assertions.assertEquals;
 import static org.junit.jupiter.api.Assertions.assertTrue;
 
 import org.junit.jupiter.api.DisplayName;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.io.TempDir;
 import java.nio.file.Path;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.util.SSR;


public class OpendapHelperTests {

    @TempDir
    Path directory;
     
    /** This tests dapToNc DGrid. */
     @Test
     @DisplayName("Tests dapToNC DGrid")
     public void testDapToNcDGrid() throws Throwable {
        String2.log("\n\n*** OpendapHelper.testDapToNcDGrid");
        String fileName, expected, results;      
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        fileName = directory.toString() + "/testDapToNcDGrid.nc";
        // TODO: This is testing the coastwatch server as much as it's testing local code which isn't ideal.
        // Solution plan: 1) Write (or find) a test that verifies a local server response to the query
        // 2) Have this test make the data request of the local server or mock out the request so that only local code is tested
        String dGridUrl = "https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday";
        OpendapHelper.dapToNc(dGridUrl, 
            //note that request for zztop is ignored (because not found)
            new String[] {"zztop", "x_wind", "y_wind"}, "[5][0][0:200:1200][0:200:2880]", //projection
            fileName, false); //jplMode
        results = NcHelper.ncdump(fileName, ""); //printData
        expected = 
"netcdf testDapToNcDGrid.nc {\n" +
"  dimensions:\n" +
"    time = 1;\n" +
"    altitude = 1;\n" +
"    latitude = 7;\n" +
"    longitude = 15;\n" +
"  variables:\n" +
"    double time(time=1);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 9.348048E8, 1.2556944E9; // double\n" +
"      :axis = \"T\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Centered Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double altitude(altitude=1);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_CoordinateZisPositive = \"up\";\n" +
"      :actual_range = 10.0, 10.0; // double\n" +
"      :axis = \"Z\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Altitude\";\n" +
"      :positive = \"up\";\n" +
"      :standard_name = \"altitude\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double latitude(latitude=7);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = -75.0, 75.0; // double\n" +
"      :axis = \"Y\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 2; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double longitude(longitude=15);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = 0.0, 360.0; // double\n" +
"      :axis = \"X\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 2; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float x_wind(time=1, altitude=1, latitude=7, longitude=15);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :colorBarMaximum = 15.0; // double\n" +
"      :colorBarMinimum = -15.0; // double\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Zonal Wind\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"x_wind\";\n" +
"      :units = \"m s-1\";\n" +
"\n" +
"    float y_wind(time=1, altitude=1, latitude=7, longitude=15);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :colorBarMaximum = 15.0; // double\n" +
"      :colorBarMinimum = -15.0; // double\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Meridional Wind\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"y_wind\";\n" +
"      :units = \"m s-1\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :composite = \"true\";\n" +
"  :contributor_name = \"Remote Sensing Systems, Inc.\";\n" +
"  :contributor_role = \"Source of level 2 data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"erd.data@noaa.gov\";\n" +
"  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :creator_type = \"institution\";\n" +
"  :creator_url = \"https://www.pfeg.noaa.gov\";\n" +
"  :date_created = \"2010-07-02\";\n" +
"  :date_issued = \"2010-07-02\";\n" +
"  :defaultGraphQuery = \"&.draw=vectors\";\n" +
"  :Easternmost_Easting = 360.0; // double\n" +
"  :geospatial_lat_max = 75.0; // double\n" +
"  :geospatial_lat_min = -75.0; // double\n" +
"  :geospatial_lat_resolution = 0.125; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 360.0; // double\n" +
"  :geospatial_lon_min = 0.0; // double\n" +
"  :geospatial_lon_resolution = 0.125; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_max = 10.0; // double\n" +
"  :geospatial_vertical_min = 10.0; // double\n" +
"  :geospatial_vertical_positive = \"up\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"Remote Sensing Systems, Inc.\n" +
"2010-07-02T15:36:22Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
today + "T";  // + time " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/mday\n" +
//today + " https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday.das\";\n" +
String expected2 = 
"  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\";\n" +
"  :institution = \"NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"altitude, atmosphere, atmospheric, coast, coastwatch, data, degrees, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Oceans > Ocean Winds > Surface Winds, global, noaa, node, ocean, oceans, QSux10, quality, quikscat, science, science quality, seawinds, surface, time, wcn, west, wind, winds, x_wind, zonal\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
"  :Northernmost_Northing = 75.0; // double\n" +
"  :origin = \"Remote Sensing Systems, Inc.\";\n" +
"  :processing_level = \"3\";\n" +
"  :project = \"CoastWatch (https://coastwatch.noaa.gov/)\";\n" +
"  :projection = \"geographic\";\n" +
"  :projection_type = \"mapped\";\n" +
"  :publisher_email = \"erd.data@noaa.gov\";\n" +
"  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :publisher_type = \"institution\";\n" +
"  :publisher_url = \"https://www.pfeg.noaa.gov\";\n" +
"  :references = \"RSS Inc. Winds: http://www.remss.com/ .\";\n" +
"  :satellite = \"QuikSCAT\";\n" +
"  :sensor = \"SeaWinds\";\n" +
"  :source = \"satellite observation: QuikSCAT, SeaWinds\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :Southernmost_Northing = -75.0; // double\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
"  :summary = \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meridional, and modulus sets. The reference height for all wind velocities is 10 meters. (This is a monthly composite.)\";\n" +
"  :time_coverage_end = \"2009-10-16T12:00:00Z\";\n" +
"  :time_coverage_start = \"1999-08-16T12:00:00Z\";\n" +
"  :title = \"Wind, QuikSCAT SeaWinds, 0.125°, Global, Science Quality, 1999-2009 (Monthly)\";\n" +
"  :Westernmost_Easting = 0.0; // double\n" +
"\n" +
"  data:\n" +
"    time = \n" +
"      {9.48024E8}\n" +
"    altitude = \n" +
"      {10.0}\n" +
"    latitude = \n" +
"      {-75.0, -50.0, -25.0, 0.0, 25.0, 50.0, 75.0}\n" +
"    longitude = \n" +
"      {0.0, 25.0, 50.0, 75.0, 100.0, 125.0, 150.0, 175.0, 200.0, 225.0, 250.0, 275.0, 300.0, 325.0, 350.0}\n" +
"    x_wind = \n" +
"      {\n" +
"        {\n" +
"          {\n" +
"            {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 0.76867574, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0},\n" +
"            {6.903795, 7.7432585, 8.052648, 7.375461, 8.358787, 7.5664454, 4.537408, 4.349131, 2.4506109, 2.1340106, 6.4230127, 8.5656395, 5.679372, 5.775274, 6.8520603},\n" +
"            {-3.513153, -9999999.0, -5.7222853, -4.0249896, -4.6091595, -9999999.0, -9999999.0, -3.9060166, -1.821446, -2.0546885, -2.349195, -4.2188687, -9999999.0, -0.7905332, -3.715024},\n" +
"            {0.38850072, -9999999.0, -2.8492346, 0.7843591, -9999999.0, -0.353197, -0.93183184, -5.3337674, -7.8715024, -5.2341905, -2.1567967, 0.46681255, -9999999.0, -3.7223456, -1.3264368},\n" +
"            {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -4.250928, -1.9779109, -2.3081408, -6.070514, -3.4209945, 2.3732827, -3.4732149, -3.2282434, -3.99131, -9999999.0},\n" +
"            {2.3816996, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 1.9863724, 1.746363, 5.305478, 2.3346918, -9999999.0, -9999999.0, 2.0079596, 3.4320266, 1.8692436},\n" +
"            {0.83961326, -3.4395192, -3.1952338, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -2.9099085}\n" +
"          }\n" +
"        }\n" +
"      }\n" +
"    y_wind = \n" +
"      {\n" +
"        {\n" +
"          {\n" +
"            {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 3.9745862, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0},\n" +
"            {-1.6358501, -2.1310546, -1.672539, -2.8083494, -1.7282568, -2.5679686, -0.032763753, 0.6524638, 0.9784334, -2.4545083, 0.6344165, -0.5887741, -0.6837046, -0.92711323, -1.9981208},\n" +
"            {3.7522712, -9999999.0, -0.04178731, 1.6603879, 5.321683, -9999999.0, -9999999.0, 1.5633415, -0.50912154, -2.964269, -0.92438585, 3.959174, -9999999.0, -2.2249718, 0.46982485},\n" +
"            {4.8992314, -9999999.0, -4.7178936, -3.2770228, -9999999.0, -2.8111093, -0.9852706, 0.46997508, 0.0683085, 0.46172503, 1.2998049, 3.5235379, -9999999.0, 1.1354263, 4.7139735},\n" +
"            {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -5.092368, -3.3667018, -0.60028434, -0.7609817, -1.114303, -3.6573937, -0.934499, -0.40036556, -2.5770886, -9999999.0},\n" +
"            {0.56877106, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -3.2394278, 0.45922723, -0.8394715, 0.7333555, -9999999.0, -9999999.0, -2.3936603, 3.725975, 0.09879057},\n" +
"            {-6.128998, 2.379096, 7.463917, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -11.026609}\n" +
"          }\n" +
"        }\n" +
"      }\n" +
"}\n";
/*From .asc request:
https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday.asc?x_wind[5][0][0:200:1200][0:200:2880],y_wind[5][0][0:200:1200][0:200:2880]
x_wind.x_wind[1][1][7][15]
[0][0][0], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 0.76867574, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0
[0][0][1], 6.903795, 7.7432585, 8.052648, 7.375461, 8.358787, 7.5664454, 4.537408, 4.349131, 2.4506109, 2.1340106, 6.4230127, 8.5656395, 5.679372, 5.775274, 6.8520603
[0][0][2], -3.513153, -9999999.0, -5.7222853, -4.0249896, -4.6091595, -9999999.0, -9999999.0, -3.9060166, -1.821446, -2.0546885, -2.349195, -4.2188687, -9999999.0, -0.7905332, -3.715024
[0][0][3], 0.38850072, -9999999.0, -2.8492346, 0.7843591, -9999999.0, -0.353197, -0.93183184, -5.3337674, -7.8715024, -5.2341905, -2.1567967, 0.46681255, -9999999.0, -3.7223456, -1.3264368
[0][0][4], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -4.250928, -1.9779109, -2.3081408, -6.070514, -3.4209945, 2.3732827, -3.4732149, -3.2282434, -3.99131, -9999999.0
[0][0][5], 2.3816996, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 1.9863724, 1.746363, 5.305478, 2.3346918, -9999999.0, -9999999.0, 2.0079596, 3.4320266, 1.8692436
[0][0][6], 0.83961326, -3.4395192, -3.1952338, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -2.9099085
y_wind.y_wind[1][1][7][15]
[0][0][0], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 3.9745862, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0
[0][0][1], -1.6358501, -2.1310546, -1.672539, -2.8083494, -1.7282568, -2.5679686, -0.032763753, 0.6524638, 0.9784334, -2.4545083, 0.6344165, -0.5887741, -0.6837046, -0.92711323, -1.9981208
[0][0][2], 3.7522712, -9999999.0, -0.04178731, 1.6603879, 5.321683, -9999999.0, -9999999.0, 1.5633415, -0.50912154, -2.964269, -0.92438585, 3.959174, -9999999.0, -2.2249718, 0.46982485
[0][0][3], 4.8992314, -9999999.0, -4.7178936, -3.2770228, -9999999.0, -2.8111093, -0.9852706, 0.46997508, 0.0683085, 0.46172503, 1.2998049, 3.5235379, -9999999.0, 1.1354263, 4.7139735
[0][0][4], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -5.092368, -3.3667018, -0.60028434, -0.7609817, -1.114303, -3.6573937, -0.934499, -0.40036556, -2.5770886, -9999999.0
[0][0][5], 0.56877106, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -3.2394278, 0.45922723, -0.8394715, 0.7333555, -9999999.0, -9999999.0, -2.3936603, 3.725975, 0.09879057
[0][0][6], -6.128998, 2.379096, 7.463917, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -11.026609
*/
        assertEquals(results.substring(0, expected.length()), expected, "results=" + results);
        int po = results.indexOf("  :infoUrl =");
        assertEquals(results.substring(po), expected2, "results=" + results);
        File2.delete(fileName);

        //test 1D var should be ignored if others are 2+D
        String2.log("\n*** test 1D var should be ignored if others are 2+D");
        fileName = SSR.getTempDirectory() + "testDapToNcDGrid1D2D.nc";
        OpendapHelper.dapToNc(dGridUrl,
            new String[] {"zztop", "x_wind", "y_wind", "latitude"}, 
            "[5][0][0:200:1200][0:200:2880]", //projection
            fileName, false); //jplMode
        results = NcHelper.ncdump(fileName, "-h"); //printData
        expected = 
"netcdf testDapToNcDGrid1D2D.nc {\n" +
"  dimensions:\n" +
"    time = 1;\n" +
"    altitude = 1;\n" +
"    latitude = 7;\n" +
"    longitude = 15;\n" +
"  variables:\n" +
"    double time(time=1);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 9.348048E8, 1.2556944E9; // double\n" +
"      :axis = \"T\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Centered Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double altitude(altitude=1);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_CoordinateZisPositive = \"up\";\n" +
"      :actual_range = 10.0, 10.0; // double\n" +
"      :axis = \"Z\";\n" +
"      :fraction_digits = 0; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Altitude\";\n" +
"      :positive = \"up\";\n" +
"      :standard_name = \"altitude\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double latitude(latitude=7);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = -75.0, 75.0; // double\n" +
"      :axis = \"Y\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 2; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double longitude(longitude=15);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = 0.0, 360.0; // double\n" +
"      :axis = \"X\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 2; // int\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float x_wind(time=1, altitude=1, latitude=7, longitude=15);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :colorBarMaximum = 15.0; // double\n" +
"      :colorBarMinimum = -15.0; // double\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Zonal Wind\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"x_wind\";\n" +
"      :units = \"m s-1\";\n" +
"\n" +
"    float y_wind(time=1, altitude=1, latitude=7, longitude=15);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :colorBarMaximum = 15.0; // double\n" +
"      :colorBarMinimum = -15.0; // double\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Meridional Wind\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"y_wind\";\n" +
"      :units = \"m s-1\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :composite = \"true\";\n" +
"  :contributor_name = \"Remote Sensing Systems, Inc.\";\n" +
"  :contributor_role = \"Source of level 2 data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"erd.data@noaa.gov\";\n" +
"  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :creator_type = \"institution\";\n" +
"  :creator_url = \"https://www.pfeg.noaa.gov\";\n" +
"  :date_created = \"2010-07-02\";\n" +
"  :date_issued = \"2010-07-02\";\n" +
"  :defaultGraphQuery = \"&.draw=vectors\";\n" +
"  :Easternmost_Easting = 360.0; // double\n" +
"  :geospatial_lat_max = 75.0; // double\n" +
"  :geospatial_lat_min = -75.0; // double\n" +
"  :geospatial_lat_resolution = 0.125; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 360.0; // double\n" +
"  :geospatial_lon_min = 0.0; // double\n" +
"  :geospatial_lon_resolution = 0.125; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_max = 10.0; // double\n" +
"  :geospatial_vertical_min = 10.0; // double\n" +
"  :geospatial_vertical_positive = \"up\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"Remote Sensing Systems, Inc.\n" +
"2010-07-02T15:36:22Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
today + "T"; //time https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/mday\n" +
//today + time " https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday.das\";\n" +
expected2 = 
"  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\";\n" +
"  :institution = \"NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"altitude, atmosphere, atmospheric, coast, coastwatch, data, degrees, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Oceans > Ocean Winds > Surface Winds, global, noaa, node, ocean, oceans, QSux10, quality, quikscat, science, science quality, seawinds, surface, time, wcn, west, wind, winds, x_wind, zonal\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
"  :Northernmost_Northing = 75.0; // double\n" +
"  :origin = \"Remote Sensing Systems, Inc.\";\n" +
"  :processing_level = \"3\";\n" +
"  :project = \"CoastWatch (https://coastwatch.noaa.gov/)\";\n" +
"  :projection = \"geographic\";\n" +
"  :projection_type = \"mapped\";\n" +
"  :publisher_email = \"erd.data@noaa.gov\";\n" +
"  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :publisher_type = \"institution\";\n" +
"  :publisher_url = \"https://www.pfeg.noaa.gov\";\n" +
"  :references = \"RSS Inc. Winds: http://www.remss.com/ .\";\n" +
"  :satellite = \"QuikSCAT\";\n" +
"  :sensor = \"SeaWinds\";\n" +
"  :source = \"satellite observation: QuikSCAT, SeaWinds\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :Southernmost_Northing = -75.0; // double\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
"  :summary = \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meridional, and modulus sets. The reference height for all wind velocities is 10 meters. (This is a monthly composite.)\";\n" +
"  :time_coverage_end = \"2009-10-16T12:00:00Z\";\n" +
"  :time_coverage_start = \"1999-08-16T12:00:00Z\";\n" +
"  :title = \"Wind, QuikSCAT SeaWinds, 0.125°, Global, Science Quality, 1999-2009 (Monthly)\";\n" +
"  :Westernmost_Easting = 0.0; // double\n" +
"}\n";
        assertEquals(results.substring(0, expected.length()), expected, "results=" + results);
        po = results.indexOf("  :infoUrl =");
        assertEquals(results.substring(po), expected2, "results=" + results);
        File2.delete(fileName);



        /* */
        String2.log("\n*** OpendapHelper.testDapToNcDGrid finished.");

    }
    
}
