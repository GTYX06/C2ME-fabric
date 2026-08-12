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

import com.ishland.c2me.opts.accel.vulkan.common.compiler.GeneratedVulkanSource;
import com.ishland.c2me.opts.accel.vulkan.common.compiler.emitters.misc.VKBlockStateMappings;
import com.ishland.c2me.opts.accel.vulkan.common.gen.cache.Stage1Cache;
import com.ishland.c2me.opts.accel.vulkan.common.shader_cache.ShaderCacheManager;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class VulkanServerWorldContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanServerWorldContext.class);

    private final ArrayList<DeviceWithProgram> openDevices = new ArrayList<>();
    private final ReferenceOpenHashSet<Pair<VulkanDevice, CompletableFuture<Void>>> pendingCompilations = new ReferenceOpenHashSet<>();

    private final Stage1Cache stage1Cache;

    private final VulkanServerGlobalContext globalContext;
    private final String description;
    private final GeneratedVulkanSource generatedCLSource;
    private final GenerationShapeConfig generationShapeConfig;
    private final VKBlockStateMappings clBlockStateMappings;

    public VulkanServerWorldContext(VulkanServerGlobalContext globalContext, String description, GeneratedVulkanSource generatedCLSource, GenerationShapeConfig generationShapeConfig, VKBlockStateMappings clBlockStateMappings) {
        this.globalContext = globalContext;
        this.description = description;
        this.generatedCLSource = Objects.requireNonNull(generatedCLSource);
        this.generationShapeConfig = Objects.requireNonNull(generationShapeConfig);
        this.clBlockStateMappings = Objects.requireNonNull(clBlockStateMappings);
        this.stage1Cache = new Stage1Cache(this);

        this.globalContext.registerWorld(this);
    }

    public void addDevice(VulkanDevice device) {
        synchronized (this.pendingCompilations) {
            for (Pair<VulkanDevice, CompletableFuture<Void>> pendingCompilation : this.pendingCompilations) {
                if (pendingCompilation.left() == device) {
                    return;
                }
            }
            LOGGER.info("Compiling program for {} for device {}", this.description, device);
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    ByteBuffer spirv = ShaderCacheManager.compileGlslToSpirv(this.generatedCLSource.getGeneratedSource(), this.description);
                    synchronized (this.openDevices) {
                        this.openDevices.add(new DeviceWithProgram(device, spirv, 0L));
                    }
                    LOGGER.info("Compiled program for {} for device {}", this.description, device);
                } catch (Throwable t) {
                    LOGGER.error("Failed to compile program for device {}", device, t);
                }
            }, device.getExecutor());

            Pair<VulkanDevice, CompletableFuture<Void>> pair = Pair.of(device, future);
            this.pendingCompilations.add(pair);
        }
    }

    public void removeDevice(VulkanDevice device) {
        synchronized (this.pendingCompilations) {
            this.pendingCompilations.removeIf(pair -> pair.left() == device);
        }
        synchronized (this.openDevices) {
            this.openDevices.removeIf(deviceWithProgram -> deviceWithProgram.device == device);
        }
    }

    public Stage1Cache getStage1Cache() {
        return stage1Cache;
    }

    public Stage1Cache getEstimateSurfaceHeightCache() {
        return stage1Cache;
    }

    public GeneratedVulkanSource getGeneratedCLSource() {
        return generatedCLSource;
    }

    public GeneratedVulkanSource getGeneratedVulkanSource() {
        return generatedCLSource;
    }

    public GenerationShapeConfig getGenerationShapeConfig() {
        return generationShapeConfig;
    }

    public void releaseAllDevices() {
        synchronized (this.pendingCompilations) {
            this.pendingCompilations.clear();
        }
        synchronized (this.openDevices) {
            this.openDevices.clear();
        }
    }

    public VKBlockStateMappings getClBlockStateMappings() {
        return clBlockStateMappings;
    }

    public record DeviceWithProgram(VulkanDevice device, ByteBuffer spirv, long constBuffer) {
    }
}
