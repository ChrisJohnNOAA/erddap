# EDDTableFromZarr Production Deployment & Performance Tuning Guide

## 1. Overview & Architecture

`EDDTableFromZarr` is ERDDAP's tabular dataset execution engine designed for accessing 1D, 2D char/string, and 0D scalar Zarr stores using the `zarr-java` library. It enables high-throughput data access over local file systems, HTTP/HTTPS web endpoints, and cloud object stores such as AWS S3.

### Core Execution Flow

```
+-------------------------------------------------------------------------------+
|                            Client DAP Request                                 |
+-------------------------------------------------------------------------------+
                                        |
                                        v
+-------------------------------------------------------------------------------+
|                Pass 1: Constraint Evaluation & Bitset Masking                 |
|                                                                               |
| 1. Read row chunks for constraint variables (batch: 100 to 100,000 rows).     |
| 2. Scale/offset unpack source arrays to destination units.                    |
| 3. Apply user DAP constraints to maintain a high-performance BitSet mask.     |
+-------------------------------------------------------------------------------+
                                        |
                                        v
+-------------------------------------------------------------------------------+
|                    Pass 2: Selected Data Extraction & Streaming               |
|                                                                               |
| 1. Extract requested column data ONLY for rows matching the BitSet mask.      |
| 2. Unwritten chunks populate typed missing/fill sentinels automatically.      |
| 3. Stream converted tables directly to TableWriter (.csv, .json, .nc, etc.).  |
+-------------------------------------------------------------------------------+
```

---

## 2. Integration & Endpoint Testing Procedures

### REST Endpoint Verification Checklist

To verify dataset compatibility across standard ERDDAP REST serialization endpoints, execute DAP requests against target `EDDTableFromZarr` datasets and inspect the generated response files:

| Format | URL Extension | Verification Criteria |
| :--- | :--- | :--- |
| **CSV** | `.csv` | Check column headers line match `destinationName` fields and missing values map to standard ERDDAP missing representations. |
| **JSON** | `.json` | Validate structure contains `"table"` dictionary with `"columnNames"`, `"columnTypes"`, and `"rows"` array. |
| **HTML Table** | `.htmlTable` | Verify output contains valid HTML table markup (`<table>...</table>`) with formatted header metadata. |
| **TSV** | `.tsv` | Ensure columns are tab-separated (`\t`) with appropriate line endings (`\n`). |
| **NetCDF** | `.nc` | Open generated NetCDF file with `ncdump` or `cdmReader` to verify attributes and variable dimensions. |
| **MATLAB** | `.mat` | Load file in MATLAB / SciPy (`scipy.io.loadmat`) to confirm array shapes and numeric accuracy. |

### Schema Modification & Reload Validation

When underlying Zarr stores are updated (e.g., expanded observation rows or appended variables), ERDDAP must re-initialize dataset metadata without restarting the server:

1. **Flag Touch Reload Trigger**:
   - Touch the reload flag in ERDDAP's data directory:
     ```bash
     touch <bigParentDirectory>/erddap/flag/<datasetID>
     ```
2. **Admin REST Reload Trigger**:
   - Issue an HTTP GET request to the administrative flag endpoint:
     ```http
     GET /erddap/admin/setDatasetFlag?datasetID=<datasetID>&flagKey=<adminFlagKey>
     ```
3. **Verification**:
   - Confirm in `logs/log.txt` that `EDDTableFromZarr` re-executes `parseZarrMetadata()`, updates `numRows`, and logs successful dataset reloading.

---

## 3. Performance Benchmarking & IO Tuning Guide

### Filter-First Bitset Pass vs. Full Table Scan Benchmarks

`EDDTableFromZarr` uses a two-pass constraint evaluator. Pass 1 reads only constraint variables to build a `BitSet` row mask, allowing Pass 2 to skip downloading data chunks for non-matching rows.

#### Profiling Procedure

1. **Setup Benchmark Dataset**:
   - Select an S3-backed Zarr store containing $>1,000,000$ observation rows across multiple chunk files.
2. **Execute Full Table Query (Baseline)**:
   - Query all columns without restrictive constraints:
     ```http
     GET /erddap/tabledap/s3_zarr_dataset.csv?time,latitude,longitude,salinity,temperature
     ```
3. **Execute High-Selectivity Bitset Filter Query**:
   - Apply a constraint matching $<5\%$ of total rows:
     ```http
     GET /erddap/tabledap/s3_zarr_dataset.csv?time,latitude,longitude,salinity,temperature&temperature>28.5
     ```
