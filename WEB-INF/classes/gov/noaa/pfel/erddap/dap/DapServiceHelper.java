package gov.noaa.pfel.erddap.dap;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.String2;
import com.cohort.util.Test;
import dods.dap.AttributeTable;
import dods.dap.BaseType;
import dods.dap.DAS;
import dods.dap.DASException;
import dods.dap.DArray;
import dods.dap.DBoolean;
import dods.dap.DByte;
import dods.dap.DConnect;
import dods.dap.DConstructor;
import dods.dap.DDS;
import dods.dap.DDSException;
import dods.dap.DFloat32;
import dods.dap.DFloat64;
import dods.dap.DGrid;
import dods.dap.DInt16;
import dods.dap.DInt32;
import dods.dap.DSequence;
import dods.dap.DString;
import dods.dap.DUInt16;
import dods.dap.DUInt32;
import dods.dap.DVector;
import dods.dap.DataDDS;
import dods.dap.NoSuchVariableException;
import dods.dap.Server.InvalidParameterException;
import dods.dap.parser.ParseException;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** A helper class to encapsulate low-level OPeNDAP functionality. */
public class DapServiceHelper {
  /** Abstraction for the core DAP metadata objects (DAS, DDS, DConnect). */
  public static class DapMetadata {
    // These are the only direct dods.dap objects; they must only be accessed
    // internally by the DapServiceHelper implementation methods.
    private final byte[] dasBytes;
    private final byte[] ddsBytes;
    private final DAS das;
    private final DDS dds;
    private final String url;
    private final boolean acceptDeflate;
    private DConnect dConnect = null;

    public DapMetadata(byte[] dasBytes, byte[] ddsBytes, String url, boolean acceptDeflate)
        throws DASException, ParseException, DDSException {
      this.dasBytes = dasBytes;
      this.ddsBytes = ddsBytes;
      // DAS
      das = new DAS();
      das.parse(new ByteArrayInputStream(dasBytes));

      // DDS
      dds = new DDS();
      dds.parse(new ByteArrayInputStream(ddsBytes));
      this.url = url;
      this.acceptDeflate = acceptDeflate;
    }

    public DConnect getdConnect() throws Throwable {
      if (dConnect == null) {
        dConnect = new DConnect(url, acceptDeflate, 1, 1);
      }
      return dConnect;
    }

    public byte[] getDasBytes() throws IOException {
      return dasBytes;
    }

    public byte[] getDdsBytes() throws IOException {
      return ddsBytes;
    }
  }

  /** Abstraction for grid variable and dimension information. */
  public static class DapVariableInfo {
    private final DArray mainDArray;

    public DapVariableInfo(BaseType baseType) throws RuntimeException, NoSuchVariableException {
      if (baseType instanceof DGrid dgrid) {
        this.mainDArray = (DArray) dgrid.getVar(0); // first element is always main array
      } else if (baseType instanceof DArray darray) {
        this.mainDArray = darray;
      } else {
        throw new RuntimeException(
            "Source variable must be a DGrid or a DArray (" + baseType.toString() + ").");
      }
    }

    public String getName() {
      return mainDArray.getName();
    }

    public int getNumDimensions() {
      return mainDArray.numDimensions();
    }

    public String getDimensionName(int av) throws InvalidParameterException {
      return mainDArray.getDimension(av).getName();
    }

    public int getDimensionSize(int av) throws InvalidParameterException {
      return mainDArray.getDimension(av).getSize();
    }

    public PAType getSourceDataType() throws Exception {
      return OpendapHelper.getElementPAType(mainDArray.getPrimitiveVector());
    }
  }

  /** Abstraction for sequence variable info (name, type, attributes). */
  public static class DapSequenceVariableInfo {
    public final String name;
    public final PAType sourceType;
    public final Attributes sourceAttributes;
    public final boolean isOuterVariable;

    public DapSequenceVariableInfo(
        String name, PAType sourceType, Attributes sourceAttributes, boolean isOuterVariable) {
      this.name = name;
      this.sourceType = sourceType;
      this.sourceAttributes = sourceAttributes;
      this.isOuterVariable = isOuterVariable;
    }
  }

  public static class DapAllVariableSequenceInfo {
    public final Map<String, DapSequenceVariableInfo> variableInfoMap;
    public final Attributes gridMappingAtts;
    public final String outerSequenceName;
    public final String innerSequenceName;

