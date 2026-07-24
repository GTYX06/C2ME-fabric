/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021-2026 ishland
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

package com.ishland.c2me.opts.math.common;

import com.ishland.c2me.base.common.config.ConfigSystem;

public class Config {

    public static final VectorMode vectorMode = new ConfigSystem.ConfigAccessor()
            .key("vanillaWorldGenOptimizations.useVectorAPI")
            .comment("""
                    Defines the Vector API mode to use for world generation math optimizations.
                    "default": Automatically checks CPU hardware capabilities and selects the best Vector system.
                    OFF: Disables Vector API optimizations.
                    AVX2: Forces 256-bit vector operations (AVX2).
                    AVX512: Forces 512-bit vector operations (AVX512).
                    
                    Please preserve quotes so this config doesn't break
                    """)
            .getEnum(VectorMode.class, VectorMode.DEFAULT, VectorMode.OFF);

    public static void init() {
        checkVectorModule();
    }

    public static void checkVectorModule() {
        if (vectorMode != VectorMode.OFF) {
            if (ModuleLayer.boot().findModule("jdk.incubator.vector").isEmpty()) {
                throwFormattedException(
                        "C2ME Vector API requires JVM incubator module jdk.incubator.vector",
                        """
                        C2ME's Vector API optimization is enabled (%s), but the required JVM module 'jdk.incubator.vector' is missing from the JVM arguments.
                        
                        To fix this issue:
                        1. Add '--add-modules jdk.incubator.vector' to your JVM launch arguments.
                        2. Alternatively, set 'vanillaWorldGenOptimizations.useVectorAPI = "OFF"' in c2me.toml to disable Vector API optimizations.
                        """.formatted(vectorMode)
                );
            }
        }
    }

    private static void throwFormattedException(String mainText, String treeText) {
        try {
            Class<?> clazz = Class.forName("net.fabricmc.loader.impl.FormattedException");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(String.class, String.class);
            Object exc = ctor.newInstance(mainText, treeText);
            sneakyThrow((Throwable) exc);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(mainText + "\n\n" + treeText);
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            if (e.getCause() instanceof RuntimeException re) throw re;
            sneakyThrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    public enum VectorMode {
        DEFAULT,
        OFF,
        AVX2,
        AVX512
    }

}
