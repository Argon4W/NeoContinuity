package me.pepperbell.continuity.client.mixin;

import me.pepperbell.continuity.client.util.SpriteCalculator;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {

	@Inject(method = "allChanged", at = @At("HEAD"))
	public void onAllChanged(CallbackInfo ci) {
		SpriteCalculator.clearCache();
	}
}
