package net.sourceforge.pmd.util.filter;

/**
 * A logical OR of a list of Filters. This implementation is short circuiting.
 * 
 * @param <T>
 *            The underlying type on which the filter applies.
 */
public class OrFilter<T> extends AbstractCompoundFilter<T> {

	public OrFilter() {
		super();
	}

	public OrFilter(Filter<T>... filters) {
		super(filters);
	}

	public boolean filter(T obj) {
		boolean match = false;
		for (Filter<T> filter : filters) {
			if (filter.filter(obj)) {
				match = true;
				break;
			}
		}
		return match;
	}

	@Override
	protected String getOperator() {
		return "or";
	}
}
