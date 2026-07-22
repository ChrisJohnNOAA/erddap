package gov.noaa.pfel.erddap.dap;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
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
import gov.noaa.pfel.erddap.util.EDStatic; // Added import for the config flag
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ucar.ma2.ArrayStructure;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

/**
 * A helper class to encapsulate low-level OPeNDAP functionality.
 *
 * <p>This class acts as a dispatcher, delegating calls to a specific implementation (e.g., dods.dap
 * or netcdf-java) based on the {@code EDStatic.config.useNetcdfDap} flag.
 */
public final class DapServiceHelper {

  private DapServiceHelper() {
    // Private constructor to prevent instantiation
  }

  // =================================================================================
  // --- Public Abstract Handle Classes ---
  // These are the public "handles" that client code (like EDD...) will use.
  // =================================================================================

  /**
   * Abstraction for the core DAP metadata. The object contains a reference to the strategy that
   * created it, ensuring subsequent calls use the correct implementation.
   */
  public abstract static class DapMetadata {
    protected final IDapServiceStrategy strategy;

    protected DapMetadata(IDapServiceStrategy strategy) {
      this.strategy = strategy;
    }

    public abstract byte[] getDasBytes() throws IOException;

    public abstract byte[] getDdsBytes() throws IOException;
  }

  /** Abstraction for grid variable and dimension information. */
  public abstract static class DapVariableInfo {
    protected final IDapServiceStrategy strategy;

    protected DapVariableInfo(IDapServiceStrategy strategy) {
      this.strategy = strategy;
    }

    public abstract String getName();

    public abstract int getNumDimensions();

    public abstract String getDimensionName(int av) throws InvalidParameterException;

    public abstract int getDimensionSize(int av) throws InvalidParameterException;

    public abstract PAType getSourceDataType() throws Exception;
  }

  // =================================================================================
  // --- POJO Inner Classes (Implementation-Agnostic) ---
  // These are simple data holders and require no changes.
  // =================================================================================

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

  // =================================================================================
  // --- Strategy Interface and Dispatcher Logic ---
  // =================================================================================

  /**
   * Defines the common interface for all OPeNDAP service implementations (e.g., DODS, NetCDF-Java).
   */
  private interface IDapServiceStrategy {
    DapMetadata fetchMetadata(String url, boolean acceptDeflate) throws Throwable;

    DapMetadata fetchMetadata(byte[] dasBytes, byte[] ddsBytes, String url, boolean acceptDeflate)
        throws Throwable;

    Attributes getAttributes(DapMetadata metadata, String varName, Attributes atts);

    DapVariableInfo getVariableInfo(DapMetadata metadata, String sourceName) throws Throwable;

    List<DapVariableInfo> getAllVariableInfos(DapMetadata metadata) throws Throwable;

    PrimitiveArray getAxisValues(DapMetadata metadata, String sourceAxisName) throws Throwable;

    PrimitiveArray[] getGridData(String url, String query, boolean acceptDeflate) throws Throwable;

    Map<String, DapSequenceVariableInfo> getSequenceVariableInfo(
        DapMetadata metadata,
        String outerSequenceName,
        String innerSequenceName,
        String errorString)
        throws Throwable;

    DapAllVariableSequenceInfo getAllSequenceVariableInfo(DapMetadata metadata) throws Throwable;

    Table readOpendapSequence(Table table, String url, boolean skipDapperSpacerRows)
        throws Exception;
  }

  // --- Static Strategy Instances ---
  private static final IDapServiceStrategy dodsStrategy = new DodsDapStrategy();
  private static final IDapServiceStrategy netcdfStrategy = new NetcdfDapStrategy();

  /** Gets the appropriate service implementation based on the static config flag. */
  private static IDapServiceStrategy getStrategy() {
    return EDStatic.config.useNetcdfDap ? netcdfStrategy : dodsStrategy;
  }

  // =================================================================================
  // --- Public Static Dispatcher Methods ---
  // These are the public API. They delegate to the appropriate strategy.
  // =================================================================================

  /**
   * Fetches the DAS and DDS bytes, parses them, and creates a metadata handle. This is a "factory"
   * method that uses the current global flag.
   */
  public static DapMetadata fetchMetadata(String url, boolean acceptDeflate) throws Throwable {
    return getStrategy().fetchMetadata(url, acceptDeflate);
  }

  /**
   * Creates a metadata handle from existing DAS and DDS bytes. This is a "factory" method that uses
   * the current global flag.
   */
  public static DapMetadata fetchMetadata(
      byte[] dasBytes, byte[] ddsBytes, String url, boolean acceptDeflate) throws Throwable {
    return getStrategy().fetchMetadata(dasBytes, ddsBytes, url, acceptDeflate);
  }

  /**
   * Gets attributes for a global or specific variable. This is a "consumer" method that delegates
   * to the strategy stored within the metadata object.
   */
  public static Attributes getAttributes(DapMetadata metadata, String varName, Attributes atts) {
    return metadata.strategy.getAttributes(metadata, varName, atts);
  }

  /** Gets abstraction information about a grid data variable. This is a "consumer" method. */
  public static DapVariableInfo getVariableInfo(DapMetadata metadata, String sourceName)
      throws Throwable {
    return metadata.strategy.getVariableInfo(metadata, sourceName);
  }

  /** Gets abstraction information for all grid data variables. This is a "consumer" method. */
  public static List<DapVariableInfo> getAllVariableInfos(DapMetadata metadata) throws Throwable {
    return metadata.strategy.getAllVariableInfos(metadata);
  }

  /** Fetches the values for a single axis variable. This is a "consumer" method. */
  public static PrimitiveArray getAxisValues(DapMetadata metadata, String sourceAxisName)
      throws Throwable {
    return metadata.strategy.getAxisValues(metadata, sourceAxisName);
  }

