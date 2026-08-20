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

package com.ishland.c2me.opts.accel.cuda.common;

import com.ishland.c2me.base.common.config.ConfigSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.UUID;

public class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    public static final int maxConcurrentTasksPerDevice = (int) new ConfigSystem.ConfigAccessor()
            .key("cudaAccel.maxConcurrentTasksPerDevice")
            .comment("""
                    Maximum number of concurrent tasks per CUDA device
                    Increasing this may increase performance and will increase VRAM usage
                    """)
            .getLong(32, 32, ConfigSystem.LongChecks.THREAD_COUNT);

    public static final boolean lowPriorityStreams = new ConfigSystem.ConfigAccessor()
            .key("cudaAccel.lowPriorityStreams")
            .comment("""
                    Whether to create CUDA streams with lower priority
                    This may reduce FPS drops during heavy world generation
                    """)
            .getBoolean(true, true);

    public static final boolean allowIncompatibilityFallback = new ConfigSystem.ConfigAccessor()
            .key("cudaAccel.allowIncompatibilityFallback")
            .comment("""
                    Whether to allow falling back to CPU world generation if CUDA initialization fails
                    """)
            .getBoolean(false, false);

    public static final boolean preserveAllControlFlows = new ConfigSystem.ConfigAccessor()
            .key("cudaAccel.preserveAllControlFlows")
            .comment("""
                    Preserve control flows in generated CUDA code
                    """)
            .getBoolean(true, false);

    public static final boolean useSmallerBatches = new ConfigSystem.ConfigAccessor()
            .key("cudaAccel.useSmallerBatches")
            .comment("""
                    Whether to use smaller batches in world generation
                    """)
            .getBoolean(false, false);

    public static final boolean doExplicitFlushes = new ConfigSystem.ConfigAccessor()
            .key("cudaAccel.doExplicitFlushes")
            .comment("""
                    Whether to perform explicit stream synchronizations after every batch
                    """)
            .getBoolean(false, false);

    public static final String deviceUUIDBlacklistRaw = new ConfigSystem.ConfigAccessor()
            .key("cudaAccel.deviceUUIDBlacklist")
            .comment("""
                    A comma-separated list of device UUIDs to blacklist
                    Example: "951e5ce5-ccec-4a37-9ece-d0a800662d8f"
                    """)
            .getString("", "");

    public static final String deviceUUIDWhitelistRaw = new ConfigSystem.ConfigAccessor()
            .key("cudaAccel.deviceUUIDWhitelist")
            .comment("""
                    A comma-separated list of device UUIDs to whitelist
                    If non-empty, only devices in this list will be used
                    """)
            .getString("", "");

    public static final HashSet<UUID> deviceUUIDBlacklist = new HashSet<>();
    public static final HashSet<UUID> deviceUUIDWhitelist = new HashSet<>();

    static {
        for (String s : deviceUUIDBlacklistRaw.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                try {
                    deviceUUIDBlacklist.add(UUID.fromString(trimmed));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in cudaAccel.deviceUUIDBlacklist: {}", trimmed);
                }
            }
        }
        for (String s : deviceUUIDWhitelistRaw.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                try {
                    deviceUUIDWhitelist.add(UUID.fromString(trimmed));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in cudaAccel.deviceUUIDWhitelist: {}", trimmed);
                }
            }
        }
    }

    public static void init() {
        tryChunkyMaxWorkingCount();
    }

    private static void tryChunkyMaxWorkingCount() {
        String chunkyMaxWorkingCount = System.getProperty("chunky.maxWorkingCount", "");
        int value;
        try {
            value = Integer.parseInt(chunkyMaxWorkingCount);
        } catch (NumberFormatException e) {
            value = 0;
        }
        if (value == 0) {
            if (Runtime.getRuntime().maxMemory() > 10L * 1024L * 1024L * 1024L) {
                value = 512;
            } else {
                value = 192;
            }
            System.setProperty("chunky.maxWorkingCount", Integer.toString(value));
        }
        LOGGER.info("chunky.maxWorkingCount: {}", value);
    }

}
