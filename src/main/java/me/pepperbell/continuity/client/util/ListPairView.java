package me.pepperbell.continuity.client.util;

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Provides an immutable view over two lists as though they were joined contiguously. Assumes, but does not explicitly
 * check, that both lists implement {@link RandomAccess}.
 */
public class ListPairView<E> extends AbstractList<E> implements RandomAccess {
	protected final List<? extends E> a;
	protected final List<? extends E> b;

	/**
	 * @param a the first list.
	 * @param b the second list.
	 */
	public ListPairView(List<? extends E> a, List<? extends E> b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public E get(int index) {
		int aSize = a.size();
		Objects.checkIndex(index, aSize + b.size());
		if (index < aSize) {
			return a.get(index);
		} else {
			return b.get(index - aSize);
		}
	}

	@Override
	public int size() {
		return a.size() + b.size();
	}
}
