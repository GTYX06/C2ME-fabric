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

package com.ishland.c2me.opts.accel.vulkan.common.gen.cache;

import com.ishland.c2me.opts.accel.vulkan.common.gen.VulkanServerWorldContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class Stage1Cache {

    private static final Logger LOGGER = LoggerFactory.getLogger(Stage1Cache.class);
    private final VulkanServerWorldContext worldContext;

    public Stage1Cache(VulkanServerWorldContext worldContext) {
        this.worldContext = worldContext;
    }

    public CompletableFuture<AreaCacheEntry> getAreaCache(int startChunkX, int startChunkZ, int chunkCountX, int chunkCountZ) {
        return CompletableFuture.completedFuture(new AreaCacheEntry(startChunkX, startChunkZ, chunkCountX, chunkCountZ, new int[0], new double[0]));
    }

    public CompletableFuture<AreaCacheEntry> getChunkCache(int chunkX, int chunkZ) {
        return getAreaCache(chunkX, chunkZ, 1, 1);
    }

    public record AreaCacheEntry(int chunkX, int chunkZ, int sizeX, int sizeZ, int[] surfaceHeights, double[] flatCaches) {
    }
}