    public DapAllVariableSequenceInfo(
        Map<String, DapSequenceVariableInfo> variableInfoMap,
        Attributes gridMappingAtts,
        String outerSequenceName,
        String innerSequenceName) {
      this.variableInfoMap = variableInfoMap;
      this.gridMappingAtts = gridMappingAtts;
      this.outerSequenceName = outerSequenceName;
      this.innerSequenceName = innerSequenceName;
    }
  }

  /**
   * Fetches the DAS and DDS bytes, parses them, and creates a live DConnect. This replaces the DAS,
   * DDS, and DConnect setup in dataset constructors.
   */
  public static DapMetadata fetchMetadata(String url, boolean acceptDeflate) throws Throwable {
    // DAS
    byte dasBytes[] = SSR.getUrlResponseBytes(url + ".das"); //
    // DDS
    byte ddsBytes[] = SSR.getUrlResponseBytes(url + ".dds"); //
    return fetchMetadata(dasBytes, ddsBytes, url, acceptDeflate);
  }

  public static DapMetadata fetchMetadata(
      byte[] dasBytes, byte[] ddsBytes, String url, boolean acceptDeflate) throws Throwable {
    return new DapMetadata(dasBytes, ddsBytes, url, acceptDeflate);
  }

  /** Gets attributes for a global or specific variable. */
  public static Attributes getAttributes(DapMetadata metadata, String varName, Attributes atts) {
    OpendapHelper.getAttributes(metadata.das, varName, atts); //
    return atts;
  }

  /** Gets abstraction information about a grid data variable (DGrid or DArray). */
  public static DapVariableInfo getVariableInfo(DapMetadata metadata, String sourceName)
      throws Throwable {
    BaseType bt = metadata.dds.getVariable(sourceName); //
    return new DapVariableInfo(bt);
  }

  public static List<DapVariableInfo> getAllVariableInfos(DapMetadata metadata) throws Throwable {
    List<DapVariableInfo> varInfoList = new ArrayList<>();
    Iterator<BaseType> vars = metadata.dds.getVariables();
    while (vars.hasNext()) {
      BaseType bt = vars.next();
      if (bt instanceof DGrid || bt instanceof DArray) {
        varInfoList.add(new DapVariableInfo(bt));
      }
    }
    return varInfoList;
  }

  /** Fetches the values for a single axis variable. */
  public static PrimitiveArray getAxisValues(DapMetadata metadata, String sourceAxisName)
      throws Throwable {
    return OpendapHelper.getPrimitiveArray(metadata.getdConnect(), "?" + sourceAxisName); //
  }

  /**
   * Fetches the data for one or more grid data variables using a constraint. The result is
   * PrimitiveArray[]: [0] is data, [1+] are axes (if a DGrid).
   */
  public static PrimitiveArray[] getGridData(String url, String query, boolean acceptDeflate)
      throws Throwable {
    DConnect dConnect = new DConnect(url, acceptDeflate, 1, 1);
    return OpendapHelper.getPrimitiveArrays(dConnect, query); //
  }

  /** Analyzes the DDS structure of a Sequence dataset to extract variable info. */
  public static Map<String, DapSequenceVariableInfo> getSequenceVariableInfo(
      DapMetadata metadata, String outerSequenceName, String innerSequenceName, String errorString)
      throws Throwable {
    Map<String, DapSequenceVariableInfo> infoMap = new HashMap<>();

    BaseType outerVariable = metadata.dds.getVariable(outerSequenceName);
    if (!(outerVariable instanceof DSequence outerSequence))
      throw new IllegalArgumentException(
          errorString != null
              ? errorString
              : ""
                  + "outerVariable not a DSequence: name="
                  + outerVariable.getName()
                  + " type="
                  + outerVariable.getTypeName());

    AttributeTable outerAttributeTable = metadata.das.getAttributeTable(outerSequenceName);

    // 1. Iterate over outer sequence variables
    Iterator<BaseType> outerVars = outerSequence.getVariables();
    while (outerVars.hasNext()) {
      BaseType outerVar = outerVars.next();
      String oName = outerVar.getName();

      if (innerSequenceName != null && innerSequenceName.equals(oName)) {
        // 2. Handle inner sequence
        DSequence innerSequence = (DSequence) outerVar;
        AttributeTable innerAttributeTable =
            metadata.das.getAttributeTable(innerSequence.getName());
        Iterator<BaseType> innerVars = innerSequence.getVariables();

        while (innerVars.hasNext()) {
          BaseType innerVar = innerVars.next();
          String iName = innerVar.getName();

          // Skip complex types in inner sequence (DConstructor and DVector are ignored in source)
          if (innerVar instanceof DConstructor || innerVar instanceof DVector) continue;

          PAType sourceType = OpendapHelper.getElementPAType(innerVar.newPrimitiveVector()); //

          // outerSequenceName + "." + innerSequenceName + "." + iName
          Attributes tAtt =
              getSequenceAttributes(metadata.das, innerAttributeTable, innerVar, iName);
          infoMap.put(iName, new DapSequenceVariableInfo(iName, sourceType, tAtt, false));
        }

      } else {
        // 3. Handle outer variables
        if (outerVar instanceof DConstructor) continue; // Skip DConstructor types

        PAType sourceType = OpendapHelper.getElementPAType(outerVar.newPrimitiveVector()); //

        // outerSequenceName + "." + oName
        Attributes tAtt = getSequenceAttributes(metadata.das, outerAttributeTable, outerVar, oName);
        infoMap.put(oName, new DapSequenceVariableInfo(oName, sourceType, tAtt, true));
      }
    }

    return infoMap;
  }

