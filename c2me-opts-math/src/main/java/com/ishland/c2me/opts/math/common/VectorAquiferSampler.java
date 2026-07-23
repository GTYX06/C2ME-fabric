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

public class VectorAquiferSampler {

    public static void refreshDistPosIdx(short[] packedBlockPositions, int[] res,
                                         int startX, int startY, int startZ, int sizeX, int sizeZ,
                                         int x, int y, int z) {
        int gx = (x - 5) >> 4;
        int gy = Math.floorDiv(y + 1, 12) - 1;
        int gz = (z - 5) >> 4;

        int[] ps = new int[12];
        int index = 12;

        for (int offY = 0; offY <= 2; ++offY) {
            int gymul = gy * 12 + offY * 12;
            for (int offZ = 0; offZ <= 1; ++offZ) {
                int gzmul = (gz + offZ) << 4;

                int index0 = index - 1;
                int posIdx0 = (gy + offY - startY) * sizeZ * sizeX + (gz + offZ - startZ) * sizeX + (gx - startX);
                short position0 = packedBlockPositions[posIdx0];
                int dx0 = (gx << 4) + (position0 >> 8) - x;
                int dy0 = gymul + ((position0 >> 4) & 15) - y;
                int dz0 = gzmul + (position0 & 15) - z;
                int dist0 = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;

                int index1 = index - 2;
                int posIdx1 = posIdx0 + 1;
                short position1 = packedBlockPositions[posIdx1];
                int dx1 = ((gx + 1) << 4) + (position1 >> 8) - x;
                int dy1 = gymul + ((position1 >> 4) & 15) - y;
                int dz1 = gzmul + (position1 & 15) - z;
                int dist1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;

                ps[12 - index] = (dist0 << 20) | (index0 << 16) | (posIdx0 & 0xFFFF);
                ps[13 - index] = (dist1 << 20) | (index1 << 16) | (posIdx1 & 0xFFFF);

                index -= 2;
            }
        }

        int A = ps[0];
        int B = -1;
        int C = -1;
        int D = -1;

        for (int i = 1; i < 12; i++) {
            int p1 = ps[i];
            if (Integer.compareUnsigned(p1, C) <= 0) {
                int n11 = Math.max(A, p1);
                A = Math.min(A, p1);

                int n12 = Math.max(B, n11);
                B = Math.min(B, n11);

                int n13 = Math.max(C, n12);
                C = Math.min(C, n12);

                D = Math.min(D, n13);
            }
        }

        res[0] = A;
        res[1] = B;
        res[2] = C;
        res[3] = D;
    }
}
