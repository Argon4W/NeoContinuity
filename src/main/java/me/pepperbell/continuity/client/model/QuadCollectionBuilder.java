package me.pepperbell.continuity.client.model;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

public class QuadCollectionBuilder {
	private final MutableQuad scratchQuad;
	private final BakedQuad[][] all;
	private final int[] cursors;
	private int direction;
	private int size;

	public QuadCollectionBuilder() {
		this.scratchQuad = new MutableQuad();
		this.all = new BakedQuad[7][];
		this.cursors = new int[7];
		this.direction = 6;
		this.size = 16;

		this.all[0] = new BakedQuad[this.size];
		this.all[1] = new BakedQuad[this.size];
		this.all[2] = new BakedQuad[this.size];
		this.all[3] = new BakedQuad[this.size];
		this.all[4] = new BakedQuad[this.size];
		this.all[5] = new BakedQuad[this.size];
		this.all[6] = new BakedQuad[this.size];
	}

	public void expand(int atLeast) {
		size = 1 << (32 - Integer.numberOfLeadingZeros(atLeast - 1));

		all[0] = Arrays.copyOf(all[0], size);
		all[1] = Arrays.copyOf(all[1], size);
		all[2] = Arrays.copyOf(all[2], size);
		all[3] = Arrays.copyOf(all[3], size);
		all[4] = Arrays.copyOf(all[4], size);
		all[5] = Arrays.copyOf(all[5], size);
		all[6] = Arrays.copyOf(all[6], size);
	}

	public void emitQuad() {
		addQuad(scratchQuad.toBakedQuad());
		scratchQuad.reset();
	}

	public void addQuad(BakedQuad quad) {
		if (cursors[direction] >= size) {
			expand(size * 2);
		}

		all[direction][cursors[direction] ++] = quad;
	}

	private void addAll(int direction, int count, BakedQuad[] quads) {
		if (count == 0) {
			return;
		}

		int required = cursors[direction] + count;

		if (required > size) {
			expand(required);
		}

		System.arraycopy(quads, 0, all[direction], cursors[direction], count);
		cursors[direction] += count;
	}

	public void addAll(QuadCollectionBuilder quadCollectionBuilder) {
		for (int i = 0; i < 7; i ++) {
			addAll(i, quadCollectionBuilder.cursors[i], quadCollectionBuilder.all[i]);
		}
	}

	public QuadCollection build(ModelObjectsContainer container) {
		int downCount = cursors[0];
		int upCount = cursors[1];
		int northCount = cursors[2];
		int southCount = cursors[3];
		int westCount = cursors[4];
		int eastCount = cursors[5];
		int unculledCount = cursors[6];
		int allCount = downCount + upCount + northCount + southCount + westCount + eastCount + unculledCount;

		if (allCount == 0) {
			return QuadCollection.EMPTY;
		}

		BakedQuad[] allQuads = container.getQuadsHolder(allCount);
		List<BakedQuad> allList = Arrays.asList(allQuads);

		int offset = 0;

		System.arraycopy(all[0], 0, allQuads, offset, downCount);
		List<BakedQuad> downList = container.getQuadsList(allList, 0, offset, offset + downCount);
		offset += downCount;

		System.arraycopy(all[1], 0, allQuads, offset, upCount);
		List<BakedQuad> upList = container.getQuadsList(allList, 1, offset, offset + upCount);
		offset += upCount;

		System.arraycopy(all[2], 0, allQuads, offset, northCount);
		List<BakedQuad> northList = container.getQuadsList(allList, 2, offset, offset + northCount);
		offset += northCount;

		System.arraycopy(all[3], 0, allQuads, offset, southCount);
		List<BakedQuad> southList = container.getQuadsList(allList, 3, offset, offset + southCount);
		offset += southCount;

		System.arraycopy(all[4], 0, allQuads, offset, westCount);
		List<BakedQuad> westList = container.getQuadsList(allList, 4, offset, offset + westCount);
		offset += westCount;

		System.arraycopy(all[5], 0, allQuads, offset, eastCount);
		List<BakedQuad> eastList = container.getQuadsList(allList, 5, offset, offset + eastCount);
		offset += eastCount;

		System.arraycopy(all[6], 0, allQuads, offset, unculledCount);
		List<BakedQuad> unculledList = container.getQuadsList(allList, 6, offset, offset + unculledCount);

		return new QuadCollection(
				allList,
				unculledList,
				northList,
				southList,
				eastList,
				westList,
				upList,
				downList
		);
	}

	public void setDirection(Direction direction) {
		this.direction = direction == null ? 6 : direction.ordinal();
	}

	public void reset() {
		for (int i = 0; i < 7; i ++) {
			Arrays.fill(all[i], 0, cursors[i], null);
		}

		Arrays.fill(cursors, 0);
	}

	public MutableQuad getScratchQuad(MutableQuad quad) {
		return quad.copyInto(scratchQuad);
	}

	public MutableQuad getScratchQuad(BakedQuad quad) {
		return scratchQuad.setFrom(quad);
	}

	public MutableQuad getScratchQuad() {
		return scratchQuad.reset();
	}
}
