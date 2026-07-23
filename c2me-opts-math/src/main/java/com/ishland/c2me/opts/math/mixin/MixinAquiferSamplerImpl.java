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

package com.ishland.c2me.opts.math.mixin;

import com.ishland.c2me.opts.math.common.VectorAquiferSampler;
import net.minecraft.world.gen.chunk.AquiferSampler;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AquiferSampler.Impl.class, priority = 1101)
public class MixinAquiferSamplerImpl {

    @Dynamic
    @Shadow
    private int c2me$packed1;
    @Dynamic
    @Shadow
    private int c2me$packed2;
    @Dynamic
    @Shadow
    private int c2me$packed3;
    @Dynamic
    @Shadow
    private int c2me$packed4;

    @Dynamic
    @Shadow
    private short[] c2me$packedBlockPositions;

    @Shadow @Final private int startX;
    @Shadow @Final private int startY;
    @Shadow @Final private int startZ;
    @Shadow @Final private int sizeX;
    @Shadow @Final private int sizeZ;

    @Unique
    private final int[] c2me$resArray = new int[4];

    /**
     * @author ishland
     * @reason replace impl with Vector API
     */
    @Dynamic
    @Overwrite
    private void aquiferExtracted$refreshDistPosIdx(int x, int y, int z) {
        VectorAquiferSampler.refreshDistPosIdx(this.c2me$packedBlockPositions, this.c2me$resArray,
                this.startX, this.startY, this.startZ, this.sizeX, this.sizeZ, x, y, z);
        this.c2me$packed1 = this.c2me$resArray[0];
        this.c2me$packed2 = this.c2me$resArray[1];
        this.c2me$packed3 = this.c2me$resArray[2];
        this.c2me$packed4 = this.c2me$resArray[3];
    }
}