4. **Metrics to Record**:
   - **Response Latency (TTFB and total time)**.
   - **Total Network Data Transferred (S3 GetObject requests and total payload bytes)**.
   - **JVM Memory Allocations & GC Pause Frequency**.

---

## 4. Memory & Cache Allocation Strategy

### Formula for Balancing `chunkCacheSize` against JVM Heap (`-Xmx`)

`zarr-java` maintains 1D chunk caches per variable array. To prevent garbage collection thrashing and `OutOfMemoryError` under high concurrency:

$$\text{Total Active Dataset Cache} \le 0.10 \times \text{JVM Heap (-Xmx)}$$

For a single `EDDTableFromZarr` dataset instance:

$$\text{chunkCacheSize} = \left\lfloor \frac{\text{JVM Heap (-Xmx)} \times 0.10}{N_{\text{active\_datasets}} \times N_{\text{variables\_per\_dataset}}} \right\rfloor$$

#### Configuration Example

- **Server JVM Heap (`-Xmx`)**: 32 GB.
- **Active Zarr Datasets**: 10 datasets.
- **Average Variables per Dataset**: 10 variables.
- **Available Chunk Memory**: $32\text{ GB} \times 0.10 = 3.2\text{ GB}$.
- **Recommended `chunkCacheSize` per Dataset**: $\approx 320\text{ MB}$ ($335,544,320$ bytes).

Set tag in `datasets.xml`:
```xml
<chunkCacheSize>335544320</chunkCacheSize>
```

---

## 5. Cloud S3 & HTTP Connection Sizing

For datasets hosted on AWS S3 or HTTP object stores, configure connection pooling and thread safety in `setup.xml` or environment variables:

| Parameter | Recommended Value | Description |
| :--- | :--- | :--- |
| `<s3TargetThroughputInGbps>` | `10.0` – `20.0` | S3 Transfer Manager target throughput for high-bandwidth cloud instances (e.g., AWS EC2/EKS). |
| `<s3MaxConcurrency>` | `50` – `200` | Maximum parallel S3 HTTP/2 connection threads per client instance. |
| `awsRegion` | Region string (e.g., `us-west-2`) | AWS Region hosting the target S3 bucket. |
| `awsEndpoint` | S3-compatible URL or `null` | Custom S3 endpoint URL for MinIO or Ceph object storage. |

---

## 6. Complete `datasets.xml` Tag Reference

Below is the complete XML tag reference for `<dataset type="EDDTableFromZarr">`:

```xml
<dataset type="EDDTableFromZarr" datasetID="s3_zarr_table_example" active="true">
    <!-- Mandatory Zarr Store Location -->
    <zarrStorePath>s3://my-ocean-bucket/zarr_datasets/table_obs.zarr</zarrStorePath>

    <!-- Optional Zarr Sub-group Name (default is root "") -->
    <zarrGroupName></zarrGroupName>

    <!-- Optional Row Dimension Name (auto-discovered if omitted: "obs", "row", "time", "index") -->
    <rowDimensionName>obs</rowDimensionName>

    <!-- Memory & Cache Settings -->
    <chunkCacheSize>335544320</chunkCacheSize>
    <cacheFromUrl>true</cacheFromUrl>

    <!-- AWS S3 Credentials & Storage Settings -->
    <awsRegion>us-west-2</awsRegion>
    <awsEndpoint>https://s3.us-west-2.amazonaws.com</awsEndpoint>

    <!-- Core ERDDAP Parameters -->
    <reloadEveryNMinutes>60</reloadEveryNMinutes>
    <defaultDataQuery>time,latitude,longitude,temperature&amp;temperature&gt;15.0</defaultDataQuery>

    <!-- Global Metadata Overrides -->
    <addAttributes>
        <att name="title">S3 Tabular Zarr Production Dataset</att>
        <att name="summary">High-resolution oceanographic tabular observation dataset stored in Zarr format.</att>
        <att name="institution">NOAA / ERDDAP Integration</att>
        <att name="cdm_data_type">Point</att>
    </addAttributes>

    <!-- Variable Definitions (Auto-discovered if omitted) -->
    <dataVariable>
        <sourceName>raw_temp</sourceName>
        <destinationName>temperature</destinationName>
        <dataType>double</dataType>
        <addAttributes>
            <att name="units">degree_C</att>
            <att name="long_name">Sea Water Temperature</att>
            <att name="ioos_category">Temperature</att>
        </addAttributes>
    </dataVariable>
</dataset>
```
