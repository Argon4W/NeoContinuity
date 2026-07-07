package me.pepperbell.continuity.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.resources.model.geometry.BakedQuad;

import java.util.Arrays;
import java.util.List;

public class ModelThreadContext {

	private BakedQuad[][] quadsHolder = new BakedQuad[4][];

	private MutableSubList<BakedQuad>[][] quadListCache = new MutableSubList[4][];

	private MutableSubList<BakedQuad>[] newQuadListCache() {
		return new MutableSubList[] {
				new MutableSubList<BakedQuad>(),
				new MutableSubList<BakedQuad>(),
				new MutableSubList<BakedQuad>(),
				new MutableSubList<BakedQuad>(),
				new MutableSubList<BakedQuad>(),
				new MutableSubList<BakedQuad>(),
				new MutableSubList<BakedQuad>(),
		};
	}

	public BakedQuad[] getQuadsHolder(int cacheIndex, int length) {
		if (RenderSystem.isOnRenderThread()) {
			return new BakedQuad[length];
		}

		if (quadsHolder.length <= cacheIndex) {
			quadsHolder = Arrays.copyOf(quadsHolder, cacheIndex + 1);
		}

		if (quadsHolder[cacheIndex] == null) {
			quadsHolder[cacheIndex] = new BakedQuad[length];
		}

		if (quadsHolder[cacheIndex].length < length) {
			quadsHolder[cacheIndex] = Arrays.copyOf(quadsHolder[cacheIndex], length);
		}

		return quadsHolder[cacheIndex];
	}

	public List<BakedQuad> getQuadsList(List<BakedQuad> root, int cacheIndex, int index, int fromIndex, int toIndex) {
		if (RenderSystem.isOnRenderThread()) {
			return root.subList(fromIndex, toIndex);
		}

		if (quadListCache.length <= cacheIndex) {
			quadListCache = Arrays.copyOf(quadListCache, cacheIndex + 1);
		}

		if (quadListCache[cacheIndex] == null) {
			quadListCache[cacheIndex] = newQuadListCache();
		}

		return quadListCache[cacheIndex][index].update(root, fromIndex, toIndex);
	}
}
