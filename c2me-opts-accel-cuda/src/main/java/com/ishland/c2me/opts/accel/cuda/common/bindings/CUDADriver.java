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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.Optional;

public class CUDADriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CUDADriver.class);

    public static final int CUDA_SUCCESS = 0;

    public static final int CU_STREAM_DEFAULT = 0;
    public static final int CU_STREAM_NON_BLOCKING = 1;

    public static final int CU_EVENT_DEFAULT = 0;
    public static final int CU_EVENT_BLOCKING_SYNC = 1;
    public static final int CU_EVENT_DISABLE_TIMING = 2;

    public static final int CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_BLOCK = 1;
    public static final int CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_X = 2;
    public static final int CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Y = 3;
    public static final int CU_DEVICE_ATTRIBUTE_MAX_BLOCK_DIM_Z = 4;
    public static final int CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_X = 5;
    public static final int CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Y = 6;
    public static final int CU_DEVICE_ATTRIBUTE_MAX_GRID_DIM_Z = 7;
    public static final int CU_DEVICE_ATTRIBUTE_TOTAL_CONSTANT_MEMORY = 9;
    public static final int CU_DEVICE_ATTRIBUTE_WARP_SIZE = 10;
    public static final int CU_DEVICE_ATTRIBUTE_MULTIPROCESSOR_COUNT = 16;
    public static final int CU_DEVICE_ATTRIBUTE_ASYNC_ENGINE_COUNT = 40;
    public static final int CU_DEVICE_ATTRIBUTE_UNIFIED_ADDRESSING = 41;
    public static final int CU_DEVICE_ATTRIBUTE_COMPUTE_CAPABILITY_MAJOR = 75;
    public static final int CU_DEVICE_ATTRIBUTE_COMPUTE_CAPABILITY_MINOR = 76;
    public static final int CU_DEVICE_ATTRIBUTE_MEMORY_POOLS_SUPPORTED = 115;

    public static final boolean IS_AVAILABLE;

    private static MethodHandle cuInitHandle;
    private static MethodHandle cuDriverGetVersionHandle;
    private static MethodHandle cuDeviceGetCountHandle;
    private static MethodHandle cuDeviceGetHandle;
    private static MethodHandle cuDeviceGetNameHandle;
    private static MethodHandle cuDeviceGetUuidHandle;
    private static MethodHandle cuDeviceGetAttributeHandle;
    private static MethodHandle cuDeviceTotalMemHandle;
    private static MethodHandle cuCtxCreateHandle;
    private static MethodHandle cuCtxDestroyHandle;
    private static MethodHandle cuCtxSetCurrentHandle;
    private static MethodHandle cuCtxPushCurrentHandle;
    private static MethodHandle cuCtxPopCurrentHandle;
    private static MethodHandle cuDevicePrimaryCtxRetainHandle;
    private static MethodHandle cuDevicePrimaryCtxReleaseHandle;
    private static MethodHandle cuStreamCreateHandle;
    private static MethodHandle cuStreamCreateWithPriorityHandle;
    private static MethodHandle cuStreamDestroyHandle;
    private static MethodHandle cuStreamSynchronizeHandle;
    private static MethodHandle cuStreamQueryHandle;
    private static MethodHandle cuEventCreateHandle;
    private static MethodHandle cuEventDestroyHandle;
    private static MethodHandle cuEventRecordHandle;
    private static MethodHandle cuEventSynchronizeHandle;
    private static MethodHandle cuEventQueryHandle;
    private static MethodHandle cuStreamWaitEventHandle;
    private static MethodHandle cuMemAllocHandle;
    private static MethodHandle cuMemAllocAsyncHandle;
    private static MethodHandle cuMemFreeHandle;
    private static MethodHandle cuMemFreeAsyncHandle;
    private static MethodHandle cuMemcpyHtoDHandle;
    private static MethodHandle cuMemcpyHtoDAsyncHandle;
    private static MethodHandle cuMemcpyDtoHHandle;
    private static MethodHandle cuMemcpyDtoHAsyncHandle;
    private static MethodHandle cuModuleLoadDataHandle;
    private static MethodHandle cuModuleUnloadHandle;
    private static MethodHandle cuModuleGetFunctionHandle;
    private static MethodHandle cuLaunchKernelHandle;
    private static MethodHandle cuGetErrorStringHandle;
    private static MethodHandle cuGetErrorNameHandle;

    static {
        boolean available = false;
        try {
            Linker linker = Linker.nativeLinker();
            String[] candidates = {
                    "libcuda.so.1",
                    "libcuda.so",
                    "/usr/lib/wsl/lib/libcuda.so.1",
                    "/usr/lib/x86_64-linux-gnu/libcuda.so.1",
                    "/usr/lib64/libcuda.so.1",
                    "nvcuda.dll",
                    "cuda"
            };
            SymbolLookup lookup = null;
            for (String candidate : candidates) {
                try {
                    if (candidate.contains("/")) {
                        lookup = SymbolLookup.libraryLookup(Path.of(candidate), Arena.global());
                    } else {
                        lookup = SymbolLookup.libraryLookup(candidate, Arena.global());
                    }
                    LOGGER.info("Successfully loaded CUDA driver library: {}", candidate);
                    break;
                } catch (Throwable ignored) {
                }
            }

            if (lookup != null) {
                cuInitHandle = bind(linker, lookup, "cuInit", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
                cuDriverGetVersionHandle = bind(linker, lookup, "cuDriverGetVersion", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                cuDeviceGetCountHandle = bind(linker, lookup, "cuDeviceGetCount", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                cuDeviceGetHandle = bind(linker, lookup, "cuDeviceGet", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
                cuDeviceGetNameHandle = bind(linker, lookup, "cuDeviceGetName", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
                cuDeviceGetUuidHandle = bindOptional(linker, lookup, "cuDeviceGetUuid", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
                cuDeviceGetAttributeHandle = bind(linker, lookup, "cuDeviceGetAttribute", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
                cuDeviceTotalMemHandle = bind(linker, lookup, "cuDeviceTotalMem_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
                if (cuDeviceTotalMemHandle == null) {
                    cuDeviceTotalMemHandle = bind(linker, lookup, "cuDeviceTotalMem", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
                }
                cuCtxCreateHandle = bindOptional(linker, lookup, "cuCtxCreate_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
                if (cuCtxCreateHandle == null) {
                    cuCtxCreateHandle = bind(linker, lookup, "cuCtxCreate", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
                }
                cuCtxDestroyHandle = bindOptional(linker, lookup, "cuCtxDestroy_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                if (cuCtxDestroyHandle == null) {
                    cuCtxDestroyHandle = bind(linker, lookup, "cuCtxDestroy", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                }
                cuCtxSetCurrentHandle = bind(linker, lookup, "cuCtxSetCurrent", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                cuCtxPushCurrentHandle = bindOptional(linker, lookup, "cuCtxPushCurrent_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                if (cuCtxPushCurrentHandle == null) {
                    cuCtxPushCurrentHandle = bind(linker, lookup, "cuCtxPushCurrent", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                }
                cuCtxPopCurrentHandle = bindOptional(linker, lookup, "cuCtxPopCurrent_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                if (cuCtxPopCurrentHandle == null) {
                    cuCtxPopCurrentHandle = bind(linker, lookup, "cuCtxPopCurrent", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                }
                cuDevicePrimaryCtxRetainHandle = bindOptional(linker, lookup, "cuDevicePrimaryCtxRetain", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
                cuDevicePrimaryCtxReleaseHandle = bindOptional(linker, lookup, "cuDevicePrimaryCtxRelease", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));

                cuStreamCreateHandle = bindOptional(linker, lookup, "cuStreamCreate", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
                cuStreamCreateWithPriorityHandle = bindOptional(linker, lookup, "cuStreamCreateWithPriority", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
                cuStreamDestroyHandle = bindOptional(linker, lookup, "cuStreamDestroy_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                if (cuStreamDestroyHandle == null) {
                    cuStreamDestroyHandle = bind(linker, lookup, "cuStreamDestroy", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                }
                cuStreamSynchronizeHandle = bind(linker, lookup, "cuStreamSynchronize", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                cuStreamQueryHandle = bind(linker, lookup, "cuStreamQuery", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

                cuEventCreateHandle = bind(linker, lookup, "cuEventCreate", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
                cuEventDestroyHandle = bindOptional(linker, lookup, "cuEventDestroy_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                if (cuEventDestroyHandle == null) {
                    cuEventDestroyHandle = bind(linker, lookup, "cuEventDestroy", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                }
                cuEventRecordHandle = bind(linker, lookup, "cuEventRecord", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                cuEventSynchronizeHandle = bind(linker, lookup, "cuEventSynchronize", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                cuEventQueryHandle = bind(linker, lookup, "cuEventQuery", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                cuStreamWaitEventHandle = bind(linker, lookup, "cuStreamWaitEvent", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

                cuMemAllocHandle = bindOptional(linker, lookup, "cuMemAlloc_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
                if (cuMemAllocHandle == null) {
                    cuMemAllocHandle = bind(linker, lookup, "cuMemAlloc", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
                }
                cuMemAllocAsyncHandle = bindOptional(linker, lookup, "cuMemAllocAsync", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
                cuMemFreeHandle = bindOptional(linker, lookup, "cuMemFree_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                if (cuMemFreeHandle == null) {
                    cuMemFreeHandle = bind(linker, lookup, "cuMemFree", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                }
                cuMemFreeAsyncHandle = bindOptional(linker, lookup, "cuMemFreeAsync", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

                cuMemcpyHtoDHandle = bindOptional(linker, lookup, "cuMemcpyHtoD_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
                if (cuMemcpyHtoDHandle == null) {
                    cuMemcpyHtoDHandle = bind(linker, lookup, "cuMemcpyHtoD", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
                }
                cuMemcpyHtoDAsyncHandle = bindOptional(linker, lookup, "cuMemcpyHtoDAsync_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
                if (cuMemcpyHtoDAsyncHandle == null) {
                    cuMemcpyHtoDAsyncHandle = bind(linker, lookup, "cuMemcpyHtoDAsync", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
                }
                cuMemcpyDtoHHandle = bindOptional(linker, lookup, "cuMemcpyDtoH_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
                if (cuMemcpyDtoHHandle == null) {
                    cuMemcpyDtoHHandle = bind(linker, lookup, "cuMemcpyDtoH", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
                }
                cuMemcpyDtoHAsyncHandle = bindOptional(linker, lookup, "cuMemcpyDtoHAsync_v2", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
                if (cuMemcpyDtoHAsyncHandle == null) {
                    cuMemcpyDtoHAsyncHandle = bind(linker, lookup, "cuMemcpyDtoHAsync", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
                }

                cuModuleLoadDataHandle = bind(linker, lookup, "cuModuleLoadData", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                cuModuleUnloadHandle = bind(linker, lookup, "cuModuleUnload", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                cuModuleGetFunctionHandle = bind(linker, lookup, "cuModuleGetFunction", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                cuLaunchKernelHandle = bind(linker, lookup, "cuLaunchKernel", FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS
                ));

                cuGetErrorStringHandle = bindOptional(linker, lookup, "cuGetErrorString", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                cuGetErrorNameHandle = bindOptional(linker, lookup, "cuGetErrorName", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

                int initRes = cuInit(0);
                available = (initRes == CUDA_SUCCESS);
                if (available) {
                    LOGGER.info("CUDA Driver initialized successfully (code: {})", initRes);
                } else {
                    LOGGER.warn("cuInit returned non-zero code: {}", initRes);
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("CUDA Driver API not available on this system", t);
        }
        IS_AVAILABLE = available;
    }

    private static MethodHandle bind(Linker linker, SymbolLookup lookup, String name, FunctionDescriptor descriptor) {
        Optional<MemorySegment> symbol = lookup.find(name);
        if (symbol.isEmpty()) {
            throw new UnsatisfiedLinkError("CUDA function not found: " + name);
        }
        return linker.downcallHandle(symbol.get(), descriptor);
    }

    private static MethodHandle bindOptional(Linker linker, SymbolLookup lookup, String name, FunctionDescriptor descriptor) {
        Optional<MemorySegment> symbol = lookup.find(name);
        return symbol.map(s -> linker.downcallHandle(s, descriptor)).orElse(null);
    }

    public static void checkCUDAError(int result) {
        if (result != CUDA_SUCCESS) {
            String errorName = getErrorName(result);
            String errorString = getErrorString(result);
            throw new RuntimeException(String.format("CUDA error %d [%s]: %s", result, errorName, errorString));
        }
    }

    public static String getErrorString(int result) {
        if (cuGetErrorStringHandle != null) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pStr = arena.allocate(ValueLayout.ADDRESS);
                int res = (int) cuGetErrorStringHandle.invokeExact(result, pStr);
                if (res == CUDA_SUCCESS) {
                    MemorySegment strSeg = pStr.get(ValueLayout.ADDRESS, 0);
                    if (!strSeg.equals(MemorySegment.NULL)) {
                        return strSeg.getString(0);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return "CUDA_ERROR_" + result;
    }

    public static String getErrorName(int result) {
        if (cuGetErrorNameHandle != null) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pStr = arena.allocate(ValueLayout.ADDRESS);
                int res = (int) cuGetErrorNameHandle.invokeExact(result, pStr);
                if (res == CUDA_SUCCESS) {
                    MemorySegment strSeg = pStr.get(ValueLayout.ADDRESS, 0);
                    if (!strSeg.equals(MemorySegment.NULL)) {
                        return strSeg.getString(0);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return "UNKNOWN_ERROR";
    }

    public static int cuInit(int flags) {
        try {
            return (int) cuInitHandle.invokeExact(flags);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuDriverGetVersion(MemorySegment pVersion) {
        try {
            return (int) cuDriverGetVersionHandle.invokeExact(pVersion);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuDeviceGetCount(MemorySegment pCount) {
        try {
            return (int) cuDeviceGetCountHandle.invokeExact(pCount);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuDeviceGet(MemorySegment pDevice, int ordinal) {
        try {
            return (int) cuDeviceGetHandle.invokeExact(pDevice, ordinal);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuDeviceGetName(MemorySegment name, int len, int device) {
        try {
            return (int) cuDeviceGetNameHandle.invokeExact(name, len, device);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuDeviceGetUuid(MemorySegment pUuid, int device) {
        if (cuDeviceGetUuidHandle != null) {
            try {
                return (int) cuDeviceGetUuidHandle.invokeExact(pUuid, device);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return -1;
    }

    public static int cuDeviceGetAttribute(MemorySegment pi, int attrib, int device) {
        try {
            return (int) cuDeviceGetAttributeHandle.invokeExact(pi, attrib, device);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuDeviceTotalMem(MemorySegment pBytes, int device) {
        try {
            return (int) cuDeviceTotalMemHandle.invokeExact(pBytes, device);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuCtxCreate(MemorySegment pctx, int flags, int dev) {
        try {
            return (int) cuCtxCreateHandle.invokeExact(pctx, flags, dev);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuCtxDestroy(MemorySegment ctx) {
        try {
            return (int) cuCtxDestroyHandle.invokeExact(ctx);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuCtxSetCurrent(MemorySegment ctx) {
        try {
            return (int) cuCtxSetCurrentHandle.invokeExact(ctx);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuCtxPushCurrent(MemorySegment ctx) {
        try {
            return (int) cuCtxPushCurrentHandle.invokeExact(ctx);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuCtxPopCurrent(MemorySegment pctx) {
        try {
            return (int) cuCtxPopCurrentHandle.invokeExact(pctx);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuDevicePrimaryCtxRetain(MemorySegment pctx, int dev) {
        if (cuDevicePrimaryCtxRetainHandle != null) {
            try {
                return (int) cuDevicePrimaryCtxRetainHandle.invokeExact(pctx, dev);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return cuCtxCreate(pctx, 0, dev);
    }

    public static int cuDevicePrimaryCtxRelease(int dev) {
        if (cuDevicePrimaryCtxReleaseHandle != null) {
            try {
                return (int) cuDevicePrimaryCtxReleaseHandle.invokeExact(dev);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return CUDA_SUCCESS;
    }

    public static int cuStreamCreate(MemorySegment phStream, int flags) {
        try {
            return (int) cuStreamCreateHandle.invokeExact(phStream, flags);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuStreamCreateWithPriority(MemorySegment phStream, int flags, int priority) {
        if (cuStreamCreateWithPriorityHandle != null) {
            try {
                return (int) cuStreamCreateWithPriorityHandle.invokeExact(phStream, flags, priority);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return cuStreamCreate(phStream, flags);
    }

    public static int cuStreamDestroy(MemorySegment hStream) {
        try {
            return (int) cuStreamDestroyHandle.invokeExact(hStream);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuStreamSynchronize(MemorySegment hStream) {
        try {
            return (int) cuStreamSynchronizeHandle.invokeExact(hStream);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuStreamQuery(MemorySegment hStream) {
        try {
            return (int) cuStreamQueryHandle.invokeExact(hStream);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuEventCreate(MemorySegment phEvent, int flags) {
        try {
            return (int) cuEventCreateHandle.invokeExact(phEvent, flags);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuEventDestroy(MemorySegment hEvent) {
        try {
            return (int) cuEventDestroyHandle.invokeExact(hEvent);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuEventRecord(MemorySegment hEvent, MemorySegment hStream) {
        try {
            return (int) cuEventRecordHandle.invokeExact(hEvent, hStream);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuEventSynchronize(MemorySegment hEvent) {
        try {
            return (int) cuEventSynchronizeHandle.invokeExact(hEvent);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuEventQuery(MemorySegment hEvent) {
        try {
            return (int) cuEventQueryHandle.invokeExact(hEvent);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuStreamWaitEvent(MemorySegment hStream, MemorySegment hEvent, int flags) {
        try {
            return (int) cuStreamWaitEventHandle.invokeExact(hStream, hEvent, flags);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuMemAlloc(MemorySegment pdptr, long byteSize) {
        try {
            return (int) cuMemAllocHandle.invokeExact(pdptr, byteSize);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuMemAllocAsync(MemorySegment pdptr, long byteSize, MemorySegment hStream) {
        if (cuMemAllocAsyncHandle != null) {
            try {
                return (int) cuMemAllocAsyncHandle.invokeExact(pdptr, byteSize, hStream);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return cuMemAlloc(pdptr, byteSize);
    }

    public static int cuMemFree(MemorySegment dptr) {
        try {
            return (int) cuMemFreeHandle.invokeExact(dptr);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuMemFreeAsync(MemorySegment dptr, MemorySegment hStream) {
        if (cuMemFreeAsyncHandle != null) {
            try {
                return (int) cuMemFreeAsyncHandle.invokeExact(dptr, hStream);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return cuMemFree(dptr);
    }

    public static int cuMemcpyHtoD(MemorySegment dstDevice, MemorySegment srcHost, long byteSize) {
        try {
            return (int) cuMemcpyHtoDHandle.invokeExact(dstDevice, srcHost, byteSize);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuMemcpyHtoDAsync(MemorySegment dstDevice, MemorySegment srcHost, long byteSize, MemorySegment hStream) {
        try {
            return (int) cuMemcpyHtoDAsyncHandle.invokeExact(dstDevice, srcHost, byteSize, hStream);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuMemcpyDtoH(MemorySegment dstHost, MemorySegment srcDevice, long byteSize) {
        try {
            return (int) cuMemcpyDtoHHandle.invokeExact(dstHost, srcDevice, byteSize);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuMemcpyDtoHAsync(MemorySegment dstHost, MemorySegment srcDevice, long byteSize, MemorySegment hStream) {
        try {
            return (int) cuMemcpyDtoHAsyncHandle.invokeExact(dstHost, srcDevice, byteSize, hStream);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuModuleLoadData(MemorySegment pModule, MemorySegment image) {
        try {
            return (int) cuModuleLoadDataHandle.invokeExact(pModule, image);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuModuleUnload(MemorySegment hModule) {
        try {
            return (int) cuModuleUnloadHandle.invokeExact(hModule);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuModuleGetFunction(MemorySegment phFunc, MemorySegment hModule, MemorySegment name) {
        try {
            return (int) cuModuleGetFunctionHandle.invokeExact(phFunc, hModule, name);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static int cuLaunchKernel(MemorySegment f,
                                     int gridDimX, int gridDimY, int gridDimZ,
                                     int blockDimX, int blockDimY, int blockDimZ,
                                     int sharedMemBytes, MemorySegment hStream,
                                     MemorySegment kernelParams, MemorySegment extra) {
        try {
            return (int) cuLaunchKernelHandle.invokeExact(
                    f,
                    gridDimX, gridDimY, gridDimZ,
                    blockDimX, blockDimY, blockDimZ,
                    sharedMemBytes, hStream,
                    kernelParams, extra
            );
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
