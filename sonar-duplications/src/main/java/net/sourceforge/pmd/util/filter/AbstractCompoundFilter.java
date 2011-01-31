/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package net.sourceforge.pmd.util.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * A base class for Filters which implements behavior using a List of other
 * Filters.
 * 
 * @param <T>
 *            The underlying type on which the filter applies.
 */
public abstract class AbstractCompoundFilter<T> implements Filter<T> {

	protected List<Filter<T>> filters;

	public AbstractCompoundFilter() {
		filters = new ArrayList<Filter<T>>(2);
	}

	public AbstractCompoundFilter(Filter<T>... filters) {
		this.filters = new ArrayList<Filter<T>>(filters.length);
		for (Filter<T> filter : filters) {
			this.filters.add(filter);
		}
	}

	public List<Filter<T>> getFilters() {
		return filters;
	}

	public void setFilters(List<Filter<T>> filters) {
		this.filters = filters;
	}

	public void addFilter(Filter<T> filter) {
		filters.add(filter);
	}

	protected abstract String getOperator();

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		for (int i = 0; i < filters.size(); i++) {
			if (i > 0) {
				builder.append(" ");
				builder.append(getOperator());
				builder.append(" ");
			}
			builder.append(filters.get(i));
		}
		builder.append(")");
		return builder.toString();
	}
}
