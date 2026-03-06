# Multi-Threaded News Aggregator

## Overview
A high-performance, concurrent Java application designed to ingest, process, deduplicate, and analyze large datasets of JSON news articles. This project demonstrates advanced multi-threading capabilities, efficient data synchronization, and scalable architecture to handle significant data volumes.

## 🚀 Key Features
* **High-Speed Data Ingestion:** Reads and parses thousands of JSON files in parallel using worker threads.
* **Concurrent Deduplication:** Utilizes `ConcurrentHashMap` to efficiently identify and remove duplicate articles based on unique IDs (`uuid`) or titles.
* **Automated Categorization & Localization:** Routes articles into specific category and language files based on predefined lists, ensuring thread-safe file writing.
* **Chronological Aggregation:** Generates a unified global timeline of all processed articles, sorted chronologically and lexicographically.
* **Keyword Analysis:** Performs text processing to count specific keywords in English articles, while safely ignoring predefined linking words.
* **Statistical Reporting:** Automatically generates comprehensive reports, including top authors, most popular categories, and most recent publications.

## 🛠️ Tech Stack
* **Language:** Java (OpenJDK 25.0.1)
* **Concurrency:** Java Threads, `ConcurrentHashMap`
* **Data Processing:** Jackson (for JSON parsing)
* **Build Tools:** Makefile

## 🧠 Parallelization Strategy
The core of this application relies on a fixed-size thread pool initiated at runtime. The workload is divided to maximize throughput and minimize race conditions:
* **Data Partitioning:** Each worker thread selects a specific chunk of files to process and convert into `Article` objects.
* **Lock-Free Lookups:** Parsed objects are stored in two `ConcurrentHashMap` instances for rapid, thread-safe duplicate detection.
* **Task Distribution:** The array of unique articles is split into equal sections for categorization, language mapping, and keyword counting.
* **Isolated File I/O:** File writing tasks are strictly divided among workers to guarantee no race conditions occur during output generation.

## 📊 Performance & Scalability
The parallel architecture significantly reduces execution time compared to sequential processing. During testing on WSL with an Intel i5-12450H processor and a dataset of 5,516 JSON files:
* **Speedup:** Achieved a speedup of ~2.5x using 6 threads.
* **Efficiency:** The performance curve demonstrates near-asymptotic behavior around 4-6 threads. This is primarily bottlenecked by unavoidable sequential file I/O operations and final data sorting requirements.
* 