  /**
   * Fetches the data for one or more grid data variables using a constraint. This method is
   * self-contained and uses the current global flag.
   */
  public static PrimitiveArray[] getGridData(String url, String query, boolean acceptDeflate)
      throws Throwable {
    return getStrategy().getGridData(url, query, acceptDeflate);
  }

  /**
   * Analyzes the DDS structure of a Sequence dataset to extract variable info. This is a "consumer"
   * method.
   */
  public static Map<String, DapSequenceVariableInfo> getSequenceVariableInfo(
      DapMetadata metadata, String outerSequenceName, String innerSequenceName, String errorString)
      throws Throwable {
    return metadata.strategy.getSequenceVariableInfo(
        metadata, outerSequenceName, innerSequenceName, errorString);
  }

  /**
   * Analyzes the DDS structure of a Sequence dataset to extract all variable info. This is a
   * "consumer" method.
   */
  public static DapAllVariableSequenceInfo getAllSequenceVariableInfo(DapMetadata metadata)
      throws Throwable {
    return metadata.strategy.getAllSequenceVariableInfo(metadata);
  }

  /**
   * Reads data from an OPeNDAP 1-level or 2-level sequence response. This method is self-contained
   * and uses the current global flag.
   */
  public static Table readOpendapSequence(Table table, String url, boolean skipDapperSpacerRows)
      throws Exception {
    return getStrategy().readOpendapSequence(table, url, skipDapperSpacerRows);
  }

  // =================================================================================
  // --- Implementation 1: DodsDapStrategy (Original Code) ---
  // All the original logic is moved into this private class.
  // =================================================================================

  private static final class DodsDapStrategy implements IDapServiceStrategy {

    /** Concrete dods.dap implementation of DapMetadata. */
    public static class DodsDapMetadata extends DapMetadata {
      // These are the only direct dods.dap objects;
      private final byte[] dasBytes;
      private final byte[] ddsBytes;
      private final DAS das;
      private final DDS dds;
      private final String url;
      private final boolean acceptDeflate;
      private DConnect dConnect = null;

      public DodsDapMetadata(byte[] dasBytes, byte[] ddsBytes, String url, boolean acceptDeflate)
          throws DASException, ParseException, DDSException {
        super(dodsStrategy); // Link to the strategy that created it
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

      /** This method is specific to DodsDapStrategy and is called by its methods. */
      public DConnect getdConnect() throws Throwable {
        if (dConnect == null) {
          dConnect = new DConnect(url, acceptDeflate, 1, 1);
        }
        return dConnect;
      }

      @Override
      public byte[] getDasBytes() throws IOException {
        return dasBytes;
      }

      @Override
      public byte[] getDdsBytes() throws IOException {
        return ddsBytes;
      }
    }

    /** Concrete dods.dap implementation of DapVariableInfo. */
    public static class DodsDapVariableInfo extends DapVariableInfo {
      private final DArray mainDArray;

      public DodsDapVariableInfo(BaseType baseType)
          throws RuntimeException, NoSuchVariableException {
        super(dodsStrategy); // Link to the strategy that created it
        if (baseType instanceof DGrid dgrid) {
          this.mainDArray = (DArray) dgrid.getVar(0); // first element is always main array
        } else if (baseType instanceof DArray darray) {
          this.mainDArray = darray;
        } else {
          throw new RuntimeException(
              "Source variable must be a DGrid or a DArray (" + baseType.toString() + ").");
        }
      }

      @Override
      public String getName() {
        return mainDArray.getName();
      }

      @Override
      public int getNumDimensions() {
        return mainDArray.numDimensions();
      }

      @Override
      public String getDimensionName(int av) throws InvalidParameterException {
        return mainDArray.getDimension(av).getName();
      }

      @Override
      public int getDimensionSize(int av) throws InvalidParameterException {
        return mainDArray.getDimension(av).getSize();
      }

      @Override
      public PAType getSourceDataType() throws Exception {
        return OpendapHelper.getElementPAType(mainDArray.getPrimitiveVector());
      }
    }

    // --- Strategy Method Implementations ---

    @Override
    public DapMetadata fetchMetadata(String url, boolean acceptDeflate) throws Throwable {
      // DAS
      byte dasBytes[] = SSR.getUrlResponseBytes(url + ".das");
      // DDS
      byte ddsBytes[] = SSR.getUrlResponseBytes(url + ".dds");
      return new DodsDapMetadata(dasBytes, ddsBytes, url, acceptDeflate);
    }

    @Override
    public DapMetadata fetchMetadata(
        byte[] dasBytes, byte[] ddsBytes, String url, boolean acceptDeflate) throws Throwable {
      return new DodsDapMetadata(dasBytes, ddsBytes, url, acceptDeflate);
    }

    @Override
    public Attributes getAttributes(DapMetadata metadata, String varName, Attributes atts) {
      DodsDapMetadata dodsMetadata = (DodsDapMetadata) metadata;
      OpendapHelper.getAttributes(dodsMetadata.das, varName, atts);
      return atts;
    }

    @Override
    public DapVariableInfo getVariableInfo(DapMetadata metadata, String sourceName)
        throws Throwable {
      DodsDapMetadata dodsMetadata = (DodsDapMetadata) metadata;
      BaseType bt = dodsMetadata.dds.getVariable(sourceName);
      return new DodsDapVariableInfo(bt);
    }

