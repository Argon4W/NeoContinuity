package me.pepperbell.continuity.impl.client;

import java.util.List;

// import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableMeshImpl;
import me.pepperbell.continuity.client.model.QuadCollectionBuilder;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataKey;
import me.pepperbell.continuity.api.client.ProcessingDataKeyRegistry;
import me.pepperbell.continuity.api.client.QuadProcessor;
// import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;

public class ProcessingContextImpl implements QuadProcessor.ProcessingContext {
	// protected final MutableMesh mutableMesh = Renderer.get().mutableMesh();
	protected QuadCollectionBuilder mutableMesh = new QuadCollectionBuilder();
	protected final Object[] processingData = new Object[ProcessingDataKeyRegistry.get().getRegisteredAmount()];

	@Override
	public QuadCollectionBuilder getExtraQuadEmitter() {
		return mutableMesh /* .emitter() */;

	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getData(ProcessingDataKey<T> key) {
		int index = key.getRawId();
		T data = (T) processingData[index];
		if (data == null) {
			data = key.getValueSupplier().get();
			processingData[index] = data;
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T getDataOrNull(ProcessingDataKey<T> key) {
		return (T) processingData[key.getRawId()];
	}

	/* public void outputTo(QuadEmitter emitter) {
		mutableMesh.outputTo(emitter);
	} */

	public void reset() {
		// mutableMesh.clear();
		mutableMesh.reset();
		resetData();
	}

	protected void resetData() {
		List<ProcessingDataKey<?>> allResettable = ProcessingDataKeyRegistryImpl.INSTANCE.getAllResettable();
		int amount = allResettable.size();
		for (int i = 0; i < amount; i++) {
			resetData(allResettable.get(i));
		}
	}

	protected <T> void resetData(ProcessingDataKey<T> key) {
		T value = getDataOrNull(key);
		if (value != null) {
			key.getValueResetAction().accept(value);
		}
	}
}
