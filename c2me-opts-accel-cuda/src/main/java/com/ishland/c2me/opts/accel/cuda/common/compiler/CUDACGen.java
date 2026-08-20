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

package com.ishland.c2me.opts.accel.cuda.common.compiler;

import com.ishland.c2me.base.mixin.access.IDoublePerlinNoiseSampler;
import com.ishland.c2me.base.mixin.access.IInterpolatedNoiseSampler;
import com.ishland.c2me.base.mixin.access.IOctavePerlinNoiseSampler;
import com.ishland.c2me.base.mixin.access.IPerlinNoiseSampler;
import com.ishland.c2me.base.mixin.access.ISimplexNoiseSampler;
import com.ishland.c2me.opts.accel.cuda.common.compiler.emitters.misc.CUDABlockStateMappings;
import com.ishland.c2me.opts.accel.cuda.common.util.CUDAStructs;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;
import com.ishland.c2me.opts.dfc.common.ast.misc.CacheLikeNode;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantF32Node;
import com.ishland.c2me.opts.dfc.common.ast.misc.ConstantNode;
import com.ishland.c2me.opts.dfc.common.ast.spline.SplineNormalNode;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACGenContext;
import com.ishland.c2me.opts.dfc.common.gen.cuda.CUDACGenFunctionContext;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF32;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CUDACGen implements CUDACGenContext {

    private final AtomicInteger methodNameCounter = new AtomicInteger();
    private final AtomicInteger varNameCounter = new AtomicInteger();

    private final Reference2IntOpenHashMap<Object> constDataOffsets = new Reference2IntOpenHashMap<>();
    private final ByteArrayOutputStream constDataStream = new ByteArrayOutputStream();

    private final Object2IntOpenHashMap<Object> dynamicDataOffsets = new Object2IntOpenHashMap<>();

    private final Object2IntOpenHashMap<CacheLikeNode> flatCacheIds = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<CacheLikeNode> cache2dIds = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<CacheLikeNode> interpolatorIds = new Object2IntOpenHashMap<>();

    private final StringBuilder generatedMethods = new StringBuilder();
    private final Map<String, String> defines = new HashMap<>();

    public CUDACGen() {
        constDataOffsets.defaultReturnValue(-1);
        dynamicDataOffsets.defaultReturnValue(-1);
        flatCacheIds.defaultReturnValue(-1);
        cache2dIds.defaultReturnValue(-1);
        interpolatorIds.defaultReturnValue(-1);
    }

    public static GeneratedCUDASource compile(NoiseConfig noiseConfig, ChunkGeneratorSettings settings, MultiNoiseUtil.MultiNoiseSampler noiseSampler, MultiNoiseUtil.Entries<RegistryEntry<Biome>> biomeEntries) {
        CUDACGen gen = new CUDACGen();
        return gen.doCompile(noiseConfig, settings, noiseSampler, biomeEntries);
    }

    private GeneratedCUDASource doCompile(NoiseConfig noiseConfig, ChunkGeneratorSettings settings, MultiNoiseUtil.MultiNoiseSampler noiseSampler, MultiNoiseUtil.Entries<RegistryEntry<Biome>> biomeEntries) {
        NoiseRouter originalNoiseRouter = null;
        if ((Object) noiseConfig.getNoiseRouter() instanceof com.ishland.c2me.opts.dfc.common.ducks.NoiseRouterExtension ext) {
            originalNoiseRouter = ext.c2me$getOriginalNoiseRouter();
        }
        NoiseRouter router = originalNoiseRouter != null ? originalNoiseRouter : noiseConfig.getNoiseRouter();
        GenerationShapeConfig shape = settings.generationShapeConfig();

        defines.put("DF_COMPILE_ESTIMATE_SURFACE_HEIGHT", "1");
        defines.put("DF_COMPILE_AQUIFER_PREFILL", "1");
        defines.put("DF_COMPILE_NOISE_KERNEL", "1");
        defines.put("DF_COMPILE_BIOME_MULTINOISE_KERNEL", "1");

        compileDensityFunctionBinding("df_binding_barrier", router.barrierNoise());
        compileDensityFunctionBinding("df_binding_fluid_level_floodedness", router.fluidLevelFloodednessNoise());
        compileDensityFunctionBinding("df_binding_fluid_level_spread", router.fluidLevelSpreadNoise());
        compileDensityFunctionBinding("df_binding_lava", router.lavaNoise());
        compileDensityFunctionBinding("df_binding_temperature", router.temperature());
        compileDensityFunctionBinding("df_binding_vegetation", router.vegetation());
        compileDensityFunctionBinding("df_binding_continents", router.continents());
        compileDensityFunctionBinding("df_binding_erosion", router.erosion());
        compileDensityFunctionBinding("df_binding_depth", router.depth());
        compileDensityFunctionBinding("df_binding_ridges", router.ridges());
        compileDensityFunctionBinding("df_binding_preliminary_surface_level", router.preliminarySurfaceLevel());
        compileDensityFunctionBinding("df_binding_final_density", router.finalDensity());
        compileDensityFunctionBinding("df_binding_vein_toggle", router.veinToggle());
        compileDensityFunctionBinding("df_binding_vein_ridged", router.veinRidged());
        compileDensityFunctionBinding("df_binding_vein_gap", router.veinGap());

        DensityFunction finalFinalDensity = null;
        if (originalNoiseRouter != null && (Object) originalNoiseRouter instanceof com.ishland.c2me.opts.dfc.common.ducks.NoiseRouterExtension ext) {
            finalFinalDensity = ext.c2me$getFinalFinalDensity();
        }
        if (finalFinalDensity == null) {
            finalFinalDensity = settings.hasAquifers() ? router.finalDensity() : DensityFunctionTypes.constant(-1.0);
        }
        compileDensityFunctionBinding("df_binding_final_final_density", finalFinalDensity);

        Pair<Integer, List<RegistryEntry<Biome>>> biomeTreeResult = genBiomeTree(biomeEntries);
        int biomeTreeOffset = biomeTreeResult.first();
        List<RegistryEntry<Biome>> biomeList = biomeTreeResult.second();
        @SuppressWarnings("unchecked")
        RegistryEntry<Biome>[] biomeRegistryEntries = biomeList.toArray(RegistryEntry[]::new);

        StringBuilder fullSource = new StringBuilder();
        fullSource.append(loadResourceString("/cudasources/c2me_cuda_ext_math.cu"));
        fullSource.append("\n\n");

        fullSource.append(String.format("__constant__ const int32_t genShapeCfg_minimumY = %d;\n", shape.minimumY()));
        fullSource.append(String.format("__constant__ const int32_t genShapeCfg_height = %d;\n", shape.height()));
        fullSource.append(String.format("__constant__ const uint32_t genShapeCfg_horizontalSize = %d;\n", shape.horizontalSize()));
        fullSource.append(String.format("__constant__ const uint32_t genShapeCfg_verticalSize = %d;\n", shape.verticalSize()));
        fullSource.append(String.format("__constant__ const uint32_t biome_multinoise_tree_offset = %d;\n", biomeTreeOffset));
        fullSource.append(String.format("__constant__ const uint32_t biome_multinoise_tree_nodes_c = %d;\n\n", biomeList.size() * 2));

        fullSource.append(generatedMethods);

        byte[] constData = constDataStream.toByteArray();
        BlockState[] blockStates = CUDABlockStateMappings.getDefaultBlockStates(settings.defaultBlock(), settings.defaultFluid());

        return new GeneratedCUDASource(
                fullSource.toString(),
                constData,
                dynamicDataOffsets,
                defines,
                biomeRegistryEntries,
                blockStates,
                flatCacheIds.size(),
                cache2dIds.size(),
                interpolatorIds.size()
        );
    }

    private void compileDensityFunctionBinding(String bindingName, DensityFunction df) {
        if (df == null) {
            df = DensityFunctionTypes.constant(0.0);
        }
        AstNode node = McToAst.toAst(df);
        FunctionContext funcCtx = new FunctionContext(this, CUDACGenFunctionContext.FunctionVariant.FULLY_CACHED);
        String res = node.generateCUDAC(funcCtx, null);
        funcCtx.appendRaw(String.format("return %s;\n", res));

        generatedMethods.append(String.format("__device__ double %s(const sample_int32_ctx_t ctx) {\n%s}\n\n", bindingName, indent(funcCtx.getBody(), 4)));
    }

    private Pair<Integer, List<RegistryEntry<Biome>>> genBiomeTree(MultiNoiseUtil.Entries<RegistryEntry<Biome>> biomeEntries) {
        List<com.mojang.datafixers.util.Pair<MultiNoiseUtil.NoiseHypercube, RegistryEntry<Biome>>> entries = biomeEntries.getEntries();
        List<RegistryEntry<Biome>> resultBiomes = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.allocate(entries.size() * 2 * CUDAStructs.SIZEOF_BIOME_SEARCH_TREE_NODE + 1024).order(ByteOrder.LITTLE_ENDIAN);

        int rootNodeState = 0;
        for (int i = 0; i < entries.size(); i++) {
            com.mojang.datafixers.util.Pair<MultiNoiseUtil.NoiseHypercube, RegistryEntry<Biome>> entry = entries.get(i);
            MultiNoiseUtil.NoiseHypercube cube = entry.getFirst();
            RegistryEntry<Biome> biome = entry.getSecond();

            resultBiomes.add(biome);
            int biomeIdx = resultBiomes.size() - 1;

            buf.putInt(0);
            buf.putInt(biomeIdx);
            buf.putInt(0);
            buf.putInt(0);

            buf.putShort((short) cube.temperature().min());
            buf.putShort((short) cube.humidity().min());
            buf.putShort((short) cube.continentalness().min());
            buf.putShort((short) cube.erosion().min());
            buf.putShort((short) cube.depth().min());
            buf.putShort((short) cube.weirdness().min());
            buf.putShort((short) cube.offset());

            buf.putShort((short) cube.temperature().max());
            buf.putShort((short) cube.humidity().max());
            buf.putShort((short) cube.continentalness().max());
            buf.putShort((short) cube.erosion().max());
            buf.putShort((short) cube.depth().max());
            buf.putShort((short) cube.weirdness().max());
            buf.putShort((short) cube.offset());
        }

        byte[] treeBytes = Arrays.copyOf(buf.array(), buf.position());
        int offset = allocGlobalConstData(treeBytes, 16);
        return new Pair<>(offset, resultBiomes);
    }

    private static String loadResourceString(String path) {
        try (InputStream in = CUDACGen.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("Resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String indent(String s, int spaces) {
        String pad = " ".repeat(spaces);
        return pad + s.replace("\n", "\n" + pad);
    }

    @Override
    public String nextMethodName() {
        return "df_method_" + methodNameCounter.incrementAndGet();
    }

    @Override
    public String nextMethodName(String suffix) {
        return "df_method_" + methodNameCounter.incrementAndGet() + suffix;
    }

    @Override
    public ValuesMethodDef newDispatcher(AstNode node, String id, AstNode.ReturnType returnType) {
        return newDispatcherF64(node, id);
    }

    @Override
    public ValuesMethodDefF64 newDispatcherF64(AstNode node) {
        return newDispatcherF64(node, "");
    }

    @Override
    public ValuesMethodDefF64 newDispatcherF64(AstNode node, String id) {
        String name = nextMethodName(id);
        return new ValuesMethodDefF64(name);
    }

    @Override
    public ValuesMethodDefF32 newDispatcherF32(AstNode node) {
        return newDispatcherF32(node, "");
    }

    @Override
    public ValuesMethodDefF32 newDispatcherF32(AstNode node, String id) {
        String name = nextMethodName(id);
        return new ValuesMethodDefF32(name);
    }

    @Override
    public ValuesMethodDef newMethod(AstNode node, CUDACGenFunctionContext.FunctionVariant variant, AstNode.ReturnType returnType) {
        return newMethodF64(node, variant);
    }

    @Override
    public ValuesMethodDefF64 newMethodF64(AstNode node, CUDACGenFunctionContext.FunctionVariant variant) {
        String name = nextMethodName(variant.suffix);
        return new ValuesMethodDefF64(name);
    }

    @Override
    public ValuesMethodDefF32 newMethodF32(AstNode node, CUDACGenFunctionContext.FunctionVariant variant) {
        String name = nextMethodName(variant.suffix);
        return new ValuesMethodDefF32(name);
    }

    @Override
    public String callDelegate(ValuesMethodDef target, AstNode.ReturnType returnType) {
        if (target.isConst()) {
            return target.returnType() == AstNode.ReturnType.F32 ?
                    Float.toString((float) ((ValuesMethodDefF32) target).constValue()) + "f" :
                    Double.toString(((ValuesMethodDefF64) target).constValue());
        }
        return target.generatedMethod() + "(ctx)";
    }

    @Override
    public String callDelegate(ValuesMethodDefF64 target) {
        if (target.isConst()) {
            return Double.toString(target.constValue());
        }
        return target.generatedMethod() + "(ctx)";
    }

    @Override
    public String callDelegate(ValuesMethodDefF32 target) {
        if (target.isConst()) {
            return Float.toString((float) target.constValue()) + "f";
        }
        return target.generatedMethod() + "(ctx)";
    }

    @Override
    public synchronized int allocGlobalConstData(byte[] data, int alignment) {
        int currentSize = constDataStream.size();
        int padding = (alignment - (currentSize % alignment)) % alignment;
        for (int i = 0; i < padding; i++) {
            constDataStream.write(0);
        }
        int offset = constDataStream.size();
        try {
            constDataStream.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return offset;
    }

    @Override
    public synchronized int allocGlobalConstDataObject(Object obj) {
        int existing = constDataOffsets.getInt(obj);
        if (existing != -1) return existing;

        byte[] encoded;
        if (obj instanceof DoublePerlinNoiseSampler sampler) {
            encoded = encodeDoublePerlinNoise(sampler);
        } else if (obj instanceof InterpolatedNoiseSampler sampler) {
            encoded = encodeInterpolatedNoise(sampler);
        } else if (obj instanceof SimplexNoiseSampler sampler) {
            encoded = encodeSimplexNoise(sampler);
        } else if (obj instanceof SplineNormalNode spline) {
            encoded = encodeSpline(spline);
        } else if (obj instanceof DensityFunction.Noise noise) {
            encoded = encodeNoise(noise);
        } else {
            throw new IllegalArgumentException("Unsupported const object type: " + obj.getClass().getName());
        }

        int offset = allocGlobalConstData(encoded, 8);
        constDataOffsets.put(obj, offset);
        return offset;
    }

    private byte[] encodeSpline(SplineNormalNode spline) {
        int len = spline.locations.length;
        ByteBuffer buf = ByteBuffer.allocate(16 + len * 12).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(len);
        for (float loc : spline.locations) {
            buf.putFloat(loc);
        }
        for (AstNode valNode : spline.values) {
            float val = 0.0f;
            if (valNode instanceof ConstantF32Node c) {
                val = c.getValue();
            } else if (valNode instanceof ConstantNode c) {
                val = (float) c.getValue();
            }
            buf.putFloat(val);
        }
        for (float deriv : spline.derivatives) {
            buf.putFloat(deriv);
        }
        return Arrays.copyOf(buf.array(), buf.position());
    }

    private byte[] encodeNoise(DensityFunction.Noise noise) {
        DoublePerlinNoiseSampler sampler = noise.noise();
        if (sampler != null) {
            return encodeDoublePerlinNoise(sampler);
        }
        return new byte[0];
    }

    private byte[] encodeDoublePerlinNoise(DoublePerlinNoiseSampler sampler) {
        IDoublePerlinNoiseSampler samplerAcc = (IDoublePerlinNoiseSampler) sampler;
        ByteBuffer buf = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN);
        buf.putDouble(samplerAcc.getAmplitude());

        IOctavePerlinNoiseSampler firstAcc = (IOctavePerlinNoiseSampler) (Object) samplerAcc.getFirstSampler();
        IOctavePerlinNoiseSampler secondAcc = (IOctavePerlinNoiseSampler) (Object) samplerAcc.getSecondSampler();

        PerlinNoiseSampler first = firstAcc.getOctaveSamplers()[0];
        PerlinNoiseSampler second = secondAcc.getOctaveSamplers()[0];

        buf.putDouble(1.0);
        buf.putDouble(first.originX);
        buf.putDouble(first.originY);
        buf.putDouble(first.originZ);
        buf.put(((IPerlinNoiseSampler) (Object) first).getPermutation());
        buf.putDouble(0.0);

        buf.putDouble(1.0);
        buf.putDouble(second.originX);
        buf.putDouble(second.originY);
        buf.putDouble(second.originZ);
        buf.put(((IPerlinNoiseSampler) (Object) second).getPermutation());
        buf.putDouble(0.0);

        return Arrays.copyOf(buf.array(), buf.position());
    }

    private byte[] encodeInterpolatedNoise(InterpolatedNoiseSampler sampler) {
        IInterpolatedNoiseSampler samplerAcc = (IInterpolatedNoiseSampler) sampler;
        ByteBuffer buf = ByteBuffer.allocate(32768).order(ByteOrder.LITTLE_ENDIAN);
        buf.putDouble(samplerAcc.getScaledXzScale());
        buf.putDouble(samplerAcc.getScaledYScale());
        buf.putDouble(samplerAcc.getXzFactor());
        buf.putDouble(samplerAcc.getYFactor());
        buf.putDouble(samplerAcc.getSmearScaleMultiplier());
        buf.putDouble(0.0);
        buf.putDouble(0.0);
        buf.putDouble(0.0);
        return Arrays.copyOf(buf.array(), buf.position());
    }

    private byte[] encodeSimplexNoise(SimplexNoiseSampler sampler) {
        ByteBuffer buf = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        int[] perms = ((ISimplexNoiseSampler) sampler).getPermutation();
        for (int p : perms) {
            buf.putInt(p);
        }
        return Arrays.copyOf(buf.array(), buf.position());
    }

    @Override
    public synchronized int allocGlobalDynamicData(Object data) {
        int existing = dynamicDataOffsets.getInt(data);
        if (existing != -1) return existing;
        int nextIdx = dynamicDataOffsets.size();
        dynamicDataOffsets.put(data, nextIdx);
        return nextIdx;
    }

    @Override
    public synchronized int getGlobalDynamicDataOffset(Object data) {
        return dynamicDataOffsets.getInt(data);
    }

    @Override
    public synchronized int registerFlatCache(CacheLikeNode node) {
        int existing = flatCacheIds.getInt(node);
        if (existing != -1) return existing;
        int nextId = flatCacheIds.size();
        flatCacheIds.put(node, nextId);
        return nextId;
    }

    @Override
    public synchronized int registerCache2d(CacheLikeNode node) {
        int existing = cache2dIds.getInt(node);
        if (existing != -1) return existing;
        int nextId = cache2dIds.size();
        cache2dIds.put(node, nextId);
        return nextId;
    }

    @Override
    public synchronized int registerInterpolator(CacheLikeNode node) {
        int existing = interpolatorIds.getInt(node);
        if (existing != -1) return existing;
        int nextId = interpolatorIds.size();
        interpolatorIds.put(node, nextId);
        return nextId;
    }

    @Override
    public void appendRaw(String raw) {
        generatedMethods.append(raw);
    }

    public static class FunctionContext implements CUDACGenFunctionContext {
        private final CUDACGen globalContext;
        private final FunctionVariant variant;
        private final StringBuilder body = new StringBuilder();

        public FunctionContext(CUDACGen globalContext, FunctionVariant variant) {
            this.globalContext = globalContext;
            this.variant = variant;
            this.body.append("const uint8_t * const cdata = (const uint8_t *) ctx.const_data;\n");
        }

        @Override
        public CUDACGen getGlobalContext() {
            return globalContext;
        }

        @Override
        public FunctionVariant getVariant() {
            return variant;
        }

        @Override
        public String nextVarName() {
            return "_var_" + globalContext.varNameCounter.incrementAndGet();
        }

        @Override
        public ValuesMethodDef newVar(AstNode node) {
            return newVarF64(node);
        }

        @Override
        public ValuesMethodDefF64 newVarF64(AstNode node) {
            String name = nextVarName();
            node.generateCUDAC(this, name);
            return new ValuesMethodDefF64(name);
        }

        @Override
        public ValuesMethodDefF32 newVarF32(AstNode node) {
            String name = nextVarName();
            node.generateCUDAC(this, name);
            return new ValuesMethodDefF32(name);
        }

        @Override
        public String newVarUnoptimized(AstNode node) {
            String name = nextVarName();
            body.append(String.format("double %s;\n", name));
            return name;
        }

        @Override
        public String getDelegateVar(ValuesMethodDef target, AstNode.ReturnType returnType) {
            if (target.isConst()) {
                return target.returnType() == AstNode.ReturnType.F32 ?
                        Float.toString((float) ((ValuesMethodDefF32) target).constValue()) + "f" :
                        Double.toString(((ValuesMethodDefF64) target).constValue());
            }
            return target.generatedMethod();
        }

        @Override
        public String getDelegateVar(ValuesMethodDefF64 target) {
            if (target.isConst()) {
                return Double.toString(target.constValue());
            }
            return target.generatedMethod();
        }

        @Override
        public String getDelegateVar(ValuesMethodDefF32 target) {
            if (target.isConst()) {
                return Float.toString((float) target.constValue()) + "f";
            }
            return target.generatedMethod();
        }

        @Override
        public CUDACGenFunctionContext fork() {
            return new FunctionContext(globalContext, variant);
        }

        @Override
        public String getBody() {
            return body.toString();
        }

        @Override
        public void appendRaw(String raw) {
            body.append(raw);
        }
    }

    public record Pair<A, B>(A first, B second) {}
}