  /** Analyzes the DDS structure of a Sequence dataset to extract variable info. */
  public static DapAllVariableSequenceInfo getAllSequenceVariableInfo(DapMetadata metadata)
      throws Throwable {
    Map<String, DapSequenceVariableInfo> infoMap = new HashMap<>();
    String outerSequenceName = null;
    String innerSequenceName = null;
    Attributes gridMappingAtts = null;

    Iterator<BaseType> variables = metadata.dds.getVariables();
    while (variables.hasNext()) {
      BaseType datasetVar = variables.next();

      // is this the pseudo-data grid_mapping variable?
      if (gridMappingAtts == null) {
        Attributes tSourceAtts = new Attributes();
        DapServiceHelper.getAttributes(metadata, datasetVar.getName(), tSourceAtts);
        gridMappingAtts = NcHelper.getGridMappingAtts(tSourceAtts);
      }
      if (outerSequenceName == null && datasetVar instanceof DSequence outerSequence) {
        outerSequenceName = outerSequence.getName();

        // get list of outerSequence variables
        Iterator<BaseType> outerVars = outerSequence.getVariables();
        while (outerVars.hasNext()) {
          BaseType outerVar = outerVars.next();
          if (outerVar instanceof DSequence innerSequence) {
            if (innerSequenceName == null) {
              innerSequenceName = outerVar.getName();
              Iterator<BaseType> innerVars = innerSequence.getVariables();
              while (innerVars.hasNext()) {
                // inner variable
                BaseType innerVar = innerVars.next();
                if (innerVar instanceof DConstructor || innerVar instanceof DVector) {
                } else {
                  String varName = innerVar.getName();
                  Attributes sourceAtts = new Attributes();
                  DapServiceHelper.getAttributes(metadata, varName, sourceAtts);
                  if (sourceAtts.size() == 0) {
                    DapServiceHelper.getAttributes(
                        metadata,
                        outerSequenceName + "." + innerSequenceName + "." + varName,
                        sourceAtts);
                  }
                  PAType sourceType = OpendapHelper.getElementPAType(innerVar);
                  infoMap.put(
                      varName, new DapSequenceVariableInfo(varName, sourceType, sourceAtts, false));
                }
              }
            }
          } else if (outerVar instanceof DConstructor) {
            // skip it
          } else {
            // outer variable
            String varName = outerVar.getName();
            Attributes sourceAtts = new Attributes();
            DapServiceHelper.getAttributes(metadata, varName, sourceAtts);
            PAType sourceType = OpendapHelper.getElementPAType(outerVar);
            infoMap.put(
                varName, new DapSequenceVariableInfo(varName, sourceType, sourceAtts, true));
          }
        }
      }
    }
    return new DapAllVariableSequenceInfo(
        infoMap, gridMappingAtts, outerSequenceName, innerSequenceName);
  }