    @Override
    public List<DapVariableInfo> getAllVariableInfos(DapMetadata metadata) throws Throwable {
      DodsDapMetadata dodsMetadata = (DodsDapMetadata) metadata;
      List<DapVariableInfo> varInfoList = new ArrayList<>();
      Iterator<BaseType> vars = dodsMetadata.dds.getVariables();
      while (vars.hasNext()) {
        BaseType bt = vars.next();
        if (bt instanceof DGrid || bt instanceof DArray) {
          varInfoList.add(new DodsDapVariableInfo(bt));
        }
      }
      return varInfoList;
    }

    @Override
    public PrimitiveArray getAxisValues(DapMetadata metadata, String sourceAxisName)
        throws Throwable {
      DodsDapMetadata dodsMetadata = (DodsDapMetadata) metadata;
      return OpendapHelper.getPrimitiveArray(dodsMetadata.getdConnect(), "?" + sourceAxisName);
    }

    @Override
    public PrimitiveArray[] getGridData(String url, String query, boolean acceptDeflate)
        throws Throwable {
      DConnect dConnect = new DConnect(url, acceptDeflate, 1, 1);
      return OpendapHelper.getPrimitiveArrays(dConnect, query);
    }

    @Override
    public Map<String, DapSequenceVariableInfo> getSequenceVariableInfo(
        DapMetadata metadata,
        String outerSequenceName,
        String innerSequenceName,
        String errorString)
        throws Throwable {

      DodsDapMetadata dodsMetadata = (DodsDapMetadata) metadata;
      Map<String, DapSequenceVariableInfo> infoMap = new HashMap<>();

      BaseType outerVariable = dodsMetadata.dds.getVariable(outerSequenceName);
      if (!(outerVariable instanceof DSequence outerSequence))
        throw new IllegalArgumentException(
            (errorString != null ? errorString : "")
                + "outerVariable not a DSequence: name="
                + outerVariable.getName()
                + " type="
                + outerVariable.getTypeName());

      AttributeTable outerAttributeTable = dodsMetadata.das.getAttributeTable(outerSequenceName);

      // 1. Iterate over outer sequence variables
      Iterator<BaseType> outerVars = outerSequence.getVariables();
      while (outerVars.hasNext()) {
        BaseType outerVar = outerVars.next();
        String oName = outerVar.getName();

        if (innerSequenceName != null && innerSequenceName.equals(oName)) {
          // 2. Handle inner sequence
          DSequence innerSequence = (DSequence) outerVar;
          AttributeTable innerAttributeTable =
              dodsMetadata.das.getAttributeTable(innerSequence.getName());
          Iterator<BaseType> innerVars = innerSequence.getVariables();

          while (innerVars.hasNext()) {
            BaseType innerVar = innerVars.next();
            String iName = innerVar.getName();

            // Skip complex types in inner sequence (DConstructor and DVector are ignored in
            // source)
            if (innerVar instanceof DConstructor || innerVar instanceof DVector) continue;

            PAType sourceType = OpendapHelper.getElementPAType(innerVar.newPrimitiveVector());

            // outerSequenceName + "." + innerSequenceName + "." + iName
            Attributes tAtt =
                getSequenceAttributes(dodsMetadata.das, innerAttributeTable, innerVar, iName);
            infoMap.put(iName, new DapSequenceVariableInfo(iName, sourceType, tAtt, false));
          }

        } else {
          // 3. Handle outer variables
          if (outerVar instanceof DConstructor) continue; // Skip DConstructor types

          PAType sourceType = OpendapHelper.getElementPAType(outerVar.newPrimitiveVector());

          // outerSequenceName + "." + oName
          Attributes tAtt =
              getSequenceAttributes(dodsMetadata.das, outerAttributeTable, outerVar, oName);
          infoMap.put(oName, new DapSequenceVariableInfo(oName, sourceType, tAtt, true));
        }
      }

      return infoMap;
    }

