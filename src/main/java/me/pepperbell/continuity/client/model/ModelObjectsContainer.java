package me.pepperbell.continuity.client.model;

import me.pepperbell.continuity.impl.client.ContinuityFeatureStatesImpl;
import net.minecraft.client.resources.model.geometry.BakedQuad;

import java.util.Arrays;
import java.util.List;
// import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;

public class ModelObjectsContainer {
	public static final ThreadLocal<ModelObjectsContainer> THREAD_LOCAL = ThreadLocal.withInitial(ModelObjectsContainer::new);

	public final CtmBlockStateModel.CtmQuadTransform ctmQuadTransform = new CtmBlockStateModel.CtmQuadTransform();
	public final EmissiveBlockStateModel.EmissiveQuadTransform emissiveQuadTransform = new EmissiveBlockStateModel.EmissiveQuadTransform();

	public final ContinuityFeatureStatesImpl featureStates = new ContinuityFeatureStatesImpl();
	// public final MutableMesh mutableMesh = Renderer.get().mutableMesh();
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
		if (quadListCache.length <= cacheIndex) {
			quadListCache = Arrays.copyOf(quadListCache, cacheIndex + 1);
		}

		if (quadListCache[cacheIndex] == null) {
			quadListCache[cacheIndex] = newQuadListCache();
		}

		return quadListCache[cacheIndex][index].update(root, fromIndex, toIndex);
	}

	public static ModelObjectsContainer get() {
		return THREAD_LOCAL.get();
	}
}