  // Internal helper to simplify attribute fetching logic from EDDTableFromDapSequence.java
  private static Attributes getSequenceAttributes(
      DAS das, AttributeTable attTable, BaseType var, String varName) {
    Attributes tAtt = new Attributes();
    if (attTable == null) {
      // Dapper needs this approach (using LongName)
      OpendapHelper.getAttributes(das, var.getLongName(), tAtt);
      // drds needs this approach (using short Name)
      if (tAtt.size() == 0) OpendapHelper.getAttributes(das, varName, tAtt);
    } else {
      // Standard approach using AttributeTable
      dods.dap.Attribute attribute = attTable.getAttribute(varName);
      if (attribute == null) {
        String2.log("WARNING!!! Unexpected: no attribute for outerVar=" + varName + ".");
      } else if (attribute.isContainer()) {
        OpendapHelper.getAttributes(attribute.getContainer(), tAtt);
      } else {
        String2.log(
            "WARNING!!! Unexpected: attribute for outerVar="
                + varName
                + " not a container: "
                + attribute.getName()
                + "="
                + attribute.getValueAt(0));
      }
    }
    return tAtt;
  }

  /**
   * Reads data from an OPeNDAP 1-level or 2-level sequence response and returns it as a new Table.
   * This encapsulates all dods.dap operations.
   *
   * @param url The OPeNDAP DAP URL (e.g., ending in .dods).
   * @param skipDapperSpacerRows if true, skips the last row of each innerSequence.
   * @return A new Table instance populated with the sequence data and metadata.
   * @throws Exception if trouble
   */
  public static Table readOpendapSequence(Table table, String url, boolean skipDapperSpacerRows)
      throws Exception {

    String errorInMethod =
        String2.ERROR + " in DapServiceHelper.readOpendapSequence(" + url + "):\n";

    // 1. Connection and Metadata (DAS)
    DConnect dConnect = new DConnect(url, Table.opendapAcceptDeflate, 1, 1);
    DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    OpendapHelper.getAttributes(das, "GLOBAL", table.globalAttributes());

    // 2. Data/Structure (DataDDS)
    DataDDS dataDds = dConnect.getData(null);
    BaseType firstVariable = dataDds.getVariables().next();

    if (!(firstVariable instanceof DSequence outerSequence))
      throw new Exception(
          errorInMethod
              + "firstVariable not a DSequence: name="
              + firstVariable.getName()
              + " type="
              + firstVariable.getTypeName());

    int nOuterRows = outerSequence.getRowCount();
    int nOuterColumns = outerSequence.elementCount();
    AttributeTable outerAttributeTable = das.getAttributeTable(outerSequence.getLongName());

    // 3. Setup Columns (Metadata Only)
    int innerSequenceColumn = -1;
    int nInnerColumns = 0;

    // First Pass: Define all columns (outer and inner)
    for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {
      BaseType obt = outerSequence.getVar(outerCol);

      if (obt instanceof DSequence innerSequence) {
        // *** Start Dealing With InnerSequence
        if (innerSequenceColumn != -1) {
          throw new Exception(errorInMethod + "The response has more than one inner sequence.");
        }
        innerSequenceColumn = outerCol;
        nInnerColumns = innerSequence.elementCount();
        AttributeTable innerAttributeTable = das.getAttributeTable(innerSequence.getName());

        for (int innerCol = 0; innerCol < nInnerColumns; innerCol++) {
          BaseType ibt = innerSequence.getVar(innerCol);
          PrimitiveArray pa = paForBaseType(ibt, errorInMethod);
          table.addColumn(ibt.getName(), pa);
          fetchDapAttributes(
              table, das, innerAttributeTable, ibt, table.nColumns() - 1, errorInMethod);
        }
        // *** End Dealing With InnerSequence
      } else {
        // Outer Column
        PrimitiveArray pa = paForBaseType(obt, errorInMethod);
        table.addColumn(obt.getName(), pa);
        fetchDapAttributes(
            table, das, outerAttributeTable, obt, table.nColumns() - 1, errorInMethod);
      }
    }

    // 4. Read Data (Row-by-Row)
    for (int outerRow = 0; outerRow < nOuterRows; outerRow++) {
      List<BaseType> outerVector = outerSequence.getRow(outerRow);
      int col; // Pointer to the current column index in the table

      // 4a. Get data from innerSequence first (to determine nInnerRows)
      int nInnerRows = 1;
      if (innerSequenceColumn >= 0) {
        DSequence innerSequence = (DSequence) outerVector.get(innerSequenceColumn);
        nInnerRows = innerSequence.getRowCount();
        if (skipDapperSpacerRows && outerRow < nOuterRows - 1) nInnerRows--;

        Test.ensureEqual(
            nInnerColumns,
            innerSequence.elementCount(),
            errorInMethod + "Unexpected nInnerColumns for outer row #" + outerRow);
        col = innerSequenceColumn; // Starting column index for inner sequence in table
        for (int innerRow = 0; innerRow < nInnerRows; innerRow++) {
          List<BaseType> innerVector = innerSequence.getRow(innerRow);
          for (int innerCol = 0; innerCol < nInnerColumns; innerCol++) {
            BaseType ibt = innerVector.get(innerCol);
            addNValueToCol(ibt, table, col + innerCol, 1, errorInMethod);
          }
        }
      }

      // 4b. Process the other outerCol variables, duplicating for nInnerRows
      col = 0;
      for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {
        if (outerCol == innerSequenceColumn) {
          col += nInnerColumns;
          continue;
        }

        BaseType obt = outerVector.get(outerCol);
        addNValueToCol(obt, table, col++, nInnerRows, errorInMethod);
      }
    }

    return table;
  }

