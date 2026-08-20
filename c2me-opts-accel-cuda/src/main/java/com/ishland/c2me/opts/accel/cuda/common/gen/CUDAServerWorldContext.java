/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 GTYX06
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.c2me.opts.accel.cuda.common.gen;

import com.ishland.c2me.opts.accel.cuda.common.bindings.CUDADriver;
import com.ishland.c2me.opts.accel.cuda.common.compiler.CUDACGen;
import com.ishland.c2me.opts.accel.cuda.common.compiler.GeneratedCUDASource;
import com.ishland.c2me.opts.accel.cuda.common.gen.cache.Stage1Cache;
import com.ishland.c2me.opts.accel.cuda.common.shader_cache.ShaderCacheManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class CUDAServerWorldContext implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CUDAServerWorldContext.class);

    private final CUDAServerGlobalContext globalContext;
    private final ServerWorld world;
    private final Stage1Cache stage1Cache = new Stage1Cache();

    private GeneratedCUDASource generatedSource;
    private MemorySegment cudaModule;
    private MemorySegment constDataDevPtr;

    private MemorySegment kernelBiomeMultiNoise;
    private MemorySegment kernelInterpolatorPrefill;
    private MemorySegment kernelAquiferPrefill;
    private MemorySegment kernelCache2dPrefill;
    private MemorySegment kernelNoise;
    private MemorySegment kernelEstimateSurfaceHeight;

    private volatile boolean initialized = false;

    public CUDAServerWorldContext(CUDAServerGlobalContext globalContext, ServerWorld world) {
        this.globalContext = globalContext;
        this.world = world;
    }

    public synchronized void initialize() {
        if (world.getChunkManager() != null) {
            initialize(world.getChunkManager().getChunkGenerator(), world.getChunkManager().getNoiseConfig());
        }
    }

    public synchronized void initialize(ChunkGenerator generator, NoiseConfig noiseConfig) {
        if (initialized || !globalContext.isAvailable()) return;

        try {
            if (generator == null || noiseConfig == null) {
                LOGGER.warn("Generator or noise config is null for world {}, skipping CUDA worldgen", world.getRegistryKey().getValue());
                return;
            }
            if (!(generator instanceof NoiseChunkGenerator noiseGenerator)) {
                LOGGER.info("ChunkGenerator for world {} is not NoiseChunkGenerator, skipping CUDA worldgen", world.getRegistryKey().getValue());
                return;
            }

            RegistryEntry<ChunkGeneratorSettings> settingsEntry = noiseGenerator.getSettings();
            ChunkGeneratorSettings settings = settingsEntry.value();

            MultiNoiseUtil.Entries<RegistryEntry<Biome>> biomeEntries = null;
            if (noiseGenerator.getBiomeSource() instanceof MultiNoiseBiomeSource multiNoise) {
                biomeEntries = ((com.ishland.c2me.base.mixin.access.IMultiNoiseBiomeSource) multiNoise).invokeGetBiomeEntries();
            } else {
                LOGGER.warn("BiomeSource for world {} is not MultiNoiseBiomeSource, skipping CUDA worldgen", world.getRegistryKey().getValue());
                return;
            }

            LOGGER.info("Compiling CUDA density function kernels for world {}...", world.getRegistryKey().getValue());
            this.generatedSource = CUDACGen.compile(noiseConfig, settings, noiseConfig.getMultiNoiseSampler(), biomeEntries);

            CUDADevice device = globalContext.getDevice();
            String cacheKey = generatedSource.source() + "#" + device.getMetadata().uuid();
            byte[] ptxBytes = ShaderCacheManager.getCached(cacheKey);
            if (ptxBytes == null) {
                ptxBytes = device.compileSource(generatedSource.source(), "c2me_worldgen_" + world.getRegistryKey().getValue().getPath() + ".cu", generatedSource.defines());
                ShaderCacheManager.putCached(cacheKey, ptxBytes);
            }

            this.cudaModule = device.loadModule("world_" + world.getRegistryKey().getValue(), ptxBytes);

            this.kernelBiomeMultiNoise = device.getFunction(cudaModule, "df_biome_multinoise_kernel");
            this.kernelAquiferPrefill = device.getFunction(cudaModule, "aquifer_data_prefill");
            this.kernelNoise = device.getFunction(cudaModule, "df_noise_kernel");
            this.kernelEstimateSurfaceHeight = device.getFunction(cudaModule, "chunkNoiseSampler_estimateSurfaceHeight_prefill_indep");

            try {
                this.kernelInterpolatorPrefill = device.getFunction(cudaModule, "df_interpolator_buffer_prefill_kernel");
            } catch (Throwable ignored) {
            }
            try {
                this.kernelCache2dPrefill = device.getFunction(cudaModule, "df_cache2d_prefill_kernel");
            } catch (Throwable ignored) {
            }

            byte[] constData = generatedSource.constData();
            if (constData.length > 0) {
                try (Arena arena = Arena.ofConfined()) {
                    MemorySegment pConstDev = arena.allocate(ValueLayout.ADDRESS);
                    int res = CUDADriver.cuMemAlloc(pConstDev, constData.length);
                    CUDADriver.checkCUDAError(res);
                    this.constDataDevPtr = pConstDev.get(ValueLayout.ADDRESS, 0);

                    MemorySegment hostConst = arena.allocate(constData.length);
                    MemorySegment.copy(MemorySegment.ofArray(constData), 0, hostConst, 0, constData.length);
                    res = CUDADriver.cuMemcpyHtoD(constDataDevPtr, hostConst, constData.length);
                    CUDADriver.checkCUDAError(res);
                }
            } else {
                this.constDataDevPtr = MemorySegment.NULL;
            }

            initialized = true;
            LOGGER.info("Successfully initialized CUDA worldgen for world {}", world.getRegistryKey().getValue());
        } catch (Throwable t) {
            LOGGER.error("Failed to initialize CUDA worldgen for world " + world.getRegistryKey().getValue(), t);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public CUDAServerGlobalContext getGlobalContext() {
        return globalContext;
    }

    public Stage1Cache getStage1Cache() {
        return stage1Cache;
    }

    public GeneratedCUDASource getGeneratedSource() {
        return generatedSource;
    }

    public MemorySegment getConstDataDevPtr() {
        return constDataDevPtr;
    }

    public MemorySegment getKernelBiomeMultiNoise() {
        return kernelBiomeMultiNoise;
    }

    public MemorySegment getKernelAquiferPrefill() {
        return kernelAquiferPrefill;
    }

    public MemorySegment getKernelNoise() {
        return kernelNoise;
    }

    public MemorySegment getKernelEstimateSurfaceHeight() {
        return kernelEstimateSurfaceHeight;
    }

    @Override
    public void close() {
        if (!initialized) return;
        initialized = false;
        if (constDataDevPtr != null && !constDataDevPtr.equals(MemorySegment.NULL)) {
            try {
                CUDADriver.cuMemFree(constDataDevPtr);
            } catch (Throwable ignored) {
            }
        }
        stage1Cache.clear();
    }
}
