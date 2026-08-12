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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;

public class VKEventCallbackManager implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(VKEventCallbackManager.class);

    private final ConcurrentHashMap<Long, Runnable> callbacks = new ConcurrentHashMap<>();
    private long ordinal = 0;
    private volatile boolean open = true;

    public void registerCallback(long handle, Runnable action) {
        if (!open) return;
        long id = ordinal++;
        callbacks.put(id, action);
    }

    public void trigger(long id) {
        Runnable action = callbacks.remove(id);
        if (action != null) {
            try {
                action.run();
            } catch (Throwable t) {
                LOGGER.error("Error invoking callback {}", id, t);
            }
        }
    }

    @Override
    public void close() {
        open = false;
        callbacks.clear();
    }
}
