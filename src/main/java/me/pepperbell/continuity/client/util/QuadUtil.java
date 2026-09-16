package me.pepperbell.continuity.client.util;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

public final class QuadUtil {
	public static void interpolate(MutableQuadView quad, TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite) {
		float oldMinU = oldSprite.getU0();
		float oldMinV = oldSprite.getV0();
		float newMinU = newSprite.getU0();
		float newMinV = newSprite.getV0();
		float uFactor = (newSprite.getU1() - newMinU) / (oldSprite.getU1() - oldMinU);
		float vFactor = (newSprite.getV1() - newMinV) / (oldSprite.getV1() - oldMinV);
		for (int i = 0; i < 4; i++) {
			quad.uv(i,
					newMinU + (quad.u(i) - oldMinU) * uFactor,
					newMinV + (quad.v(i) - oldMinV) * vFactor
			);
		}
		quad.animated(newSprite.contents().isAnimated());
	}

	public static void interpolate(BakedQuad quad, PackedUvContainer output, TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite) {
		float oldMinU = oldSprite.getU0();
		float oldMinV = oldSprite.getV0();
		float newMinU = newSprite.getU0();
		float newMinV = newSprite.getV0();
		float uFactor = (newSprite.getU1() - newMinU) / (oldSprite.getU1() - oldMinU);
		float vFactor = (newSprite.getV1() - newMinV) / (oldSprite.getV1() - oldMinV);
		for (int i = 0; i < 4; i++) {
			long packedUv = quad.packedUV(i);
			output.packedUV(i, UVPair.pack(
					newMinU + (UVPair.unpackU(packedUv) - oldMinU) * uFactor,
					newMinV + (UVPair.unpackV(packedUv) - oldMinV) * vFactor
			));
		}
	}

	public static void emitOverlayQuad(QuadEmitter emitter, Direction face, TextureAtlasSprite sprite, int color, ChunkSectionLayer chunkLayer, TriState ao) {
		emitter.square(face, 0, 0, 1, 1, 0);
		emitter.color(color, color, color, color);
		emitter.uv(0, sprite.getU0(), sprite.getV0());
		emitter.uv(1, sprite.getU0(), sprite.getV1());
		emitter.uv(2, sprite.getU1(), sprite.getV1());
		emitter.uv(3, sprite.getU1(), sprite.getV0());
		emitter.atlas(QuadAtlas.BLOCK);
		emitter.animated(sprite.contents().isAnimated());
		emitter.chunkLayer(chunkLayer);
		emitter.itemRenderType(chunkLayer == ChunkSectionLayer.TRANSLUCENT ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet());
		emitter.ambientOcclusion(ao);
		emitter.emit();
	}

