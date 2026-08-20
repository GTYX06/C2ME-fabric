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

package com.ishland.c2me.opts.accel.cuda.common.shader_cache;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ShaderCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShaderCacheManager.class);

    private static Path getCacheDir() {
        try {
            return FabricLoader.getInstance().getGameDir().resolve(".c2me-cuda-cache");
        } catch (Throwable t) {
            return Path.of(".c2me-cuda-cache");
        }
    }

    public static byte[] getCached(String key) {
        try {
            String hash = hashKey(key);
            Path filePath = getCacheDir().resolve(hash + ".ptx.gz");
            if (Files.exists(filePath)) {
                try (InputStream in = new GZIPInputStream(Files.newInputStream(filePath))) {
                    byte[] bytes = in.readAllBytes();
                    LOGGER.info("Loaded cached CUDA PTX ({}, {} bytes)", hash, bytes.length);
                    return bytes;
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to read cached shader", t);
        }
        return null;
    }

    public static void putCached(String key, byte[] ptxBytes) {
        try {
            Path cacheDir = getCacheDir();
            Files.createDirectories(cacheDir);
            String hash = hashKey(key);
            Path filePath = cacheDir.resolve(hash + ".ptx.gz");
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(ptxBytes);
                gzip.finish();
                Files.write(filePath, baos.toByteArray());
                LOGGER.info("Cached compiled CUDA PTX ({}, {} bytes)", hash, ptxBytes.length);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to save shader to cache", t);
        }
    }

    private static String hashKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
