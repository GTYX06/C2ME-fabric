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
        int gy = Math.floorDiv(y + 1, 12);
        int gz = (z - 5) >> 4;

        int A = Integer.MAX_VALUE;
        int B = Integer.MAX_VALUE;
        int C = Integer.MAX_VALUE;
        int D = Integer.MAX_VALUE;

        int index = 12;
        for (int offY = -1; offY <= 1; ++offY) {
            int gymul = (gy + offY) * 12;
            for (int offZ = 0; offZ <= 1; ++offZ) {
                int gzmul = (gz + offZ) << 4;

                int index0 = index - 1;
                int posIdx0 = (gy + offY - startY) * sizeZ * sizeX + (gz + offZ - startZ) * sizeX + (gx - startX);
                int position0 = packedBlockPositions[posIdx0] & 0xFFFF;
                int dx0 = (gx << 4) + (position0 >> 8) - x;
                int dy0 = gymul + ((position0 >> 4) & 15) - y;
                int dz0 = gzmul + (position0 & 15) - z;
                int dist0 = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;

                int index1 = index - 2;
                int posIdx1 = posIdx0 + 1;
                int position1 = packedBlockPositions[posIdx1] & 0xFFFF;
                int dx1 = ((gx + 1) << 4) + (position1 >> 8) - x;
                int dy1 = gymul + ((position1 >> 4) & 15) - y;
                int dz1 = gzmul + (position1 & 15) - z;
                int dist1 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1;

                int dist0Clamped = Math.min(dist0, 2047);
                int p0 = (dist0Clamped << 20) | (index0 << 16) | (posIdx0 & 0xFFFF);
                if (p0 <= C) {
                    int n01 = Math.max(A, p0);
                    A = Math.min(A, p0);

                    int n02 = Math.max(B, n01);
                    B = Math.min(B, n01);

                    int n03 = Math.max(C, n02);
                    C = Math.min(C, n02);

                    D = Math.min(D, n03);
                }

                int dist1Clamped = Math.min(dist1, 2047);
                int p1 = (dist1Clamped << 20) | (index1 << 16) | (posIdx1 & 0xFFFF);
                if (p1 <= C) {
                    int n11 = Math.max(A, p1);
                    A = Math.min(A, p1);

                    int n12 = Math.max(B, n11);
                    B = Math.min(B, n11);

                    int n13 = Math.max(C, n12);
                    C = Math.min(C, n12);

                    D = Math.min(D, n13);
                }

                index -= 2;
            }
        }

        res[0] = A;
        res[1] = B;
        res[2] = C;
        res[3] = D;
    }
}