	public static boolean isQuadUnitSquare(QuadView quad) {
		int indexA;
		int indexB;
		switch (quad.lightFace().getAxis()) {
			case X:
				indexA = 1;
				indexB = 2;
				break;
			case Y:
				indexA = 0;
				indexB = 2;
				break;
			case Z:
				indexA = 1;
				indexB = 0;
				break;
			default:
				return false;
		}

		for (int i = 0; i < 4; i++) {
			float a = quad.posByIndex(i, indexA);
			if ((a >= 0.0001f || a <= -0.0001f) && (a >= 1.0001f || a <= 0.9999f)) {
				return false;
			}
			float b = quad.posByIndex(i, indexB);
			if ((b >= 0.0001f || b <= -0.0001f) && (b >= 1.0001f || b <= 0.9999f)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns an int in range [0, 7] representing the texture orientation of the given quad relative to the world.
	 *
	 * <ul>
	 *     <li>0 - 0 degree counterclockwise rotation, counterclockwise UV winding order</li>
	 *     <li>1 - 90 degree counterclockwise rotation, counterclockwise UV winding order</li>
	 *     <li>2 - 180 degree counterclockwise rotation, counterclockwise UV winding order</li>
	 *     <li>3 - 270 degree counterclockwise rotation, counterclockwise UV winding order</li>
	 *     <li>4 - 0 degree counterclockwise rotation, clockwise UV winding order</li>
	 *     <li>5 - 90 degree counterclockwise rotation, clockwise UV winding order</li>
	 *     <li>6 - 180 degree counterclockwise rotation, clockwise UV winding order</li>
	 *     <li>7 - 270 degree counterclockwise rotation, clockwise UV winding order</li>
	 * </ul>
	 */
	public static int getTextureOrientation(QuadView quad) {
		// Texture matrix
		float tm00 = quad.u(3) - quad.u(1);
		float tm01 = quad.v(3) - quad.v(1);
		float tm10 = quad.u(2) - quad.u(0);
		float tm11 = quad.v(2) - quad.v(0);
		// Determinant of texture matrix; also cross product of its column vectors
		float determinant = tm00 * tm11 - tm10 * tm01;
		if (determinant == 0) {
			return 0;
		}
		float s = 1 / determinant;
		// Second column of inverse texture matrix
		float itm10 = -tm10 * s;
		float itm11 = tm00 * s;

		int xAxis;
		int xAxisSign;
		int yAxis;
		int yAxisSign;
		switch (quad.lightFace()) {
			case DOWN -> {
				xAxis = 0; // +X
				xAxisSign = 1;
				yAxis = 2; // +Z
				yAxisSign = 1;
			}
			case UP -> {
				xAxis = 0; // +X
				xAxisSign = 1;
				yAxis = 2; // -Z
				yAxisSign = -1;
			}
			case NORTH -> {
				xAxis = 0; // -X
				xAxisSign = -1;
				yAxis = 1; // +Y
				yAxisSign = 1;
			}
			case SOUTH -> {
				xAxis = 0; // +X
				xAxisSign = 1;
				yAxis = 1; // +Y
				yAxisSign = 1;
			}
			case WEST -> {
				xAxis = 2; // +Z
				xAxisSign = 1;
				yAxis = 1; // +Y
				yAxisSign = 1;
			}
			case EAST -> {
				xAxis = 2; // -Z
				xAxisSign = -1;
				yAxis = 1; // +Y
				yAxisSign = 1;
			}
			default -> {
				return 0;
			}
		}
		// Position matrix
		float pm00 = quad.posByIndex(3, xAxis) - quad.posByIndex(1, xAxis);
		float pm01 = quad.posByIndex(3, yAxis) - quad.posByIndex(1, yAxis);
		float pm10 = quad.posByIndex(2, xAxis) - quad.posByIndex(0, xAxis);
		float pm11 = quad.posByIndex(2, yAxis) - quad.posByIndex(0, yAxis);

		// Texture up vector in projected world space
		// Computed as (position matrix * inverse texture matrix * [0; -1]); [0; -1] is the texture up vector in texture space
		// Axis signs should be multiplied into position matrix values, but multiplying here instead saves 2 multiplications
		float x = -(pm00 * itm10 + pm10 * itm11) * xAxisSign;
		float y = -(pm01 * itm10 + pm11 * itm11) * yAxisSign;

		// Clamp vector to nearest axis-aligned direction
		// up/+y -> 0, left/-x -> 1, down/-y -> 2, right/+x -> 3
		// Add 4 if the UV winding order is clockwise
		return (Math.abs(y) >= Math.abs(x) ? (y > 0 ? 0 : 2) : (x > 0 ? 3 : 1)) + (determinant < 0 ? 4 : 0);
	}

	public static class PackedUvContainer {
		public long packedUV0;
		public long packedUV1;
		public long packedUV2;
		public long packedUV3;

		public void packedUV(int index, long packedUV) {
			switch (index) {
				case 0 -> packedUV0 = packedUV;
				case 1 -> packedUV1 = packedUV;
				case 2 -> packedUV2 = packedUV;
				case 3 -> packedUV3 = packedUV;
				default -> throw new IndexOutOfBoundsException(index);
			}
		}
	}
}
