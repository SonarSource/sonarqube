/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
