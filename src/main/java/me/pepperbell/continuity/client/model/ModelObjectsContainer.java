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
	private BakedQuad[] quadsHolder = new BakedQuad[8];

	private final MutableSubList<BakedQuad>[] quadListCache = new MutableSubList[] {
			new MutableSubList<BakedQuad>(),
			new MutableSubList<BakedQuad>(),
			new MutableSubList<BakedQuad>(),
			new MutableSubList<BakedQuad>(),
			new MutableSubList<BakedQuad>(),
			new MutableSubList<BakedQuad>(),
			new MutableSubList<BakedQuad>(),
	};

	public BakedQuad[] getQuadsHolder(int length) {
		if (quadsHolder.length < length) {
			quadsHolder = Arrays.copyOf(quadsHolder, length);
		}

		return quadsHolder;
	}

	public List<BakedQuad> getQuadsList(List<BakedQuad> root, int index, int fromIndex, int toIndex) {
		return quadListCache[index].update(root, fromIndex, toIndex);
	}

	public static ModelObjectsContainer get() {
		return THREAD_LOCAL.get();
	}
}
