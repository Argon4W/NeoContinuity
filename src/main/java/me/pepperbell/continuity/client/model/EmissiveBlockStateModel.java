package me.pepperbell.continuity.client.model;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.client.util.RenderUtil;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.util.QuadUtil;
// import me.pepperbell.continuity.client.util.RenderUtil;
// import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
// import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class EmissiveBlockStateModel extends /* WrapperBlockStateModel */ DelegateBlockStateModel {
	public EmissiveBlockStateModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	// public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			// super.emitQuads(emitter, level, pos, state, random, cullTest);
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			// super.emitQuads(emitter, level, pos, state, random, cullTest);
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		EmissiveQuadTransform quadTransform = container.emissiveQuadTransform;
		if (quadTransform.isActive()) {
			// super.emitQuads(emitter, level, pos, state, random, cullTest);
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		/* MutableMesh mutableMesh = container.mutableMesh;
		quadTransform.prepare(mutableMesh.emitter(), state, cullTest);

		emitter.pushTransform(quadTransform);
		super.emitQuads(emitter, level, pos, state, random, cullTest);
		emitter.popTransform();

		mutableMesh.outputTo(emitter);
		mutableMesh.clear(); */

		quadTransform.prepare(/* mutableMesh.emitter(), */ state);

		super.collectParts(level, pos, state, random, parts);

		QuadCollectionBuilder emitter = quadTransform.extraQuadEmitter;

		for (int i = 0, s1 = parts.size(); i < s1; i ++) {
			BlockStateModelPart part = parts.get(i);

			emitter.reset();

			for (int j = 0, s2 = RenderUtil.DIRECTIONS.length; j < s2; j ++) {
				Direction direction = RenderUtil.DIRECTIONS[j];

				emitter.setDirection(direction);

				List<BakedQuad> quads = part.getQuads(direction);

				for (int k = 0, s3 = quads.size(); k < s3; k ++) {
					quadTransform.transform(quads.get(k));
				}
			}

			emitter.setDirection(null);

			List<BakedQuad> quads = part.getQuads(null);

			for (int k = 0, s3 = quads.size(); k < s3; k ++) {
				quadTransform.transform(quads.get(k));
			}

			parts.add(new SimpleModelWrapper(emitter.build(quadTransform, i), part.useAmbientOcclusion(), part.particleMaterial()));
		}

		quadTransform.reset();
	}

	@Override
	@Nullable
	public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			return super.createGeometryKey(level, pos, state, random);
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			return super.createGeometryKey(level, pos, state, random);
		}

		EmissiveQuadTransform quadTransform = container.emissiveQuadTransform;
		if (quadTransform.isActive()) {
			return super.createGeometryKey(level, pos, state, random);
		}

		Object subkey = super.createGeometryKey(level, pos, state, random);
		if (subkey == null) {
			return null;
		}

		record Key(Object subkey) {
		}

		return new Key(subkey);
	}

	protected static class EmissiveQuadTransform /* implements QuadTransform */ extends ModelThreadContext {
		protected QuadCollectionBuilder extraQuadEmitter = new QuadCollectionBuilder();
		protected BlockState state;
		// protected Predicate<@Nullable Direction> cullTest;

		protected boolean active;

		// @Override
		public /* boolean */ void transform(/* MutableQuadView */ BakedQuad quad) {
			/* if (cullTest.test(quad.cullFace())) {
				return false;
			} */

			TextureAtlasSprite sprite = /* RenderUtil.getSpriteFinder().find(quad) */ quad.materialInfo().sprite();
			TextureAtlasSprite emissiveSprite = EmissiveSpriteApi.get().getEmissiveSprite(sprite);
			if (emissiveSprite != null) {
				MutableQuad toEmit = extraQuadEmitter.getScratchQuad(quad);

				// emitter.copyFrom(quad);
				// emitter.emissive(true).diffuseShade(false).ambientOcclusion(TriState.FALSE);

				toEmit.setLightEmission(15);
				toEmit.setShade(false);
				toEmit.setAmbientOcclusion(false);

				ChunkSectionLayer renderLayer = quad.materialInfo().layer();
				if (renderLayer == ChunkSectionLayer.SOLID) {
					// emitter.chunkLayer(ChunkSectionLayer.CUTOUT);
					toEmit.setSprite(toEmit.sprite(), ChunkSectionLayer.CUTOUT, toEmit.itemRenderType());
				}

				QuadUtil.interpolate(/* emitter */ toEmit, sprite, emissiveSprite);
				// emitter.emit();
				extraQuadEmitter.emitQuad();
			}
			// return true;
		}

		public boolean isActive() {
			return active;
		}

		public void prepare(/* QuadEmitter emitter, */ BlockState state /* , Predicate<@Nullable Direction> cullTest */) {
			// this.emitter = emitter;
			this.state = state;
			// this.cullTest = cullTest;

			active = true;
		}

		public void reset() {
			// emitter = null;
			state = null;
			// cullTest = null;

			active = false;
		}
	}
}
