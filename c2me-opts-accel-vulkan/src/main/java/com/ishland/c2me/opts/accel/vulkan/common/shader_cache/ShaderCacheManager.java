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

package com.ishland.c2me.opts.accel.vulkan.common.shader_cache;

import com.google.common.base.Stopwatch;
import com.ishland.c2me.opts.accel.vulkan.common.zstd.ZstdInputStreamNoFinalizer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public class ShaderCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShaderCacheManager.class);

    private final Map<String, Path> cacheIndex;

    public ShaderCacheManager() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            this.cacheIndex = Collections.unmodifiableMap(scan());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stopwatch.stop();
        LOGGER.info("Indexed {} shader cache entries in {}", this.cacheIndex.size(), stopwatch);
    }

    private static Map<String, Path> scan() throws IOException {
        Path baseDir = Path.of(".", "config", "c2me-shader-delivery");

        if (!Files.isDirectory(baseDir)) {
            return new Object2ObjectOpenHashMap<>();
        }

        Object2ObjectOpenHashMap<String, Path> index = new Object2ObjectOpenHashMap<>();

        for (Path path : Files.list(baseDir).sorted().toList()) {
            path = path.normalize();
            if (path.getFileName().toString().endsWith(".tar.zst") && Files.isRegularFile(path)) {
                Stopwatch stopwatch = Stopwatch.createStarted();
                try (var in = new TarArchiveInputStream(new BufferedInputStream(new ZstdInputStreamNoFinalizer(Files.newInputStream(path)), 1048576))) {
                    TarArchiveEntry entry;
                    while ((entry = in.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (name.endsWith("/") || !entry.isFile()) {
                            continue;
                        }
                        if (index.containsKey(name)) {
                            throw new IllegalStateException(String.format("Duplicate entry in (%s) and (%s): (%s)", path, index.get(name), name));
                        }
                        index.put(name, path);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to read {}", path, e);
                }
                stopwatch.stop();
                LOGGER.info("Read {} fully in {}", path, stopwatch);
            }
        }

        return index;
    }

    public byte[] tryCache(String path) {
        Path archivePath = this.cacheIndex.get(path);
        if (archivePath == null) return null;

        try (var in = new TarArchiveInputStream(new BufferedInputStream(new ZstdInputStreamNoFinalizer(Files.newInputStream(archivePath)), 1048576))) {
            TarArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith("/") || !entry.isFile() || !name.equals(path)) {
                    continue;
                }
                if (!in.canReadEntryData(entry)) {
                    LOGGER.error("Unable to read {} from archive {}, something is wrong", path, archivePath);
                    continue;
                }
                return in.readAllBytes();
            }
            return null;
        } catch (IOException e) {
            LOGGER.error("Failed to read {}", archivePath, e);
            return null;
        }
    }

    public static ByteBuffer compileGlslToSpirv(String source, String fileName) {
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        Shaderc.shaderc_compile_options_set_target_env(options, Shaderc.shaderc_target_env_vulkan, Shaderc.shaderc_env_version_vulkan_1_2);
        Shaderc.shaderc_compile_options_set_optimization_level(options, Shaderc.shaderc_optimization_level_performance);

        ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source);
        long result;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            result = Shaderc.shaderc_compile_into_spv(
                    compiler,
                    sourceBuffer,
                    Shaderc.shaderc_compute_shader,
                    stack.UTF8(fileName),
                    stack.UTF8("main"),
                    options
            );
        } finally {
            MemoryUtil.memFree(sourceBuffer);
        }

        if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
            String errorMsg = Shaderc.shaderc_result_get_error_message(result);
            Shaderc.shaderc_result_release(result);
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
            throw new RuntimeException("GLSL to SPIR-V compilation failed for " + fileName + ": " + errorMsg);
        }

        ByteBuffer bytes = Shaderc.shaderc_result_get_bytes(result);
        ByteBuffer copy = MemoryUtil.memAlloc(bytes.remaining());
        copy.put(bytes);
        copy.flip();

        Shaderc.shaderc_result_release(result);
        Shaderc.shaderc_compile_options_release(options);
        Shaderc.shaderc_compiler_release(compiler);

        return copy;
    }
}
