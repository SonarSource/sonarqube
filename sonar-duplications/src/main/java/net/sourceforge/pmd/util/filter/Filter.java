package net.sourceforge.pmd.util.filter;

/**
 * A Filter interface, used for filtering arbitrary objects.
 * 
 * @param <T>
 *            The underlying type on which the filter applies.
 */
public interface Filter<T> {
	boolean filter(T obj);
}
