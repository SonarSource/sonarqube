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
