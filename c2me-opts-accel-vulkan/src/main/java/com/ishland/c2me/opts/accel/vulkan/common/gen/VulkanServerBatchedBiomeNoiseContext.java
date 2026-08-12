/*
 * All Rights Reserved
 *
 * Copyright (c) 2025-2026 ishland
 *
 * All rights reserved. Do not redistribute.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.c2me.opts.accel.vulkan.common.gen;

import com.ishland.c2me.opts.accel.vulkan.common.Config;
import com.ishland.c2me.rewrites.chunksystem.common.ChunkLoadingContext;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class VulkanServerBatchedBiomeNoiseContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanServerBatchedBiomeNoiseContext.class);

    public static final int BATCH_SIZE = Config.useSmallerBatches ? 2 : 4;
    public static final int BATCH_MASK = BATCH_SIZE - 1;
    public static final int BATCH_SHIFT = Integer.bitCount(BATCH_MASK);

    public static boolean isAligned(int x, int z) {
        return (x & BATCH_MASK) == 0 && (z & BATCH_MASK) == 0;
    }

    private final ChunkPos startingPos;
    private final VulkanServerWorldContext worldContext;
    private final NoiseChunkGenerator generator;
    private final NoiseConfig noiseConfig;

    public VulkanServerBatchedBiomeNoiseContext(ChunkPos startingPos, VulkanServerWorldContext worldContext, NoiseChunkGenerator generator, NoiseConfig noiseConfig) {
        this.startingPos = Objects.requireNonNull(startingPos);
        this.worldContext = Objects.requireNonNull(worldContext);
        this.generator = Objects.requireNonNull(generator);
        this.noiseConfig = Objects.requireNonNull(noiseConfig);
    }

    public CompletableFuture<Void> execute(ChunkLoadingContext context, BoundedRegionArray<ProtoChunk> chunks, BoundedRegionArray<StructureAccessor> structureAccessors) {
        return CompletableFuture.completedFuture(null);
    }
}
