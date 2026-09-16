package me.pepperbell.continuity.client.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.model.EmissiveItemGeometry;
import me.pepperbell.continuity.client.resource.CuboidItemModelWrapperInitContext;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinderGetter;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

// Set priority to higher than FRAPI's mixin to this class to ensure emissive quads are rendered after FRAPI quads
@Mixin(value = CuboidItemModelWrapper.class, priority = 2000)
abstract class CuboidItemModelWrapperMixin {
	@Shadow
	@Final
	private ItemQuads itemQuads;

	@Unique
	@Nullable
	private EmissiveItemGeometry emissiveGeometry;

	@Inject(method = "<init>(Ljava/util/List;Lnet/minecraft/client/resources/model/geometry/QuadCollection;Lnet/minecraft/client/renderer/item/ModelRenderProperties;Lorg/joml/Matrix4fc;)V", at = @At("RETURN"))
	private void onReturnInit(List<ItemTintSource> tints, QuadCollection quads, ModelRenderProperties properties, Matrix4fc transformation, CallbackInfo ci) {
		SpriteFinderGetter spriteFinderGetter = CuboidItemModelWrapperInitContext.SPRITE_FINDER_GETTER.get();
		emissiveGeometry = EmissiveItemGeometry.of(quads, itemQuads, spriteFinderGetter);
	}

	@Inject(method = "update(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/item/ItemModelResolver;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/ItemOwner;I)V", at = @At("RETURN"))
	private void onReturnUpdate(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed, CallbackInfo ci, @Local ItemStackRenderState.LayerRenderState layer) {
		if (emissiveGeometry != null && ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			if (emissiveGeometry.baseAndEmissiveItemQuads() != null) {
				layer.setQuads(emissiveGeometry.baseAndEmissiveItemQuads());
			} else if (emissiveGeometry.emissiveMesh() != null) {
				emissiveGeometry.emissiveMesh().outputTo(layer.emitter());
			}
			output.appendModelIdentityElement(EmissiveItemGeometry.EMISSIVE_GEOMETRY_MARKER);
			if (emissiveGeometry.emissiveAnimated()) {
				output.setAnimated();
			}
		}
	}
}
