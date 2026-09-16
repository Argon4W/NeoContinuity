package me.pepperbell.continuity.client.model;

import me.pepperbell.continuity.impl.client.ContinuityFeatureStatesImpl;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.function.IntFunction;
// import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;

public class ModelObjectsContainer implements IntFunction<BakedQuad[]> {
	public static final ThreadLocal<ModelObjectsContainer> THREAD_LOCAL = ThreadLocal.withInitial(ModelObjectsContainer::new);

	public final CtmBlockStateModel.CtmQuadTransform ctmQuadTransform = new CtmBlockStateModel.CtmQuadTransform();
	public final EmissiveBlockStateModel.EmissiveQuadTransform emissiveQuadTransform = new EmissiveBlockStateModel.EmissiveQuadTransform();
	public final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();

	public final ContinuityFeatureStatesImpl featureStates = new ContinuityFeatureStatesImpl();
	// public final MutableMesh mutableMesh = Renderer.get().mutableMesh();

	public static ModelObjectsContainer get() {
		return THREAD_LOCAL.get();
	}

	private BakedQuad[][] persistentQuadsStack = new BakedQuad[16][];

	private int depth = 0;
	private int cursor = 0;

	public BakedQuad[] getOrCreatePersistentQuads(int length) {
		int index = cursor ++;

		if (persistentQuadsStack.length <= index) {
			persistentQuadsStack = Arrays.copyOf(persistentQuadsStack, persistentQuadsStack.length * 2);
		}

		BakedQuad[] persistentQuads = persistentQuadsStack[index];

		if (persistentQuads == null || persistentQuads.length < length) {
			persistentQuads = persistentQuadsStack[index] = new BakedQuad[length];
		}

		return persistentQuads;
	}

	public void push() {
		if ((depth ++) == 0) {
			cursor = 0;
		}
	}

	public void pop() {
		depth --;
	}

	@Override
	public BakedQuad[] apply(int value) {
		return getOrCreatePersistentQuads(value);
	}
}
