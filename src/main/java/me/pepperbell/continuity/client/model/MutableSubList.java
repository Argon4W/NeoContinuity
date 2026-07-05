package me.pepperbell.continuity.client.model;

import java.util.*;

public class MutableSubList<E> extends AbstractList<E> {
	private List<E> root;
	private int offset;
	protected int size;

	public MutableSubList() {
		this.root = null;
		this.offset = 0;
		this.size = 0;
	}

	public MutableSubList<E> update(List<E> root, int fromIndex, int toIndex) {
		this.root = root;
		this.offset = fromIndex;
		this.size = toIndex - fromIndex;

		return this;
	}

	@Override
	public E set(int index, E element) {
		return root.set(offset + index, element);
	}

	@Override
	public E get(int index) {
		return root.get(offset + index);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Iterator<E> iterator() {
		return listIterator();
	}

	public ListIterator<E> listIterator(int index) {
		return new ListIterator<>() {
			private final ListIterator<E> i = root.listIterator(offset + index);

			public boolean hasNext() {
				return nextIndex() < size;
			}

			public E next() {
				if (hasNext())
					return i.next();
				else
					throw new NoSuchElementException();
			}

			public boolean hasPrevious() {
				return previousIndex() >= 0;
			}

			public E previous() {
				if (hasPrevious())
					return i.previous();
				else
					throw new NoSuchElementException();
			}

			public int nextIndex() {
				return i.nextIndex() - offset;
			}

			public int previousIndex() {
				return i.previousIndex() - offset;
			}

			public void remove() {
				i.remove();
			}

			public void set(E e) {
				i.set(e);
			}

			public void add(E e) {
				i.add(e);
			}
		};
	}
}
