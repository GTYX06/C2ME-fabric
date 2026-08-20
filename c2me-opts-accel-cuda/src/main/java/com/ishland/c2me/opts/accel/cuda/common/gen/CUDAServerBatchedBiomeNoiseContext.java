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
import com.ishland.c2me.opts.accel.cuda.common.compiler.GeneratedCUDASource;
import com.ishland.c2me.opts.accel.cuda.common.ducks.PalettedContainerExtension;
import com.ishland.c2me.opts.accel.cuda.common.gen.cache.CUDABufferCache;
import com.ishland.c2me.opts.accel.cuda.common.util.CUDAStructs;
import com.ishland.c2me.opts.accel.cuda.common.util.TLUtil;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.ReadableContainer;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CUDAServerBatchedBiomeNoiseContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(CUDAServerBatchedBiomeNoiseContext.class);

    private final CUDAServerWorldContext worldContext;
    private final ChunkGeneratorSettings settings;

    public CUDAServerBatchedBiomeNoiseContext(CUDAServerWorldContext worldContext, ChunkGeneratorSettings settings) {
        this.worldContext = worldContext;
        this.settings = settings;
    }

    public CompletableFuture<Void> executeBatch(List<Chunk> chunks, AquiferSampler.FluidLevelSampler fluidLevelSampler) {
        if (!worldContext.isInitialized() || chunks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CUDADevice device = worldContext.getGlobalContext().getDevice();
        CUDABufferCache bufferCache = device.getBufferCache();
        MemorySegment stream = device.acquireStream();

        GenerationShapeConfig shape = settings.generationShapeConfig();
        int height = shape.height();
        int minY = shape.minimumY();
        int biomeHeight = height >> 2;

        int biomeCountPerChunk = 4 * 4 * biomeHeight;
        int blockCountPerChunk = 16 * 16 * height;

        int totalBiomeBytes = chunks.size() * biomeCountPerChunk * 4;
        int totalBlockBytes = chunks.size() * blockCountPerChunk;

        MemorySegment dBiomes = bufferCache.acquireBuffer(totalBiomeBytes);
        MemorySegment dBlocks = bufferCache.acquireBuffer(totalBlockBytes);
        MemorySegment dRwData = bufferCache.acquireBuffer(chunks.size() * 65536L);

        CompletableFuture<Void> future = new CompletableFuture<>();

        CUDAExecutors.CUDA_COMPLETION_EXECUTOR.execute(() -> {
            try (Arena hostArena = Arena.ofConfined()) {
                device.bindToCurrentThread();

                MemorySegment hostBiomesSeg = hostArena.allocate(totalBiomeBytes);
                MemorySegment hostBlocksSeg = hostArena.allocate(totalBlockBytes);

                for (int i = 0; i < chunks.size(); i++) {
                    Chunk chunk = chunks.get(i);
                    ChunkPos pos = chunk.getPos();
                    int chunkX = pos.x();
                    int chunkZ = pos.z();

                    long rwChunkOffset = (long) i * 65536L;
                    MemorySegment chunkRwDev = dRwData.asSlice(rwChunkOffset, 65536L);

                    ByteBuffer paramsBuf = TLUtil.getDirectBuffer(CUDAStructs.SIZEOF_WORLDGEN_PARAMS + 4096);
                    CUDAStructs.writeWorldgenParams(
                            paramsBuf,
                            chunkX << 2, chunkZ << 2, 4, 4,
                            chunkX << 2, minY >> 3, chunkZ << 2, 4, height >> 3, 4,
                            chunkX << 2, chunkZ << 2, 4, 4,
                            chunkX << 4, chunkZ << 4, 16, 16,
                            0, CUDAStructs.BLOCK_DEFAULT_BLOCK, CUDAStructs.BLOCK_WATER,
                            0, 0, 0
                    );
                    paramsBuf.flip();

                    MemorySegment hostParamsSeg = MemorySegment.ofBuffer(paramsBuf);
                    CUDADriver.cuMemcpyHtoDAsync(chunkRwDev, hostParamsSeg, paramsBuf.remaining(), stream);

                    long biomeOffset = (long) i * biomeCountPerChunk * 4L;
                    long blockOffset = (long) i * blockCountPerChunk;
                    MemorySegment chunkBiomeDev = dBiomes.asSlice(biomeOffset, (long) biomeCountPerChunk * 4L);
                    MemorySegment chunkBlockDev = dBlocks.asSlice(blockOffset, (long) blockCountPerChunk);

                    try (Arena arena = Arena.ofConfined()) {
                        MemorySegment kParamsBiome = arena.allocate(ValueLayout.ADDRESS, 9);
                        kParamsBiome.setAtIndex(ValueLayout.ADDRESS, 0, ptr(arena, worldContext.getConstDataDevPtr()));
                        kParamsBiome.setAtIndex(ValueLayout.ADDRESS, 1, ptr(arena, chunkRwDev));
                        kParamsBiome.setAtIndex(ValueLayout.ADDRESS, 2, ptr(arena, chunkBiomeDev));
                        kParamsBiome.setAtIndex(ValueLayout.ADDRESS, 3, intVal(arena, chunkX << 2));
                        kParamsBiome.setAtIndex(ValueLayout.ADDRESS, 4, intVal(arena, chunkZ << 2));
                        kParamsBiome.setAtIndex(ValueLayout.ADDRESS, 5, intVal(arena, minY >> 2));
                        kParamsBiome.setAtIndex(ValueLayout.ADDRESS, 6, intVal(arena, 4));
                        kParamsBiome.setAtIndex(ValueLayout.ADDRESS, 7, intVal(arena, 4));
                        kParamsBiome.setAtIndex(ValueLayout.ADDRESS, 8, intVal(arena, biomeHeight));

                        CUDADriver.cuLaunchKernel(
                                worldContext.getKernelBiomeMultiNoise(),
                                1, 1, (biomeHeight + 3) / 4,
                                4, 4, 4,
                                0, stream,
                                kParamsBiome, MemorySegment.NULL
                        );

                        MemorySegment kParamsNoise = arena.allocate(ValueLayout.ADDRESS, 8);
                        kParamsNoise.setAtIndex(ValueLayout.ADDRESS, 0, ptr(arena, worldContext.getConstDataDevPtr()));
                        kParamsNoise.setAtIndex(ValueLayout.ADDRESS, 1, ptr(arena, chunkRwDev));
                        kParamsNoise.setAtIndex(ValueLayout.ADDRESS, 2, ptr(arena, chunkBlockDev));
                        kParamsNoise.setAtIndex(ValueLayout.ADDRESS, 3, intVal(arena, chunkX));
                        kParamsNoise.setAtIndex(ValueLayout.ADDRESS, 4, intVal(arena, chunkZ));
                        kParamsNoise.setAtIndex(ValueLayout.ADDRESS, 5, intVal(arena, 16));
                        kParamsNoise.setAtIndex(ValueLayout.ADDRESS, 6, intVal(arena, 16));
                        kParamsNoise.setAtIndex(ValueLayout.ADDRESS, 7, intVal(arena, height));

                        CUDADriver.cuLaunchKernel(
                                worldContext.getKernelNoise(),
                                1, 1, (height + 7) / 8,
                                16, 16, 8,
                                0, stream,
                                kParamsNoise, MemorySegment.NULL
                        );
                    }
                }

                CUDADriver.cuMemcpyDtoHAsync(hostBiomesSeg, dBiomes, totalBiomeBytes, stream);
                CUDADriver.cuMemcpyDtoHAsync(hostBlocksSeg, dBlocks, totalBlockBytes, stream);

                CUDADriver.cuStreamSynchronize(stream);

                GeneratedCUDASource genSource = worldContext.getGeneratedSource();
                RegistryEntry<Biome>[] biomes = genSource.biomeRegistryEntries();
                BlockState[] blockStates = genSource.blockStateRegistryEntries();

                for (int i = 0; i < chunks.size(); i++) {
                    Chunk chunk = chunks.get(i);
                    writeBiomesToChunk(chunk, hostBiomesSeg, i * biomeCountPerChunk, biomes, shape);
                    writeBlocksToChunk(chunk, hostBlocksSeg, i * blockCountPerChunk, blockStates, shape);
                }

                future.complete(null);
            } catch (Throwable t) {
                LOGGER.error("Error executing CUDA worldgen batch", t);
                future.completeExceptionally(t);
            } finally {
                bufferCache.releaseBuffer(dBiomes, totalBiomeBytes);
                bufferCache.releaseBuffer(dBlocks, totalBlockBytes);
                bufferCache.releaseBuffer(dRwData, chunks.size() * 65536L);
                device.releaseStream(stream);
            }
        });

        return future;
    }

    private void writeBiomesToChunk(Chunk chunk, MemorySegment biomesSeg, int startIdx, RegistryEntry<Biome>[] biomeRegistry, GenerationShapeConfig shape) {
        int minY = shape.minimumY();
        int height = shape.height();
        int biomeHeight = height >> 2;

        ChunkSection[] sections = chunk.getSectionArray();
        for (int sectionIdx = 0; sectionIdx < sections.length; sectionIdx++) {
            ChunkSection section = sections[sectionIdx];
            if (section == null) continue;
            ReadableContainer<RegistryEntry<Biome>> readableContainer = section.getBiomeContainer();
            if (!(readableContainer instanceof PalettedContainer<RegistryEntry<Biome>> biomeContainer)) continue;
            int sectionMinY = chunk.sectionIndexToCoord(sectionIdx) << 4;
            int relBiomeY = (sectionMinY - minY) >> 2;
            if (relBiomeY < 0 || relBiomeY + 4 > biomeHeight) continue;

            for (int y = 0; y < 4; y++) {
                int by = relBiomeY + y;
                for (int z = 0; z < 4; z++) {
                    for (int x = 0; x < 4; x++) {
                        long idx = (long) startIdx + (by * 4 + z) * 4 + x;
                        int biomeId = biomesSeg.get(ValueLayout.JAVA_INT_UNALIGNED, idx * 4);
                        if (biomeId >= 0 && biomeId < biomeRegistry.length) {
                            biomeContainer.set(x, y, z, biomeRegistry[biomeId]);
                        }
                    }
                }
            }
        }
    }

    private void writeBlocksToChunk(Chunk chunk, MemorySegment blocksSeg, int startIdx, BlockState[] blockStates, GenerationShapeConfig shape) {
        int minY = shape.minimumY();
        int height = shape.height();

        ChunkSection[] sections = chunk.getSectionArray();
        for (int sectionIdx = 0; sectionIdx < sections.length; sectionIdx++) {
            ChunkSection section = sections[sectionIdx];
            if (section == null) continue;
            int sectionMinY = chunk.sectionIndexToCoord(sectionIdx) << 4;
            int relY = sectionMinY - minY;
            if (relY < 0 || relY + 16 > height) continue;

            for (int y = 0; y < 16; y++) {
                int worldRelY = relY + y;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        long idx = (long) startIdx + (worldRelY * 16 + z) * 16 + x;
                        int raw = blocksSeg.get(ValueLayout.JAVA_BYTE, idx) & 0xFF;
                        int blockId = raw & 0x7F;
                        if (blockId > 0 && blockId < blockStates.length) {
                            BlockState state = blockStates[blockId];
                            if (state != null) {
                                section.setBlockState(x, y, z, state, false);
                                chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG).trackUpdate(x, sectionMinY + y, z, state);
                                chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG).trackUpdate(x, sectionMinY + y, z, state);
                            }
                        }
                    }
                }
            }
        }
    }

    private static MemorySegment ptr(Arena arena, MemorySegment val) {
        MemorySegment seg = arena.allocate(ValueLayout.ADDRESS);
        seg.set(ValueLayout.ADDRESS, 0, val);
        return seg;
    }

    private static MemorySegment intVal(Arena arena, int val) {
        MemorySegment seg = arena.allocate(ValueLayout.JAVA_INT);
        seg.set(ValueLayout.JAVA_INT, 0, val);
        return seg;
    }
}
