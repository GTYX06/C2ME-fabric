<p align="center">
  <img src="https://github.com/RelativityMC/C2ME-fabric/raw/ver/1.17/src/main/resources/assets/c2me/icon.png" width="140" alt="C2ME Logo" />
</p>

<h1 align="center">C²M-Engine (CUDA Edition)</h1>

<p align="center">
  <b>A next-generation Fabric mod designed to revolutionize Minecraft chunk performance with Multithreading & NVIDIA CUDA GPU Acceleration.</b>
</p>

<p align="center">
  <a href="https://github.com/RelativityMC/C2ME-fabric/actions"><img src="https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square" alt="Build Status" /></a>
  <a href="https://developer.nvidia.com/cuda-toolkit"><img src="https://img.shields.io/badge/CUDA-12.0+-76B900?style=flat-square&logo=nvidia&logoColor=white" alt="CUDA 12+" /></a>
  <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Fabric-1.21+-blue?style=flat-square" alt="Fabric" /></a>
  <a href="https://adoptium.net/"><img src="https://img.shields.io/badge/Java-25+-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java 25+" /></a>
  <a href="https://discord.gg/Kdy8NM5HW4"><img src="https://img.shields.io/discord/756715786747248641?logo=discord&logoColor=white&style=flat-square&label=Discord" alt="Discord" /></a>
  <a href="/LICENSE.md"><img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License" /></a>
</p>

---

## ⚡ What is C2ME?

**C²M-Engine** (*Concurrent Chunk Management Engine*) is a high-performance optimization mod for Minecraft Fabric that accelerates chunk generation, I/O, and loading.

This edition features a **native NVIDIA CUDA acceleration backend** that offloads heavy 3D terrain density calculations, multi-octave Perlin/Simplex noise evaluation, 6D biome searching, and aquifer simulation directly to your GPU.

```
                    ┌───────────────────────────────┐
                    │      Minecraft World Gen      │
                    └───────────────┬───────────────┘
                                    │
                       (Batched Chunks 16-64x)
                                    │
                                    ▼
       ┌────────────────────────────────────────────────────────┐
       │   C2ME Multi-Stream CUDA Engine (32 Parallel Streams)  │
       └────┬───────────────────────┬──────────────────────┬────┘
            │                       │                      │
            ▼                       ▼                      ▼
┌───────────────────────┐┌────────────────────┐┌────────────────────────┐
│  3D Density Functions ││ 6D Biome MultiNoise││ Stage 1 Height & Aquifer│
│   (JIT NVRTC C++)     ││   KD-Tree Search   ││       Prefilling       │
└───────────────────────┘└────────────────────┘└────────────────────────┘
            │                       │                      │
            └───────────────────────┼──────────────────────┘
                                    │
                     (Async Direct DMA Off-Heap)
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │  Zero-Allocation Paletted     │
                    │       Chunk Sections          │
                    └───────────────────────────────┘
```

---

## 🌟 Key Features

### 🟢 Native NVIDIA CUDA 12+ Acceleration
* **NVRTC Dynamic JIT Compilation**: Compiles Minecraft's exact mathematical Density Function tree directly into optimized native CUDA C++ kernels at world load.
* **32 Concurrent Hardware Streams**: Non-blocking asynchronous chunk generation pipelines saturating GPU Streaming Multiprocessors (SMs).
* **Direct Off-Heap Memory Transfer**: Uses Java 25 Foreign Function & Memory (FFM) API for zero-copy DMA transfers between device VRAM and off-heap host memory.
* **Sub-millisecond Shader Cache**: Precompiled PTX kernels are compressed and cached in `.c2me-cuda-cache` for instant subsequent world loads (< 1 ms).

### 🚀 Full Worldgen Pipeline Offloaded to GPU
* **3D Density & Voxel Placement**: Computes complete 3D block states ($16 \times 16 \times 384$ blocks per chunk) directly on GPU cores.
* **6D Multi-Noise Biome Tree Search**: Traverses climate parameter bounding boxes in parallel on the GPU to determine biomes per voxel column.
* **Noise Samplers**: Full GPU implementation of Perlin Octaves, 2D/3D Simplex noise, End Islands, and Interpolated Noise.
* **Hermite Splines & Math**: Evaluates complex multi-point climate curves with hardware-accelerated Horner-form FMA (Fused Multiply-Add).
* **Stage 1 Terrain Prefill**: GPU-evaluated surface height estimations and 3D aquifer water/lava barriers.

### 🧵 Concurrent CPU Multithreading & I/O
* **Asynchronous Chunk I/O**: High-throughput multi-threaded chunk loading and saving.
* **Scheduled Lighting Engine**: Non-blocking asynchronous lighting updates.
* **No-Tick & Extended View Distance**: Massive render distance support without ticking performance degradation.

---

## 💻 System Requirements

| Component | Requirement |
|---|---|
| **GPU** | NVIDIA GeForce GTX 16-series, RTX 20/30/40/50-series, or newer |
| **CUDA Driver** | NVIDIA Driver supporting CUDA 12.0+ (driver version $\ge 525.60$) |
| **Java** | OpenJDK 25+ (e.g. Eclipse Temurin 25) |
| **Mod Loader** | Fabric Loader |
| **Recommended Mods** | [Lithium](https://modrinth.com/mod/lithium), [FerriteCore](https://modrinth.com/mod/ferrite-core) |

---

## 🛠️ Building from Source

### Prerequisites
* JDK 25+ (Temurin recommended)
* Git

### Build Instructions

```bash
# Clone the repository with submodules
git clone --recursive https://github.com/GTYX06/C2ME-fabric.git
cd C2ME-fabric

# Build the mod jars
./gradlew clean build
```

The compiled mod artifacts will be located at:
* **All-in-one Mod Jar**: `build/libs/c2me-fabric-mc*.jar`
* **CUDA Acceleration Module**: `c2me-opts-accel-cuda/build/libs/`

---

## 🧩 Compatibility & Vanilla Parity

* **Vanilla Parity**: C2ME preserves 100% of vanilla world generation math and structure distribution.
* **Datapacks & Mods**: Fully compatible with custom datapacks, vanilla dimensions, and world generation mods (such as *Lithostitched*).
* **Graceful CPU Fallback**: If no compatible NVIDIA GPU or CUDA driver is found, C2ME smoothly falls back to multi-threaded CPU generation.

---

## 📜 License

* **Core C2ME & Submodules**: Licensed under the **MIT License** — Copyright (c) 2021-2026 ishland / RelativityMC.
* **CUDA Acceleration Module (`c2me-opts-accel-cuda`)**: Licensed under the **MIT License** — Copyright (c) 2026 GTYX06.

See [LICENSE.md](/LICENSE.md) and [`licenses/LICENSE-MIT-GTYX06.txt`](/licenses/LICENSE-MIT-GTYX06.txt) for full license terms.
