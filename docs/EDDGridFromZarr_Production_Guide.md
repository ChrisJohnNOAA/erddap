# ERDDAP `EDDGridFromZarr` Production Deployment & Performance Guide

## 1. Overview & Architecture

`EDDGridFromZarr` integrates Zarr v2 and Zarr v3 datasets into ERDDAP as gridded datasets (`EDDGrid`). Supported backends include:
* Local filesystems (`FilesystemStore`)
* Public and private HTTP/HTTPS endpoints (`HttpStore`)
* Amazon S3 cloud object stores (`S3Store`) via AWS SDK v2

### Key Architectural Highlights
* **Zero-Copy Vector Storage & Slicing**: Slices multidimensional Zarr arrays dynamically on demand into flat C-order row-major arrays.
* **On-the-Fly Unpacking**: Handles missing value transformations (`_FillValue`, `missing_value`, `NaN`) and linear scale-offset transformations (`scale_factor`, `add_offset`).
* **Metadata Auto-Discovery**: Discovers Zarr v3 `dimensionNames` and Zarr v2 `_ARRAY_DIMENSIONS` xarray metadata to automatically structure grid axes and data variables.

---

## 2. Build System & Dependency Manifest

### Maven Coordinates (`pom.xml`)

Ensure the following dependencies are included in `pom.xml`:

```xml
<!-- Zarr Core Java Library -->
<dependency>
    <groupId>dev.zarr</groupId>
    <artifactId>zarr-java</artifactId>
    <version>0.2.0</version>
</dependency>

<!-- AWS SDK v2 S3 Client -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.1</version>
</dependency>

<!-- AWS S3 Async Transfer Manager & Netty Async HTTP Engine -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3-transfer-manager</artifactId>
    <version>2.25.1</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk.crt</groupId>
    <artifactId>aws-crt</artifactId>
    <version>0.29.11</version>
</dependency>
```

---

## 3. Performance Benchmarking & Cache Tuning

High-throughput cloud datasets require balancing chunk-level caching in `zarr-java` against ERDDAP's high-level memory caching.

### Chunk Caching vs. ERDDAP Internal RAM Cache

1. **`zarr-java` Chunk Byte Cache (`chunkCacheSize`)**:
   * Controls byte-array caching of uncompressed/compressed Zarr array chunks.
   * **Recommended Value**: `1073741824` (1 GB) to `4294967296` (4 GB) per dataset, depending on available heap.
   * **Setting in `datasets.xml`**:
     ```xml
     <chunkCacheSize>2147483648</chunkCacheSize> <!-- 2 GB -->
     ```

2. **ERDDAP High-Level RAM Cache (`EDDGrid` Cache)**:
   * Configured globally in `setup.xml` via `<cacheMegabytes>` or per-dataset via `<dimensionValuesInMemory>true</dimensionValuesInMemory>`.
   * **Recommendation**: Leave coordinate axis dimension values in memory (`<dimensionValuesInMemory>true</dimensionValuesInMemory>`) to avoid repeatedly fetching 1D axis arrays from S3 or disk.

### Cloud I/O Profiling & Network Tuning (S3 & HTTP Backends)

When serving data from high-latency remote storage (e.g., AWS S3 or HTTP object stores):

1. **S3 Target Throughput & Connection Pool Sizing**:
   * Set ERDDAP setup environment variables or `setup.xml` tags for AWS CRT client tuning:
     ```xml
     <s3TargetThroughputInGbps>20.0</s3TargetThroughputInGbps>
     <s3MaxConcurrency>100</s3MaxConcurrency>
     ```
2. **Worker Thread Pool Limits (`nThreads`)**:
   * Set `<nThreads>` in `datasets.xml` to match available CPU logical cores for parallel multi-chunk extraction:
     ```xml
     <nThreads>8</nThreads>
     ```

---

## 4. End-to-End Integration Testing & Verification

### Web Endpoint Output Format Verification

Verify that all standard ERDDAP griddap endpoints return proper headers and data payloads for `EDDGridFromZarr` datasets:

* **DAS Metadata (`.das`)**: `https://erddap.example.org/erddap/griddap/zarr_dataset.das`
* **DDS Structure (`.dds`)**: `https://erddap.example.org/erddap/griddap/zarr_dataset.dds`
* **HTML Table (`.htmlTable`)**: `https://erddap.example.org/erddap/griddap/zarr_dataset.htmlTable?sst[0:1][0:10][0:10]`
* **NetCDF-3 / NetCDF-4 (`.nc`)**: `https://erddap.example.org/erddap/griddap/zarr_dataset.nc?sst[0:1][0:10][0:10]`
* **CSV Data Export (`.csv`)**: `https://erddap.example.org/erddap/griddap/zarr_dataset.csv?sst[0:1][0:10][0:10]`
* **PNG Map Generation (`.png`)**: `https://erddap.example.org/erddap/griddap/zarr_dataset.png?sst[0][:][:]`

### Dataset Reload Verification (`flag.txt`)

To force ERDDAP to re-initialize an `EDDGridFromZarr` dataset when source Zarr store metadata or attributes change:
1. Touch `flag.txt` with dataset ID:
   ```bash
   echo "zarr_dataset_id" >> /erddapData/flag/flag.txt
   ```
2. Or trigger the REST admin reload endpoint:
   ```http
   GET /erddap/admin/reload?datasetID=zarr_dataset_id
   ```

---

## 5. Production `datasets.xml` Reference

```xml
<dataset type="EDDGridFromZarr" datasetID="zarr_global_sst" active="true">
    <reloadEveryNMinutes>1440</reloadEveryNMinutes>
    <zarrStorePath>s3://ocean-data-bucket/zarr/global_sst.zarr</zarrStorePath>
    <zarrGroupName></zarrGroupName>
    <chunkCacheSize>2147483648</chunkCacheSize>
    <nThreads>8</nThreads>
    <dimensionValuesInMemory>true</dimensionValuesInMemory>

    <addAttributes>
        <att name="title">Global Sea Surface Temperature (Zarr v3)</att>
        <att name="summary">High resolution SST dataset served directly from cloud S3 object store.</att>
        <att name="institution">NOAA / NESDIS</att>
        <att name="infoUrl">https://example.org/info/sst</att>
        <att name="cdm_data_type">Grid</att>
    </addAttributes>

    <axisVariable>
        <sourceName>time</sourceName>
        <destinationName>time</destinationName>
    </axisVariable>
    <axisVariable>
        <sourceName>latitude</sourceName>
        <destinationName>latitude</destinationName>
    </axisVariable>
    <axisVariable>
        <sourceName>longitude</sourceName>
        <destinationName>longitude</destinationName>
    </axisVariable>

    <dataVariable>
        <sourceName>sst</sourceName>
        <destinationName>sst</destinationName>
        <dataType>double</dataType>
        <addAttributes>
            <att name="ioos_category">Temperature</att>
            <att name="long_name">Sea Surface Temperature</att>
            <att name="units">degree_C</att>
        </addAttributes>
    </dataVariable>
</dataset>
```
