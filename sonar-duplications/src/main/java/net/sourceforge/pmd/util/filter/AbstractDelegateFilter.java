package net.sourceforge.pmd.util.filter;

/**
 * A base class for Filters which implements behavior using delegation
 * to an underlying filter.
 * 
 * @param <T>
 *            The underlying type on which the filter applies.
 */
public abstract class AbstractDelegateFilter<T> implements Filter<T> {
	protected Filter<T> filter;

	public AbstractDelegateFilter() {
	}

	public AbstractDelegateFilter(Filter<T> filter) {
		this.filter = filter;
	}

	public Filter<T> getFilter() {
		return filter;
	}

	public void setFilter(Filter<T> filter) {
		this.filter = filter;
	}

	// Subclass should override to do something other the simply delegate.
	public boolean filter(T obj) {
		return filter.filter(obj);
	}

	// Subclass should override to do something other the simply delegate.
	public String toString() {
		return filter.toString();
	}
}
