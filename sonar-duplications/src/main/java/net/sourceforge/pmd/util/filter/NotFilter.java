package net.sourceforge.pmd.util.filter;

/**
 * A logical NEGATION of a Filter.
 * 
 * @param <T>
 *            The underlying type on which the filter applies.
 */
public class NotFilter<T> extends AbstractDelegateFilter<T> {
	public NotFilter() {
		super();
	}

	public NotFilter(Filter<T> filter) {
		super(filter);
	}

	public boolean filter(T obj) {
		return !filter.filter(obj);
	}

	public String toString() {
		return "not (" + filter + ")";
	}
}
