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

package com.ishland.c2me;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.MixinService;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreLaunchHandler implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("C2ME PreLaunch");

    @Override
    public void onPreLaunch() {
        checkVectorModuleAndRelaunch();

        if (Boolean.getBoolean("com.ishland.c2me.mixin.doAudit")) {
            Logger auditLogger = LoggerFactory.getLogger("C2ME Mixin Audit");
            try {
                final Class<?> transformerClazz = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");
                if (transformerClazz.isInstance(MixinEnvironment.getCurrentEnvironment().getActiveTransformer())) {
                    final Field processorField = transformerClazz.getDeclaredField("processor");
                    processorField.setAccessible(true);
                    final Object processor = processorField.get(MixinEnvironment.getCurrentEnvironment().getActiveTransformer());
                    final Class<?> processorClazz = Class.forName("org.spongepowered.asm.mixin.transformer.MixinProcessor");
                    final Field configsField = processorClazz.getDeclaredField("configs");
                    configsField.setAccessible(true);
                    final List<?> configs = (List<?>) configsField.get(processor);
                    final Class<?> configClazz = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig");
                    final Method getUnhandledTargetsMethod = configClazz.getDeclaredMethod("getUnhandledTargets");
                    getUnhandledTargetsMethod.setAccessible(true);
                    Set<String> unhandled = new HashSet<>();
                    for (Object config : configs) {
                        final Set<String> unhandledTargets = (Set<String>) getUnhandledTargetsMethod.invoke(config);
                        unhandled.addAll(unhandledTargets);
                    }
                    for (String s : unhandled) {
                        auditLogger.info("Loading class {}", s);
                        MixinService.getService().getClassProvider().findClass(s, false);
                    }
                    for (Object config : configs) {
                        for (String unhandledTarget : (Set<String>) getUnhandledTargetsMethod.invoke(config)) {
                            auditLogger.error("{} is already classloaded", unhandledTarget);
                        }
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException("Failed to audit mixins", t);
            }
        }
    }

    private static void checkVectorModuleAndRelaunch() {
        if (ModuleLayer.boot().findModule("jdk.incubator.vector").isEmpty()) {
            if (Boolean.getBoolean("c2me.relaunched")) {
                LOGGER.error("jdk.incubator.vector module is still not present after relaunch. Please check your JVM arguments or Java installation.");
                return;
            }
            LOGGER.warn("jdk.incubator.vector module is not added to JVM modules. Relaunching Minecraft with --add-modules jdk.incubator.vector...");
            try {
                String javaBin = ProcessHandle.current().info().command().orElse(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
                List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
                List<String> command = new ArrayList<>();
                command.add(javaBin);

                boolean hasAddModules = false;
                for (int i = 0; i < jvmArgs.size(); i++) {
                    String arg = jvmArgs.get(i);
                    if (arg.startsWith("--add-modules=")) {
                        command.add(arg + ",jdk.incubator.vector");
                        hasAddModules = true;
                    } else if (arg.equals("--add-modules")) {
                        command.add(arg);
                        if (i + 1 < jvmArgs.size()) {
                            i++;
                            command.add(jvmArgs.get(i) + ",jdk.incubator.vector");
                        } else {
                            command.add("jdk.incubator.vector");
                        }
                        hasAddModules = true;
                    } else {
                        command.add(arg);
                    }
                }
                if (!hasAddModules) {
                    command.add("--add-modules");
                    command.add("jdk.incubator.vector");
                }
                command.add("-Dc2me.relaunched=true");

                String classpath = System.getProperty("java.class.path");
                if (classpath != null && !classpath.isEmpty()) {
                    command.add("-cp");
                    command.add(classpath);
                }

                String mainCommand = System.getProperty("sun.java.command");
                if (mainCommand != null && !mainCommand.isEmpty()) {
                    String[] parts = mainCommand.split("\\s+");
                    if (parts.length > 0) {
                        if (parts[0].endsWith(".jar")) {
                            command.add("-jar");
                        }
                        for (String part : parts) {
                            if (!part.isEmpty()) {
                                command.add(part);
                            }
                        }
                    }
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.inheritIO();
                pb.start();
                System.exit(0);
            } catch (Throwable t) {
                LOGGER.error("Failed to relaunch Minecraft with --add-modules jdk.incubator.vector", t);
            }
        } else {
            LOGGER.info("jdk.incubator.vector module is present in the JVM.");
        }
    }
}
