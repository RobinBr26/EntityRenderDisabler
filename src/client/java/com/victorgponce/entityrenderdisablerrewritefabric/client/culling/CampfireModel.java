package com.victorgponce.entityrenderdisablerrewritefabric.client.culling;

import com.victorgponce.entityrenderdisablerrewritefabric.client.config.ModConfig;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.List;
import java.util.function.Predicate;

public final class CampfireModel implements BlockStateModel {
    private final BlockState state;
    private final BlockStateModel delegate;

    public CampfireModel(BlockState state, BlockStateModel delegate) {
        this.state = state;
        this.delegate = delegate;
    }

    @Override
    public void addParts(Random random, List<BlockModelPart> parts) {
        if (!ModConfig.isBlockHidden(state)) {
            delegate.addParts(random, parts);
        }
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockRenderView world, BlockPos pos, BlockState state, Random random, Predicate<Direction> cullTest) {
        if (!ModConfig.isBlockHidden(this.state)) {
            delegate.emitQuads(emitter, world, pos, state, random, cullTest);
        }
    }

    @Override
    public Object createGeometryKey(BlockRenderView world, BlockPos pos, BlockState state, Random random) {
        if (ModConfig.isBlockHidden(this.state)) {
            return GeometryKey.HIDDEN;
        }
        Object key = delegate.createGeometryKey(world, pos, state, random);
        return key == null ? null : new GeometryKey(key);
    }

    @Override
    public Sprite particleSprite() {
        return delegate.particleSprite();
    }

    @Override
    public Sprite particleSprite(BlockRenderView world, BlockPos pos, BlockState state) {
        return delegate.particleSprite(world, pos, state);
    }

    private record GeometryKey(Object delegate) {
        private static final GeometryKey HIDDEN = new GeometryKey(null);
    }
}
