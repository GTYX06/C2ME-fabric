<img width="200" src="https://github.com/RelativityMC/C2ME-fabric/raw/ver/1.17/src/main/resources/assets/c2me/icon.png" alt="C2ME icon" align="right">
<div align="left">
<h1>C^2M-Engine (Vector API Fork)</h1>

[![Github-CI](https://github.com/GTYX06/C2ME-fabric/workflows/C2ME%20Build%20Script/badge.svg)](https://github.com/GTYX06/C2ME-fabric/actions?query=workflow%3ACI)
[![Discord](https://img.shields.io/discord/756715786747248641?logo=discord&logoColor=white)](https://discord.gg/Kdy8NM5HW4)
<h3>A Fabric mod designed to improve the chunk performance of Minecraft.</h3>
</div>

> [!NOTE]
> **Fork Information**: This repository is a specialized fork of [RelativityMC/C2ME-fabric](https://github.com/RelativityMC/C2ME-fabric).  
> It replaces the original native C math optimization module (`c2me-opts-natives-math` / C-based native libraries with Clang requirements) with pure Java SIMD vectorization using the **Java Vector API** (`c2me-opts-math` via `jdk.incubator.vector`).

---

## Fork Features & Key Differences from Upstream

- **Java Vector API SIMD Acceleration**: Replaces C-native JNI/FFI math bindings with Java Vector API implementations (`VectorAquiferSampler`, `VectorBiomeAccess`, `VectorBiomeSearchTree`, `VectorEndIslands`, `VectorInterpolatedNoise`, `VectorPerlinNoise`, `VectorMathUtil`).
- **No Native C Compiler / LLVM Dependency**: Cross-platform pure Java execution without requiring native binary compiling or system-specific shared libraries (`.so`, `.dll`, `.dylib`).
- **Zero-Allocation Optimizations**: Uses `ThreadLocal` vector buffers to eliminate array allocations during noisy density/biome calculations.
- **Flexible Configuration**: Adds `vanillaWorldGenOptimizations.useVectorAPI` configuration mode (`DEFAULT`, `OFF`, `AVX2`, `AVX512`) in `c2me.toml`.
- **Friendly Startup Check**: Includes Fabric pre-launch validation that provides clear error notifications and instructions if `--add-modules jdk.incubator.vector` is missing from launch JVM arguments.

---

## Java Vector API Setup Requirement

Because Java Vector API is currently an incubator module in modern JDKs (JDK 21/22+), you **must** pass the `--add-modules` flag to your JVM launch arguments when running this mod with Vector API optimizations enabled.

### JVM Launch Arguments
```text
--add-modules jdk.incubator.vector
```

> [!IMPORTANT]
> If `--add-modules jdk.incubator.vector` is omitted, C2ME will prompt a Fabric error screen at startup explaining how to add the flag or how to set `vanillaWorldGenOptimizations.useVectorAPI = "OFF"` in `c2me.toml`.

---

## Configuration (`c2me.toml`)

In `config/c2me.toml`, you can configure Vector API optimization behavior:

```toml
[vanillaWorldGenOptimizations]
# Defines the Vector API mode to use for world generation math optimizations.
# "DEFAULT": Automatically checks CPU hardware capabilities and selects the best Vector system.
# "OFF": Disables Vector API optimizations.
# "AVX2": Forces 256-bit vector operations (AVX2).
# "AVX512": Forces 512-bit vector operations (AVX512).
useVectorAPI = "DEFAULT"
```

---

## So what is C2ME?

C^2M-Engine, or C2ME for short, is a Fabric mod designed to improve the performance of chunk generation, I/O, and loading. This is done by taking advantage of multiple CPU cores in parallel. For the best performance it is recommended to use C2ME with [Lithium](https://github.com/CaffeineMC/lithium-fabric) and [Starlight](https://github.com/Spottedleaf/Starlight).

## What does C2ME stand for?

Concurrent chunk management engine, it's about making the game better threaded and more scalable in regard to world gen and chunk io performance.

## Vanilla parity

C2ME does not sacrifice vanilla functionality or behavior, or alter the vanilla world generation in the name of raw speed by default.
However, due to the [non-determinism of vanilla world generation](https://bugs.mojang.com/browse/MC-55596), worlds will vary
significantly run-to-run even with the same seed. This is not a bug on our side. 

While we carefully check that we do not modify any vanilla behavior, bugs are unavoidable after all. 
So, if you do encounter an issue where C2ME deviates from the intended vanilla behavior, don't hesitate to open an issue.

## Mod and Datapack compatibility

World generation datapacks that can run on vanilla Minecraft are fully supported.  
Custom world generators implemented in mods usually run well, but *may* cause compatibility issues due to certain
design assumptions used by mod authors being broken for further speedups of world generation.  
As a world generation mod author, if you find your mod broken, don't hesitate to look for help in our discord server (linked below).
We are willing to help mod authors embrace scalable world generation.  

### Undefined behavior sanitization

C2ME includes `CheckedThreadLocalRandom` for world random (included in [UWRAD](https://modrinth.com/mod/uwrad)) plus a few others.
These detections exist to prevent mods from screwing up Minecraft internals and causing undebuggable problems.  
The detection should almost **never** produce false positives, and should be taken seriously and reported
to corresponding mod authors instead.

## Usage notice

**Backup your worlds and practice good game modding skills.**

## Building and setting up

JDK 22+ is required to build this fork. (Note: LLVM/Clang compiler is no longer required).

Run the following commands in the root directory:

```shell
git submodule update --init --recursive
./gradlew clean build
```

## Upstream & Support

- Upstream Repository: [RelativityMC/C2ME-fabric](https://github.com/RelativityMC/C2ME-fabric)
- Issue Tracker: [GTYX06/C2ME-fabric Issues](https://github.com/GTYX06/C2ME-fabric/issues)
- Upstream Discord Server: [Discord Link](https://discord.gg/Kdy8NM5HW4)

## License

License information can be found [here](/licenses/LICENSE).
