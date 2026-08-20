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

package com.ishland.c2me.opts.accel.cuda.common.bindings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NVRTC {

    private static final Logger LOGGER = LoggerFactory.getLogger(NVRTC.class);

    public static final int NVRTC_SUCCESS = 0;
    public static final boolean IS_AVAILABLE;

    private static MethodHandle nvrtcVersionHandle;
    private static MethodHandle nvrtcCreateProgramHandle;
    private static MethodHandle nvrtcCompileProgramHandle;
    private static MethodHandle nvrtcGetProgramLogSizeHandle;
    private static MethodHandle nvrtcGetProgramLogHandle;
    private static MethodHandle nvrtcGetPTXSizeHandle;
    private static MethodHandle nvrtcGetPTXHandle;
    private static MethodHandle nvrtcGetCUBINSizeHandle;
    private static MethodHandle nvrtcGetCUBINHandle;
    private static MethodHandle nvrtcDestroyProgramHandle;
    private static MethodHandle nvrtcGetErrorStringHandle;

    static {
        boolean available = false;
        try {
            Linker linker = Linker.nativeLinker();
            List<String> candidates = new ArrayList<>();
            candidates.add("libnvrtc.so.12");
            candidates.add("libnvrtc.so");
            candidates.add("libnvrtc.so.11.2");
            candidates.add("/usr/local/cuda/lib64/libnvrtc.so");
            candidates.add("/usr/lib/x86_64-linux-gnu/libnvrtc.so");
            candidates.add("/usr/lib/x86_64-linux-gnu/libnvrtc.so.12");
            candidates.add("/usr/lib/wsl/lib/libnvrtc.so");
            candidates.add("nvrtc64_120_0.dll");
            candidates.add("nvrtc64_112_0.dll");
            candidates.add("nvrtc.dll");

            String cudaPath = System.getenv("CUDA_PATH");
            if (cudaPath != null && !cudaPath.isEmpty()) {
                candidates.add(cudaPath + "/lib/x64/nvrtc.lib");
                candidates.add(cudaPath + "/bin/nvrtc64_120_0.dll");
                candidates.add(cudaPath + "/lib64/libnvrtc.so");
            }

            SymbolLookup lookup = null;
            for (String candidate : candidates) {
                try {
                    if (candidate.contains("/") || candidate.contains("\\")) {
                        lookup = SymbolLookup.libraryLookup(Path.of(candidate), Arena.global());
                    } else {
                        lookup = SymbolLookup.libraryLookup(candidate, Arena.global());
                    }
                    LOGGER.info("Successfully loaded NVRTC library: {}", candidate);
                    break;
                } catch (Throwable ignored) {
                }
            }

            if (lookup != null) {
                nvrtcVersionHandle = bind(linker, lookup, "nvrtcVersion", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                nvrtcCreateProgramHandle = bind(linker, lookup, "nvrtcCreateProgram", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                nvrtcCompileProgramHandle = bind(linker, lookup, "nvrtcCompileProgram", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                nvrtcGetProgramLogSizeHandle = bind(linker, lookup, "nvrtcGetProgramLogSize", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                nvrtcGetProgramLogHandle = bind(linker, lookup, "nvrtcGetProgramLog", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                nvrtcGetPTXSizeHandle = bind(linker, lookup, "nvrtcGetPTXSize", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                nvrtcGetPTXHandle = bind(linker, lookup, "nvrtcGetPTX", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                nvrtcGetCUBINSizeHandle = bindOptional(linker, lookup, "nvrtcGetCUBINSize", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                nvrtcGetCUBINHandle = bindOptional(linker, lookup, "nvrtcGetCUBIN", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                nvrtcDestroyProgramHandle = bind(linker, lookup, "nvrtcDestroyProgram", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                nvrtcGetErrorStringHandle = bind(linker, lookup, "nvrtcGetErrorString", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

                available = true;
            }
        } catch (Throwable t) {
            LOGGER.warn("NVRTC library not directly loaded (will attempt nvcc CLI fallback if needed)", t);
        }
        IS_AVAILABLE = available;
    }

    private static MethodHandle bind(Linker linker, SymbolLookup lookup, String name, FunctionDescriptor descriptor) {
        Optional<MemorySegment> symbol = lookup.find(name);
        if (symbol.isEmpty()) {
            throw new UnsatisfiedLinkError("NVRTC function not found: " + name);
        }
        return linker.downcallHandle(symbol.get(), descriptor);
    }

    private static MethodHandle bindOptional(Linker linker, SymbolLookup lookup, String name, FunctionDescriptor descriptor) {
        Optional<MemorySegment> symbol = lookup.find(name);
        return symbol.map(s -> linker.downcallHandle(s, descriptor)).orElse(null);
    }

    public static String getErrorString(int result) {
        if (nvrtcGetErrorStringHandle != null) {
            try {
                MemorySegment strSeg = (MemorySegment) nvrtcGetErrorStringHandle.invokeExact(result);
                if (!strSeg.equals(MemorySegment.NULL)) {
                    return strSeg.getString(0);
                }
            } catch (Throwable ignored) {
            }
        }
        return "NVRTC_RESULT_" + result;
    }

    public static byte[] compileToPTX(String source, String progName, String arch, Map<String, String> defines) {
        if (IS_AVAILABLE) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pProg = arena.allocate(ValueLayout.ADDRESS);
                MemorySegment srcSeg = arena.allocateFrom(source);
                MemorySegment nameSeg = arena.allocateFrom(progName != null ? progName : "c2me_kernel.cu");

                int res = (int) nvrtcCreateProgramHandle.invokeExact(pProg, srcSeg, nameSeg, 0, MemorySegment.NULL, MemorySegment.NULL);
                if (res != NVRTC_SUCCESS) {
                    throw new RuntimeException("nvrtcCreateProgram failed: " + getErrorString(res));
                }

                MemorySegment prog = pProg.get(ValueLayout.ADDRESS, 0);
                try {
                    List<String> options = new ArrayList<>();
                    options.add("--std=c++17");
                    options.add("-diag-suppress=191,177,68");
                    if (arch != null && !arch.isEmpty()) {
                        options.add("--gpu-architecture=" + arch);
                    }
                    if (defines != null) {
                        for (Map.Entry<String, String> entry : defines.entrySet()) {
                            options.add("-D" + entry.getKey() + "=" + entry.getValue());
                        }
                    }

                    MemorySegment optArray = arena.allocate(ValueLayout.ADDRESS, options.size());
                    for (int i = 0; i < options.size(); i++) {
                        MemorySegment optSeg = arena.allocateFrom(options.get(i));
                        optArray.setAtIndex(ValueLayout.ADDRESS, i, optSeg);
                    }

                    int compileRes = (int) nvrtcCompileProgramHandle.invokeExact(prog, options.size(), optArray);

                    MemorySegment logSizeRet = arena.allocate(ValueLayout.JAVA_LONG);
                    int logSizeRes = (int) nvrtcGetProgramLogSizeHandle.invokeExact(prog, logSizeRet);
                    long logSize = logSizeRet.get(ValueLayout.JAVA_LONG, 0);
                    String log = "";
                    if (logSize > 1) {
                        MemorySegment logSeg = arena.allocate(logSize);
                        int logRes = (int) nvrtcGetProgramLogHandle.invokeExact(prog, logSeg);
                        log = logSeg.getString(0);
                    }

                    if (compileRes != NVRTC_SUCCESS) {
                        LOGGER.error("NVRTC compilation failed with error {}: {}\nCompilation log:\n{}", compileRes, getErrorString(compileRes), log);
                        throw new RuntimeException("NVRTC compilation failed: " + getErrorString(compileRes) + "\n" + log);
                    }

                    if (!log.isBlank()) {
                        LOGGER.debug("NVRTC compilation log:\n{}", log);
                    }

                    MemorySegment ptxSizeRet = arena.allocate(ValueLayout.JAVA_LONG);
                    int ptxRes = (int) nvrtcGetPTXSizeHandle.invokeExact(prog, ptxSizeRet);
                    if (ptxRes != NVRTC_SUCCESS) {
                        throw new RuntimeException("nvrtcGetPTXSize failed: " + getErrorString(ptxRes));
                    }
                    long ptxSize = ptxSizeRet.get(ValueLayout.JAVA_LONG, 0);

                    MemorySegment ptxSeg = arena.allocate(ptxSize);
                    ptxRes = (int) nvrtcGetPTXHandle.invokeExact(prog, ptxSeg);
                    if (ptxRes != NVRTC_SUCCESS) {
                        throw new RuntimeException("nvrtcGetPTX failed: " + getErrorString(ptxRes));
                    }

                    byte[] ptxBytes = new byte[(int) ptxSize];
                    MemorySegment.copy(ptxSeg, ValueLayout.JAVA_BYTE, 0, ptxBytes, 0, (int) ptxSize);
                    return ptxBytes;
                } finally {
                    int destroyRes = (int) nvrtcDestroyProgramHandle.invokeExact(pProg);
                }
            } catch (Throwable t) {
                LOGGER.warn("NVRTC in-process compilation failed, trying nvcc CLI fallback...", t);
            }
        }

        return compileWithNvccCli(source, arch, defines);
    }

    private static byte[] compileWithNvccCli(String source, String arch, Map<String, String> defines) {
        try {
            Path tempCu = Files.createTempFile("c2me_cuda_", ".cu");
            Path tempPtx = Files.createTempFile("c2me_cuda_", ".ptx");
            try {
                Files.writeString(tempCu, source, StandardCharsets.UTF_8);
                List<String> cmd = new ArrayList<>();
                cmd.add("nvcc");
                cmd.add("--ptx");
                cmd.add("--std=c++17");
                if (arch != null && !arch.isEmpty()) {
                    cmd.add("-arch=" + arch);
                }
                if (defines != null) {
                    for (Map.Entry<String, String> entry : defines.entrySet()) {
                        cmd.add("-D" + entry.getKey() + "=" + entry.getValue());
                    }
                }
                cmd.add("-o");
                cmd.add(tempPtx.toAbsolutePath().toString());
                cmd.add(tempCu.toAbsolutePath().toString());

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("nvcc compilation failed (exit code " + exitCode + "):\n" + output);
                }
                return Files.readAllBytes(tempPtx);
            } finally {
                Files.deleteIfExists(tempCu);
                Files.deleteIfExists(tempPtx);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to compile CUDA kernel via NVRTC and nvcc fallback", t);
        }
    }
}