  private static void addNValueToCol(
      BaseType bt, Table table, int col, int addCount, String errorInMethod) throws Exception {
    if (bt instanceof DByte t) ((ByteArray) table.getColumn(col)).addN(addCount, t.getValue());
    else if (bt instanceof DFloat32 t)
      ((FloatArray) table.getColumn(col)).addN(addCount, t.getValue());
    else if (bt instanceof DFloat64 t)
      ((DoubleArray) table.getColumn(col)).addN(addCount, t.getValue());
    else if (bt instanceof DUInt16 t)
      ((ShortArray) table.getColumn(col)).addN(addCount, t.getValue());
    else if (bt instanceof DInt16 t)
      ((ShortArray) table.getColumn(col)).addN(addCount, t.getValue());
    else if (bt instanceof DUInt32 t)
      ((IntArray) table.getColumn(col)).addN(addCount, t.getValue());
    else if (bt instanceof DInt32 t) ((IntArray) table.getColumn(col)).addN(addCount, t.getValue());
    else if (bt instanceof DBoolean t)
      ((ByteArray) table.getColumn(col))
          .addN(
              addCount,
              (byte) (t.getValue() ? 1 : 0)); // .nc doesn't support booleans, so store byte=0|1
    else if (bt instanceof DString t)
      ((StringArray) table.getColumn(col)).addN(addCount, t.getValue());
    else {
      throw new Exception(
          errorInMethod
              + "Unexpected inner variable type="
              + bt.getTypeName()
              + " for name="
              + bt.getName());
    }
  }

  private static PrimitiveArray paForBaseType(BaseType bt, String errorInMethod) throws Exception {
    if (bt instanceof DByte) return new ByteArray();
    else if (bt instanceof DFloat32) return new FloatArray();
    else if (bt instanceof DFloat64) return new DoubleArray();
    else if (bt instanceof DInt16) return new ShortArray();
    else if (bt instanceof DUInt16) return new ShortArray();
    else if (bt instanceof DInt32) return new IntArray();
    else if (bt instanceof DUInt32) return new IntArray();
    else if (bt instanceof DBoolean)
      return new ByteArray(); // .nc doesn't support booleans, so store byte=0|1
    else if (bt instanceof DString) return new StringArray();

    throw new Exception(
        errorInMethod
            + "Unexpected variable type="
            + bt.getTypeName()
            + " for name="
            + bt.getName());
  }

  /** Helper method to encapsulate attribute retrieval for outer/inner sequence variables. */
  private static void fetchDapAttributes(
      Table table, DAS das, AttributeTable attTable, BaseType bt, int tCol, String errorInMethod) {
    // Logic extracted from Table.java:readOpendapSequence
    if (attTable == null) {
      // Dapper needs this approach (using LongName)
      OpendapHelper.getAttributes(das, bt.getLongName(), table.columnAttributes(tCol));
      // drds needs this approach (using short Name)
      if (table.columnAttributes(tCol).size() == 0)
        OpendapHelper.getAttributes(das, bt.getName(), table.columnAttributes(tCol));
    } else {
      // Standard approach using AttributeTable
      dods.dap.Attribute attribute = attTable.getAttribute(bt.getName());
      if (attribute == null) {
        String2.log(errorInMethod + "Unexpected: no attribute for var=" + bt.getName() + ".");
      } else if (attribute.isContainer()) {
        OpendapHelper.getAttributes(attribute.getContainer(), table.columnAttributes(tCol));
      } else {
        String2.log(
            errorInMethod
                + "Unexpected: attribute for var="
                + bt.getName()
                + " not a container: "
                + attribute.getName()
                + "="
                + attribute.getValueAt(0));
      }
    }
  }
}