    @Override
    public DapAllVariableSequenceInfo getAllSequenceVariableInfo(DapMetadata metadata)
        throws Throwable {

      DodsDapMetadata dodsMetadata = (DodsDapMetadata) metadata;
      Map<String, DapSequenceVariableInfo> infoMap = new HashMap<>();
      String outerSequenceName = null;
      String innerSequenceName = null;
      Attributes gridMappingAtts = null;

      Iterator<BaseType> variables = dodsMetadata.dds.getVariables();
      while (variables.hasNext()) {
        BaseType datasetVar = variables.next();

        // is this the pseudo-data grid_mapping variable?
        if (gridMappingAtts == null) {
          Attributes tSourceAtts = new Attributes();
          // Use the *strategy* method, not the static dispatcher, to avoid recursion
          this.getAttributes(metadata, datasetVar.getName(), tSourceAtts);
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
                    this.getAttributes(metadata, varName, sourceAtts);
                    if (sourceAtts.size() == 0) {
                      this.getAttributes(
                          metadata,
                          outerSequenceName + "." + innerSequenceName + "." + varName,
                          sourceAtts);
                    }
                    PAType sourceType = OpendapHelper.getElementPAType(innerVar);
                    infoMap.put(
                        varName,
                        new DapSequenceVariableInfo(varName, sourceType, sourceAtts, false));
                  }
                }
              }
            } else if (outerVar instanceof DConstructor) {
              // skip it
            } else {
              // outer variable
              String varName = outerVar.getName();
              Attributes sourceAtts = new Attributes();
              this.getAttributes(metadata, varName, sourceAtts);
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

    @Override
    public Table readOpendapSequence(Table table, String url, boolean skipDapperSpacerRows)
        throws Exception {

      String errorInMethod =
          String2.ERROR + " in DodsDapStrategy.readOpendapSequence(" + url + "):\n";

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

    // --- Private Helper Methods (Moved from original class) ---

    // Internal helper to simplify attribute fetching logic from
    // EDDTableFromDapSequence.java
    private Attributes getSequenceAttributes(
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

    private void addNValueToCol(
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
      else if (bt instanceof DInt32 t)
        ((IntArray) table.getColumn(col)).addN(addCount, t.getValue());
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

    private PrimitiveArray paForBaseType(BaseType bt, String errorInMethod) throws Exception {
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
    private void fetchDapAttributes(
        Table table,
        DAS das,
        AttributeTable attTable,
        BaseType bt,
        int tCol,
        String errorInMethod) {
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
  } // --- End of DodsDapStrategy ---

  // =================================================================================
  // --- Implementation 2: NetcdfDapStrategy (New Implementation) ---
  // =================================================================================

  private static final class NetcdfDapStrategy implements IDapServiceStrategy {

    // Regex to find all bracketed sections, e.g., [0:1:10]
    private static final Pattern RANGE_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

    /**
     * Parses a full OPeNDAP constraint string into a variable name and a list of Ranges.
     *
     * @param constraintString The constraint, e.g., "?temp[0:1:10][100:150]"
     * @return A ParsedConstraint object
     * @throws InvalidRangeException if the range syntax is invalid
     */
    public static List<Range> parseConstraintToRanges(String constraintString)
        throws InvalidRangeException {

      List<Range> ranges = new ArrayList<>();
      Matcher rangeMatcher = RANGE_PATTERN.matcher(constraintString);

      while (rangeMatcher.find()) {
        String rangeStr = rangeMatcher.group(1); // e.g., "0:1:10" or "100:150"
        ranges.add(parseOpendapRange(rangeStr));
      }

      return ranges;
    }

    /**
     * Parses a single OPeNDAP range string into a ucar.ma2.Range. OPeNDAP format is
     * [start:stride:stop] or [start:stop]. ucar.ma2.Range is new Range(first, last, stride).
     *
     * @param opendapRangeStr The string inside the brackets, e.g., "0:1:10"
     * @return A ucar.ma2.Range object
     * @throws InvalidRangeException if the range syntax is invalid
     */
    private static Range parseOpendapRange(String opendapRangeStr) throws InvalidRangeException {
      String[] parts = opendapRangeStr.split(":");

      try {
        if (parts.length == 1) {
          // Format: [index], e.g., [5]
          int index = Integer.parseInt(parts[0]);
          return new Range(index, index, 1);
        } else if (parts.length == 2) {
          // Format: [start:stop] (default stride of 1)
          int start = Integer.parseInt(parts[0]);
          int stop = Integer.parseInt(parts[1]);
          return new Range(start, stop, 1);
        } else if (parts.length == 3) {
          // Format: [start:stride:stop]
          int start = Integer.parseInt(parts[0]);
          int stride = Integer.parseInt(parts[1]);
          int stop = Integer.parseInt(parts[2]);
          return new Range(start, stop, stride);
        } else {
          throw new InvalidRangeException("Invalid OPeNDAP range syntax: " + opendapRangeStr);
        }
      } catch (NumberFormatException e) {
        throw new InvalidRangeException("Invalid number in range: " + opendapRangeStr);
      }
    }

    // --- Concrete NetCDF-Java Handle Implementations ---

    /**
     * Concrete netcdf-java implementation of DapMetadata.
     *
     * <p>NOTE: This object is "stateless" and does not hold an open NetcdfDataset. It holds the URL
     * and the raw bytes (to satisfy the API contract). Methods using this object must open their
     * own NetcdfDataset instance.
     */
    public static class NetcdfDapMetadata extends DapMetadata {
      private final byte[] dasBytes;
      private final byte[] ddsBytes;
      public final String url;
      public final boolean acceptDeflate;

      public NetcdfDapMetadata(byte[] dasBytes, byte[] ddsBytes, String url, boolean acceptDeflate)
          throws Throwable {
        super(netcdfStrategy);
        this.dasBytes = dasBytes;
        this.ddsBytes = ddsBytes;
        this.url = url;
        this.acceptDeflate = acceptDeflate;
        // We do a quick open/close here just to validate the URL.
        try (NetcdfDataset ncd = NetcdfDatasets.openDataset(url)) {
          if (ncd == null) {
            throw new IOException("Unable to open OPeNDAP URL: " + url);
          }
        }
      }

      @Override
      public byte[] getDasBytes() throws IOException {
        return dasBytes;
      }

      @Override
      public byte[] getDdsBytes() throws IOException {
        return ddsBytes;
      }
    }

    /**
     * Concrete netcdf-java implementation of DapVariableInfo. This is a "detached" info object; it
     * copies info from the ncVar so the parent NetcdfDataset can be closed.
     */
    public static class NetcdfDapVariableInfo extends DapVariableInfo {
      private final String name;
      private final PAType paType;
      private final List<String> dimNames;
      private final List<Integer> dimSizes;

      public NetcdfDapVariableInfo(ucar.nc2.Variable ncVar) throws Exception {
        super(netcdfStrategy);
        this.name = ncVar.getShortName();
        this.paType = paTypeForNCType(ncVar.getDataType());
        this.dimNames = new ArrayList<>();
        this.dimSizes = new ArrayList<>();
        for (ucar.nc2.Dimension dim : ncVar.getDimensions()) {
          this.dimNames.add(dim.getShortName());
          this.dimSizes.add(dim.getLength());
        }
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public int getNumDimensions() {
        return dimNames.size();
      }

      @Override
      public String getDimensionName(int av) throws InvalidParameterException {
        return dimNames.get(av);
      }

      @Override
      public int getDimensionSize(int av) throws InvalidParameterException {
        return dimSizes.get(av);
      }

      @Override
      public PAType getSourceDataType() throws Exception {
        return paType;
      }
    }

    // --- Strategy Method Implementations ---

    @Override
    public DapMetadata fetchMetadata(String url, boolean acceptDeflate) throws Throwable {
      // Must fetch bytes to satisfy the DapMetadata contract, even if netcdf-java
      // doesn't strictly need them passed in.
      byte dasBytes[] = SSR.getUrlResponseBytes(url + ".das");
      byte ddsBytes[] = SSR.getUrlResponseBytes(url + ".dds");
      return new NetcdfDapMetadata(dasBytes, ddsBytes, url, acceptDeflate);
    }

    @Override
    public DapMetadata fetchMetadata(
        byte[] dasBytes, byte[] ddsBytes, String url, boolean acceptDeflate) throws Throwable {
      return new NetcdfDapMetadata(dasBytes, ddsBytes, url, acceptDeflate);
    }

    @Override
    public Attributes getAttributes(DapMetadata metadata, String varName, Attributes atts) {
      NetcdfDapMetadata ncMetadata = (NetcdfDapMetadata) metadata;
      try (NetcdfDataset ncd = NetcdfDatasets.openDataset(ncMetadata.url)) {

        ucar.nc2.AttributeContainer attContainer;
        if (varName == null || varName.equals("GLOBAL")) {
          attContainer = ncd.getRootGroup();
        } else {
          ucar.nc2.Variable v = ncd.findVariable(varName);
          if (v == null) {
            String2.log(
                "Warning: NetcdfDapStrategy.getAttributes couldn't find variable: " + varName);
            return atts; // Not found
          }
          attContainer = v;
        }
        addNcAttributes(attContainer, atts);
      } catch (Exception e) {
        throw new RuntimeException("NetcdfDapStrategy.getAttributes failed", e);
      }
      return atts;
    }

    @Override
    public DapVariableInfo getVariableInfo(DapMetadata metadata, String sourceName)
        throws Throwable {
      NetcdfDapMetadata ncMetadata = (NetcdfDapMetadata) metadata;
      try (ucar.nc2.dataset.NetcdfDataset ncd =
          ucar.nc2.dataset.NetcdfDataset.openDataset(ncMetadata.url)) {
        ucar.nc2.Variable v = ncd.findVariable(sourceName);
        if (v == null) {
          throw new NoSuchVariableException(
              "NetcdfDapStrategy: Variable '" + sourceName + "' not found in " + ncMetadata.url);
        }
        // DGrid or DArray equivalent
        if (v.getDataType() == ucar.ma2.DataType.STRUCTURE || v.isScalar()) {
          throw new RuntimeException(
              "Source variable must be a Grid or Array, but "
                  + sourceName
                  + " is "
                  + v.getDataType());
        }
        return new NetcdfDapVariableInfo(v);
      }
    }

    @Override
    public List<DapVariableInfo> getAllVariableInfos(DapMetadata metadata) throws Throwable {
      NetcdfDapMetadata ncMetadata = (NetcdfDapMetadata) metadata;
      List<DapVariableInfo> varInfoList = new ArrayList<>();
      try (ucar.nc2.dataset.NetcdfDataset ncd =
          ucar.nc2.dataset.NetcdfDataset.openDataset(ncMetadata.url)) {
        for (ucar.nc2.Variable v : ncd.getVariables()) {
          // Original code checked for DGrid or DArray.
          // In netcdf-java, this means "not a Structure" and "not scalar".
          if (!v.isScalar() && v.getDataType() != ucar.ma2.DataType.STRUCTURE) {
            varInfoList.add(new NetcdfDapVariableInfo(v));
          }
        }
      }
      return varInfoList;
    }

    @Override
    public PrimitiveArray getAxisValues(DapMetadata metadata, String sourceAxisName)
        throws Throwable {
      NetcdfDapMetadata ncMetadata = (NetcdfDapMetadata) metadata;
      try (NetcdfDataset ncd = NetcdfDatasets.openDataset(ncMetadata.url)) {
        ucar.nc2.Variable v = ncd.findVariable(sourceAxisName);
        if (v == null) {
          throw new NoSuchVariableException(
              "NetcdfDapStrategy: Axis variable '"
                  + sourceAxisName
                  + "' not found in "
                  + ncMetadata.url);
        }
        return readVariableData(v);
      }
    }

    @Override
    public PrimitiveArray[] getGridData(String url, String query, boolean acceptDeflate)
        throws Throwable {

      List<PrimitiveArray> results = new ArrayList<>();

      // String fullUrl = url + ".nc" + query;
      // String2.log("url for grid data: " + fullUrl);

      try (NetcdfDataset ncd = NetcdfDatasets.openDataset(url)) {

        // Find the variable
        ucar.nc2.Variable mainVar = null;
        if (query.startsWith("?")) {
          query = query.substring(1);
        }
        String mainVarName = query.split("\\[")[0].split("\\.")[0].split(",")[0];
        mainVar = ncd.findVariable(mainVarName);

        if (mainVar == null) {
          throw new Exception(
              "NetcdfDapStrategy: No grid/array variable found in response for varName: "
                  + mainVarName);
        }

        List<Range> ranges = parseConstraintToRanges(query);

        // Read the main data slice
        results.add(NcHelper.getPrimitiveArray(mainVar.read(ranges))); // [0] = data

        // Read the axes
        if (mainVar instanceof ucar.nc2.dataset.VariableDS) {
          ucar.nc2.dataset.VariableDS mainVarDS = (ucar.nc2.dataset.VariableDS) mainVar;
          List<ucar.nc2.dataset.CoordinateSystem> csList = mainVarDS.getCoordinateSystems();

          if (csList != null && !csList.isEmpty()) {
            ucar.nc2.dataset.CoordinateSystem cs = csList.get(0);

            // We need to apply the *same ranges* to the axes
            List<ucar.nc2.dataset.CoordinateAxis> axes = cs.getCoordinateAxes();

            // OPeNDAP queries list dimensions in order.
            // We must assume the ranges list and axes list correspond.
            if (axes.size() != ranges.size()) {
              String2.log(
                  "Warning: Query ranges ("
                      + ranges.size()
                      + ") != coordinate axes ("
                      + axes.size()
                      + "). Axes may be incorrect.");
            }
            for (int i = 0; i < axes.size(); i++) {
              ucar.nc2.dataset.CoordinateAxis axis = axes.get(i);
              // An axis might only have one dimension.
              // We need to find the correct range for this axis.
              // Assume order is correct.
              if (i < ranges.size()) {
                List<Range> axisRange = new ArrayList<>();
                axisRange.add(ranges.get(i));
                // Read the *slice* of the axis
                results.add(NcHelper.getPrimitiveArray(axis.read(axisRange)));
              } else {
                // Not enough ranges for this axis? Read the whole thing.
                results.add(NcHelper.getPrimitiveArray(axis.read()));
              }
            }
          }
        }

        return results.toArray(new PrimitiveArray[0]);
      }
    }

    @Override
    public Map<String, DapSequenceVariableInfo> getSequenceVariableInfo(
        DapMetadata metadata,
        String outerSequenceName,
        String innerSequenceName,
        String errorString)
        throws Throwable {

      NetcdfDapMetadata ncMetadata = (NetcdfDapMetadata) metadata;
      Map<String, DapSequenceVariableInfo> infoMap = new HashMap<>();

      try (NetcdfDataset ncd = NetcdfDatasets.openDataset(ncMetadata.url)) {
        ucar.nc2.Variable v = ncd.findVariable(outerSequenceName);
        if (!(v instanceof Sequence outerSequence)) {
          throw new IllegalArgumentException(
              (errorString != null ? errorString : "")
                  + "outerVariable not a Sequence: name="
                  + v.getShortName()
                  + " type="
                  + v.getDataType());
        }

        // 1. Iterate over outer sequence variables
        for (ucar.nc2.Variable outerVar : outerSequence.getVariables()) {
          String oName = outerVar.getShortName();

          if (innerSequenceName != null && innerSequenceName.equals(oName)) {
            // 2. Handle inner sequence
            ucar.nc2.Sequence innerSequence = (ucar.nc2.Sequence) outerVar;
            for (ucar.nc2.Variable innerVar : innerSequence.getVariables()) {
              String iName = innerVar.getShortName();
              if (innerVar.getDataType() == ucar.ma2.DataType.STRUCTURE) continue;

              PAType sourceType = paTypeForNCType(innerVar.getDataType());
              Attributes tAtt = new Attributes();
              addNcAttributes(innerVar, tAtt); // Get attributes directly from the variable
              infoMap.put(iName, new DapSequenceVariableInfo(iName, sourceType, tAtt, false));
            }
          } else {
            // 3. Handle outer variables
            if (outerVar.getDataType() == ucar.ma2.DataType.STRUCTURE) continue;

            PAType sourceType = paTypeForNCType(outerVar.getDataType());
            Attributes tAtt = new Attributes();
            addNcAttributes(outerVar, tAtt);
            infoMap.put(oName, new DapSequenceVariableInfo(oName, sourceType, tAtt, true));
          }
        }
      }
      return infoMap;
    }

    @Override
    public DapAllVariableSequenceInfo getAllSequenceVariableInfo(DapMetadata metadata)
        throws Throwable {

      NetcdfDapMetadata ncMetadata = (NetcdfDapMetadata) metadata;
      Map<String, DapSequenceVariableInfo> infoMap = new HashMap<>();
      String outerSequenceName = null;
      String innerSequenceName = null;
      Attributes gridMappingAtts = null;

      try (NetcdfDataset ncd = NetcdfDatasets.openDataset(ncMetadata.url)) {

        for (ucar.nc2.Variable datasetVar : ncd.getVariables()) {
          // is this the pseudo-data grid_mapping variable?
          if (gridMappingAtts == null) {
            Attributes tSourceAtts = new Attributes();
            addNcAttributes(datasetVar, tSourceAtts);
            gridMappingAtts = NcHelper.getGridMappingAtts(tSourceAtts);
          }

          if (outerSequenceName == null && datasetVar instanceof ucar.nc2.Sequence outerSequence) {
            outerSequenceName = outerSequence.getShortName();

            // get list of outerSequence variables
            for (ucar.nc2.Variable outerVar : outerSequence.getVariables()) {
              if (innerSequenceName == null
                  && outerVar instanceof ucar.nc2.Sequence innerSequence) {
                innerSequenceName = outerVar.getShortName();
                for (ucar.nc2.Variable innerVar : innerSequence.getVariables()) {
                  // inner variable
                  if (innerVar.getDataType() != ucar.ma2.DataType.STRUCTURE) {
                    String varName = innerVar.getShortName();
                    Attributes sourceAtts = new Attributes();
                    addNcAttributes(innerVar, sourceAtts);
                    PAType sourceType = paTypeForNCType(innerVar.getDataType());
                    infoMap.put(
                        varName,
                        new DapSequenceVariableInfo(varName, sourceType, sourceAtts, false));
                  }
                }
              } else if (outerVar.getDataType() != ucar.ma2.DataType.STRUCTURE) {
                // outer variable
                String varName = outerVar.getShortName();
                Attributes sourceAtts = new Attributes();
                addNcAttributes(outerVar, sourceAtts);
                PAType sourceType = paTypeForNCType(outerVar.getDataType());
                infoMap.put(
                    varName, new DapSequenceVariableInfo(varName, sourceType, sourceAtts, true));
              }
            }
          }
        }
      }
      return new DapAllVariableSequenceInfo(
          infoMap, gridMappingAtts, outerSequenceName, innerSequenceName);
    }

    @Override
    public Table readOpendapSequence(Table table, String url, boolean skipDapperSpacerRows)
        throws Exception {
      String2.log("NetcdfDapStrategy.readOpendapSequence url=" + url);

      String errorInMethod =
          String2.ERROR + " in NetcdfDapStrategy.readOpendapSequence(" + url + "):\n";

      try (NetcdfDataset ncd = NetcdfDatasets.openDataset(url)) {

        // 1. Connection and Metadata (Global Attributes)
        addNcAttributes(ncd.getRootGroup(), table.globalAttributes());

        // 2. Data/Structure (Find Outer Sequence)
        Structure outerSequence = null;
        for (Variable v : ncd.getVariables()) {
          String2.log(
              "Found variable: name="
                  + v.getShortName()
                  + ", type="
                  + v.getClass().getName()
                  + ", dataType="
                  + v.getDataType());
          if (v instanceof Structure) {
            outerSequence = (Structure) v;
            String2.log(">>> This is the Structure! <<<"); // DEBUG
            break;
          }
        }
        if (outerSequence == null) {
          throw new Exception(errorInMethod + "firstVariable not a Structure.");
        }

        // 3. Setup Columns (Metadata Only)
        int innerSequenceColumn = -1;
        Structure innerSequence = null;
        List<ucar.nc2.Variable> outerVars = new ArrayList<>();
        List<ucar.nc2.Variable> innerVars = new ArrayList<>();
        int tableCol = 0;

        for (ucar.nc2.Variable obt : outerSequence.getVariables()) {
          String2.log(
              "Found variable: name="
                  + obt.getShortName()
                  + ", type="
                  + obt.getClass().getName()
                  + ", dataType="
                  + obt.getDataType());
          if (obt instanceof Structure) {
            // *** Start Dealing With InnerSequence
            if (innerSequence != null) {
              throw new Exception(errorInMethod + "The response has more than one inner sequence.");
            }
            innerSequence = (Structure) obt;
            innerSequenceColumn = tableCol;

            for (ucar.nc2.Variable ibt : innerSequence.getVariables()) {
              if (ibt.getDataType() == ucar.ma2.DataType.STRUCTURE) continue;
              innerVars.add(ibt);
              PrimitiveArray pa = primitiveArrayForNCType(ibt.getDataType());
              table.addColumn(ibt.getShortName(), pa);
              addNcAttributes(ibt, table.columnAttributes(table.nColumns() - 1));
              tableCol++;
            }
            // *** End Dealing With InnerSequence
          } else {
            // Outer Column
            if (obt.getDataType() == ucar.ma2.DataType.STRUCTURE) continue;
            outerVars.add(obt);
            PrimitiveArray pa = primitiveArrayForNCType(obt.getDataType());
            table.addColumn(obt.getShortName(), pa);
            addNcAttributes(obt, table.columnAttributes(table.nColumns() - 1));
            tableCol++;
          }
        }
        int nInnerColumns = innerVars.size();

        // 4. Read Data (Row-by-Row)
        ArrayStructure data = (ArrayStructure) outerSequence.read();
        int nOuterRows = (int) data.getSize();
        ucar.ma2.StructureMembers.Member innerSeqMember =
            (innerSequence != null)
                ? data.getStructureMembers().findMember(innerSequence.getShortName())
                : null;

        for (int outerRow = 0; outerRow < nOuterRows; outerRow++) {
          // 4a. Get data from innerSequence first (to determine nInnerRows)
          int nInnerRows = 1;
          if (innerSeqMember != null) {
            ucar.ma2.ArrayStructure innerData = data.getArrayStructure(outerRow, innerSeqMember);
            nInnerRows = (int) innerData.getSize();
            if (skipDapperSpacerRows && outerRow < nOuterRows - 1) nInnerRows--;

            for (int innerRow = 0; innerRow < nInnerRows; innerRow++) {
              for (int innerCol = 0; innerCol < nInnerColumns; innerCol++) {
                ucar.nc2.Variable ivar = innerVars.get(innerCol);
                PrimitiveArray pa = table.getColumn(innerSequenceColumn + innerCol);
                addNValueToCol(pa, ivar.getDataType(), innerData, ivar, innerRow, 1);
              }
            }
          }

          // 4b. Process the other outerCol variables, duplicating for nInnerRows
          tableCol = 0;
          for (ucar.nc2.Variable ovar : outerVars) {
            if (tableCol == innerSequenceColumn) {
              tableCol += nInnerColumns;
            }
            PrimitiveArray pa = table.getColumn(tableCol++);
            addNValueToCol(pa, ovar.getDataType(), data, ovar, outerRow, nInnerRows);
          }
        }
      }
      return table;
    }

    // https://ferret.pmel.noaa.gov/pmel/erddap/tabledap/ChukchiSea_454a_037a_fcf4?prof,id,cast,cruise,time,longitude,lon360,latitude&time%3E=2012-09-04&time%3C=2012-09-07&distinct()

    // --- Private Helper Methods ---

    /** Converts a netcdf-java DataType to a PAType. */
    private static PAType paTypeForNCType(ucar.ma2.DataType ncType) throws Exception {
      if (ncType == ucar.ma2.DataType.BYTE) return PAType.BYTE;
      if (ncType == ucar.ma2.DataType.CHAR) return PAType.CHAR;
      if (ncType == ucar.ma2.DataType.SHORT) return PAType.SHORT;
      if (ncType == ucar.ma2.DataType.INT) return PAType.INT;
      if (ncType == ucar.ma2.DataType.LONG) return PAType.LONG;
      if (ncType == ucar.ma2.DataType.FLOAT) return PAType.FLOAT;
      if (ncType == ucar.ma2.DataType.DOUBLE) return PAType.DOUBLE;
      if (ncType == ucar.ma2.DataType.STRING) return PAType.STRING;
      if (ncType == ucar.ma2.DataType.BOOLEAN) return PAType.BYTE; // Store as byte
      // OPAQUE, STRUCTURE, SEQUENCE, ENUM...
      throw new Exception("NetcdfDapStrategy: Unsupported ncType: " + ncType);
    }

    /** Creates an empty PrimitiveArray for a given netcdf-java DataType. */
    private static PrimitiveArray primitiveArrayForNCType(ucar.ma2.DataType ncType)
        throws Exception {
      if (ncType == ucar.ma2.DataType.BYTE) return new ByteArray();
      if (ncType == ucar.ma2.DataType.CHAR) return new StringArray(); // Store CHAR as String
      if (ncType == ucar.ma2.DataType.SHORT) return new ShortArray();
      if (ncType == ucar.ma2.DataType.INT) return new IntArray();
      if (ncType == ucar.ma2.DataType.LONG) return new LongArray();
      if (ncType == ucar.ma2.DataType.FLOAT) return new FloatArray();
      if (ncType == ucar.ma2.DataType.DOUBLE) return new DoubleArray();
      if (ncType == ucar.ma2.DataType.STRING) return new StringArray();
      if (ncType == ucar.ma2.DataType.BOOLEAN) return new ByteArray(); // Store as byte
      throw new Exception("NetcdfDapStrategy: Unsupported ncType for PA: " + ncType);
    }

    /** Reads all data from a ucar.nc2.Variable and returns it as a PrimitiveArray. */
    private PrimitiveArray readVariableData(ucar.nc2.Variable v) throws Exception {
      ucar.ma2.Array data = v.read();
      PrimitiveArray pa =
          PrimitiveArray.factory(paTypeForNCType(v.getDataType()), (int) data.getSize(), false);
      Object storage = data.getStorage();
      if (storage.getClass().isArray()) {
        pa.append(PrimitiveArray.factory(storage));
      } else {
        // Fallback for complex storage
        ucar.ma2.IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          pa.addDouble(iter.getDoubleNext()); // Add as double, PA will convert
        }
      }
      return pa;
    }

    /** Copies attributes from a netcdf-java AttributeContainer to a cohort Attributes object. */
    private void addNcAttributes(ucar.nc2.AttributeContainer ncAtts, Attributes cohortAtts) {
      for (ucar.nc2.Attribute ncAtt : ncAtts.getAttributes()) {
        try {
          String name = ncAtt.getShortName();
          PrimitiveArray pa;
          if (ncAtt.isString()) {
            pa = new StringArray();
            int n = ncAtt.getLength();
            for (int i = 0; i < n; i++) {
              pa.addString(ncAtt.getStringValue(i));
            }
          } else {
            ucar.ma2.Array data = ncAtt.getValues();
            pa =
                PrimitiveArray.factory(
                    paTypeForNCType(ncAtt.getDataType()), (int) data.getSize(), false);
            Object storage = data.getStorage();
            if (storage.getClass().isArray()) {
              pa.append(PrimitiveArray.factory(storage));
            } else {
              // Fallback for complex storage
              ucar.ma2.IndexIterator iter = data.getIndexIterator();
              while (iter.hasNext()) {
                pa.addDouble(iter.getDoubleNext()); // Add as double, PA will convert
              }
            }
          }
          cohortAtts.add(name, pa);
        } catch (Exception e) {
          String2.log(
              "Warning: NetcdfDapStrategy.addNcAttributes failed to read attribute: "
                  + ncAtt.getShortName()
                  + " ("
                  + e.getMessage()
                  + ")");
        }
      }
    }

    /**
     * Helper to read a single value from an ArrayStructure and add it N times to a PrimitiveArray.
     */
    private void addNValueToCol(
        PrimitiveArray pa,
        ucar.ma2.DataType ncType,
        ucar.ma2.ArrayStructure data,
        Variable var,
        int index,
        int addCount)
        throws Exception {

      ucar.ma2.StructureMembers.Member member =
          data.getStructureMembers().findMember(var.getShortName());
      if (ncType == ucar.ma2.DataType.BYTE)
        ((ByteArray) pa).addN(addCount, data.getScalarByte(index, member));
      else if (ncType == ucar.ma2.DataType.CHAR) // Treat CHAR as String
      ((StringArray) pa).addN(addCount, data.getScalarString(index, member));
      else if (ncType == ucar.ma2.DataType.SHORT)
        ((ShortArray) pa).addN(addCount, data.getScalarShort(index, member));
      else if (ncType == ucar.ma2.DataType.INT)
        ((IntArray) pa).addN(addCount, data.getScalarInt(index, member));
      else if (ncType == ucar.ma2.DataType.LONG)
        ((LongArray) pa).addN(addCount, data.getScalarLong(index, member));
      else if (ncType == ucar.ma2.DataType.FLOAT)
        ((FloatArray) pa).addN(addCount, data.getScalarFloat(index, member));
      else if (ncType == ucar.ma2.DataType.DOUBLE)
        ((DoubleArray) pa).addN(addCount, data.getScalarDouble(index, member));
      else if (ncType == ucar.ma2.DataType.STRING)
        ((StringArray) pa).addN(addCount, data.getScalarString(index, member));
      else if (ncType == ucar.ma2.DataType.BOOLEAN)
        ((ByteArray) pa).addN(addCount, data.getScalarByte(index, member));
      else {
        throw new Exception(
            "NetcdfDapStrategy: Unexpected inner variable type="
                + ncType
                + " for name="
                + member.getName());
      }
    }
  } // --- End of NetcdfDapStrategy ---
}
