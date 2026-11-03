# Postgres Random Access Pagination Benchmark (Accompanying Repository)

This repository contains the full source code, SQL benchmark queries, load runners, and utility scripts used in the article [Random Access On A Huge Postgres Dataset](https://blog.fortunate.work/posts/2026-11-15-random-access-limits-on-a-huge-dataset/)

## Quick Start

### 1. Prerequisites
Ensure you have the following installed on your machine:
- **Java 21**
- **Maven**
- **Docker** and **Docker Compose**

### 2. Launch the Database
```bash
docker compose up -d
``` 

### 3. Load Datasets & Run Benchmarks

#### Step A: Benchmark 100K Rows
Load the baseline dataset and run the miniscule offset benchmarks:
```bash
./scripts/load/01a-load-100K-records.sh
./scripts/run/01b-run-miniscule-offset.sh
```
#### Step B: Benchmark 1,000,000 Rows (The Cache Thrashing Threshold)
Load the 1M dataset and execute standard offset runs. Observe the dramatic transition from buffer-hits to disk reads:
```bash
./scripts/load/02a-load-1MIL-records.sh
./scripts/run/02b-run-normal-offset.sh
./scripts/run/02c-run-large-offset.sh
./scripts/run/02d-run-deferred-join-large-offset.sh
```
#### Step C: Benchmark 10,000,000 Rows (Scaling Beyond Naive Limits)
Load the 10M dataset and run optimized keyset pagination benchmarks:
```bash
./scripts/load/03a-load-10MIL-records.sh
./scripts/run/03b-run-keyset-pagination-large-offset.sh
./scripts/run/03c-run-keyset-pagination-huge-offset.sh
```

## Query Validation

To guarantee that the optimized keyset-checkpoint query produces the exact same results as standard pagination, run the verification test-suite:
```bash
./scripts/04-validate-checkpointing.sh
```
---

## Clean Up & Teardown

To stop the containers and reclaim allocated Docker volume directories:
```bash
./scripts/05-teardown.sh
```
