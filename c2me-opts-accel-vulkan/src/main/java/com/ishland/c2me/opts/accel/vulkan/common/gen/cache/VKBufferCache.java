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

import com.ishland.c2me.base.common.util.MemoryUtil;
import com.ishland.c2me.opts.accel.vulkan.common.Config;
import com.ishland.flowsched.util.Assertions;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Objects;
import java.util.function.LongFunction;

public class VKBufferCache {

    private static final int MAX_CACHE_SIZE = Config.maxConcurrentTasksPerDevice + 4;

    private final int[] cacheBufferSizes = new int[Type.values().length];
    private final LongArrayList[] caches = new LongArrayList[Type.values().length];

    private final Thread owner;

    {
        for (int i = 0; i < caches.length; i++) {
            this.caches[i] = new LongArrayList();
        }
    }

    public VKBufferCache(Thread owner) {
        this.owner = Objects.requireNonNull(owner);
    }

    public synchronized BufferEntry allocate(Type type, int size, LongFunction<BufferEntry> allocator) {
        Assertions.assertTrue(Thread.currentThread() == this.owner, "Buffer cache can only be accessed from its owner thread (Current: %s, Owner: %s)", Thread.currentThread(), this.owner);

        int cacheBufferSize = this.cacheBufferSizes[type.ordinal()];
        if (cacheBufferSize < size) {
            this.cacheBufferSizes[type.ordinal()] = cacheBufferSize = MemoryUtil.roundUp(size, 4096);
            clearCache0(this.caches[type.ordinal()]);
        }
        BufferEntry entry = tryBorrow0(type, cacheBufferSize);
        if (entry == null) {
            entry = allocator.apply(cacheBufferSize);
        }
        return entry;
    }

    private synchronized BufferEntry tryBorrow0(Type type, int size) {
        Assertions.assertTrue(Thread.currentThread() == this.owner, "Buffer cache can only be accessed from its owner thread (Current: %s, Owner: %s)", Thread.currentThread(), this.owner);

        LongArrayList cache = this.caches[type.ordinal()];
        if (cache == null || cache.isEmpty()) {
            return null;
        } else {
            long l = cache.removeLong(cache.size() - 1);
            return new BufferEntry(l, size, 0L, 0L);
        }
    }

    public void returnBuffer(Type type, BufferEntry entry) {
        if (entry != null) {
            returnBuffer(type, entry.size(), entry.buffer());
        }
    }

    public synchronized void returnBuffer(Type type, int size, long buffer) {
        Assertions.assertTrue(Thread.currentThread() == this.owner, "Buffer cache can only be accessed from its owner thread (Current: %s, Owner: %s)", Thread.currentThread(), this.owner);

        LongArrayList cache = this.caches[type.ordinal()];
        if (cache == null || this.cacheBufferSizes[type.ordinal()] != size) {
            return;
        }
        cache.add(buffer);
        while (cache.size() > MAX_CACHE_SIZE) {
            cache.removeLong(0);
        }
    }

    public synchronized void clearCache() {
        Assertions.assertTrue(Thread.currentThread() == this.owner, "Buffer cache can only be accessed from its owner thread (Current: %s, Owner: %s)", Thread.currentThread(), this.owner);

        for (LongArrayList cache : this.caches) {
            clearCache0(cache);
        }
    }

    private static void clearCache0(LongArrayList cache) {
        if (cache != null) {
            cache.clear();
        }
    }

    public enum Type {
        READ_ONLY,
        READ_WRITE,
        DYNAMIC,
        ;
    }

    public record BufferEntry(long buffer, int size, long memory, long deviceAddress) {
    }
}
