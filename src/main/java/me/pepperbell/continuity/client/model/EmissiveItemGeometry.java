package me.pepperbell.continuity.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.mixinterface.TextureAtlasSpriteExtension;
import me.pepperbell.continuity.client.util.ListPairView;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.model.MeshQuadCollection;
import net.fabricmc.fabric.api.client.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinderGetter;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;

/**
 * Bundles emissive geometry and other data for {@link CuboidItemModelWrapper}. Allows minimizing the memory footprint
 * of item emissive textures by necessitating addition of only a single field through Mixin.
 *
 * <p>Exactly one of {@link #baseAndEmissiveItemQuads} and {@link #emissiveMesh} is not null.
 */
public record EmissiveItemGeometry(@Nullable ItemQuads baseAndEmissiveItemQuads, @Nullable Mesh emissiveMesh, boolean emissiveAnimated) {
	public static final Object EMISSIVE_GEOMETRY_MARKER = new Object();

	@Nullable
	public static EmissiveItemGeometry of(QuadCollection quads, ItemQuads itemQuads, @Nullable SpriteFinderGetter spriteFinderGetter) {
		if (quads instanceof MeshQuadCollection meshQuadCollection) {
			if (spriteFinderGetter == null) {
				return null;
			}

			return of(meshQuadCollection.getMesh(), spriteFinderGetter);
		} else {
			return of(itemQuads);
		}
	}

	@Nullable
	public static EmissiveItemGeometry of(ItemQuads itemQuads) {
		if (itemQuads.isEmpty()) {
			return null;
		}

		List<BakedQuad> emissiveAll = null;
		List<BakedQuad> emissiveSolid = null;
		List<BakedQuad> emissiveTranslucent = null;
		QuadUtil.PackedUvContainer output = null;
		boolean emissiveAnimated = false;
		for (BakedQuad quad : itemQuads.all()) {
			BakedQuad.MaterialInfo materialInfo = quad.materialInfo();
			TextureAtlasSprite emissiveSprite = ((TextureAtlasSpriteExtension) materialInfo.sprite()).continuity$getEmissiveSprite();
			if (emissiveSprite != null) {
				if (emissiveAll == null) {
					output = new QuadUtil.PackedUvContainer();
					emissiveAll = new ArrayList<>();
				}

				QuadUtil.interpolate(quad, output, quad.materialInfo().sprite(), emissiveSprite);
				BakedQuad.MaterialInfo emissiveMaterialInfo = new BakedQuad.MaterialInfo(emissiveSprite, materialInfo.layer(), materialInfo.itemRenderType(), materialInfo.itemGlintRenderType(), materialInfo.itemGlintSpecialRenderType(), materialInfo.tintIndex(), Direction.UP, 15);
				BakedQuad emissiveQuad = new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), output.packedUV0, output.packedUV1, output.packedUV2, output.packedUV3, quad.direction(), emissiveMaterialInfo);
				emissiveAll.add(emissiveQuad);

				if (emissiveMaterialInfo.itemRenderType().hasBlending()) {
					if (emissiveTranslucent == null) {
						emissiveTranslucent = new ArrayList<>();
					}
					emissiveTranslucent.add(emissiveQuad);
				} else {
					if (emissiveSolid == null) {
						emissiveSolid = new ArrayList<>();
					}
					emissiveSolid.add(emissiveQuad);
				}

				emissiveAnimated |= emissiveSprite.contents().isAnimated();
			}
		}

		if (emissiveAll == null) {
			return null;
		}

		List<BakedQuad> combinedAll;
		List<BakedQuad> combinedSolid;
		List<BakedQuad> combinedTranslucent;
		// Use ListPairView to combine base quad list and emissive quad list instead of allocating new joined backing
		// array to minimize memory footprint.
		if (itemQuads.translucent().isEmpty() && emissiveTranslucent == null && itemQuads.all().equals(itemQuads.solid())) {
			combinedAll = new ListPairView<>(itemQuads.all(), List.copyOf(emissiveAll));
			combinedSolid = combinedAll;
			combinedTranslucent = List.of();
		} else if (itemQuads.solid().isEmpty() && emissiveSolid == null && itemQuads.all().equals(itemQuads.translucent())) {
			combinedAll = new ListPairView<>(itemQuads.all(), List.copyOf(emissiveAll));
			combinedSolid = List.of();
			combinedTranslucent = combinedAll;
		} else {
			combinedAll = new ListPairView<>(itemQuads.all(), List.copyOf(emissiveAll));
			combinedSolid = combine(itemQuads.solid(), emissiveSolid);
			combinedTranslucent = combine(itemQuads.translucent(), emissiveTranslucent);
		}
		return new EmissiveItemGeometry(new ItemQuads(combinedAll, combinedSolid, combinedTranslucent), null, emissiveAnimated);
	}

	private static List<BakedQuad> combine(List<BakedQuad> baseQuads, @Nullable List<BakedQuad> emissiveQuads) {
		if (emissiveQuads == null) {
			return baseQuads;
		}
		List<BakedQuad> copiedEmissiveQuads = List.copyOf(emissiveQuads);
		if (baseQuads.isEmpty()) {
			return copiedEmissiveQuads;
		}
		return new ListPairView<>(baseQuads, copiedEmissiveQuads);
	}

	@Nullable
	public static EmissiveItemGeometry of(MeshView mesh, SpriteFinderGetter spriteFinderGetter) {
		var quadConsumer = new Consumer<QuadView>() {
			@Nullable
			private MutableMesh emissiveMeshBuilder;
			@Nullable
			private QuadEmitter emitter;
			private @BakedQuad.MaterialFlags int flags = 0;

			@Override
			public void accept(QuadView quad) {
				TextureAtlasSprite sprite = spriteFinderGetter.spriteFinder(quad.atlas()).find(quad);
				TextureAtlasSprite emissiveSprite = ((TextureAtlasSpriteExtension) sprite).continuity$getEmissiveSprite();
				if (emissiveSprite != null) {
					if (emissiveMeshBuilder == null) {
						emissiveMeshBuilder = Renderer.get().mutableMesh();
						emitter = emissiveMeshBuilder.emitter();
					}

					emitter.copyFrom(quad);
					QuadUtil.interpolate(emitter, sprite, emissiveSprite);
					emitter.emissive(true).shadeDirectionOverride(Direction.UP);
					flags |= ModelHelper.computeMaterialFlags(emitter);
					emitter.emit();
				}
			}
		};
		mesh.forEach(quadConsumer);

		if (quadConsumer.emissiveMeshBuilder == null) {
			return null;
		}
		return new EmissiveItemGeometry(null, quadConsumer.emissiveMeshBuilder.immutableCopy(), (quadConsumer.flags & BakedQuad.FLAG_ANIMATED) != 0);
	}
}
