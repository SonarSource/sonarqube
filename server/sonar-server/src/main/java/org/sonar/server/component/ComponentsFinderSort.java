/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.component;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.List;
import org.sonar.api.component.Component;

/**
 * @since 3.7
 */
class ComponentsFinderSort {

  private Collection<? extends Component> components;
  private ComponentQuery query;

  public ComponentsFinderSort(Collection<? extends Component> components, ComponentQuery query) {
    this.components = components;
    this.query = query;
  }

  public Collection<? extends Component> sort() {
    String sort = query.sort();
    Boolean asc = query.asc();
    if (sort != null && asc != null) {
      return getComponentProcessor(sort).sort(components, asc);
    }
    return components;
  }

  private static ComponentProcessor getComponentProcessor(String sort) {
    if (ComponentQuery.SORT_BY_NAME.equals(sort)) {
      return new NameSort();
    }
    throw new IllegalArgumentException("Cannot sort on field : " + sort);
  }

  interface ComponentProcessor {
    Function sortFieldFunction();

    Ordering sortFieldOrdering(boolean ascending);

    default List<? extends Component> sort(Collection<? extends Component> components, boolean ascending) {
      Ordering<Component> ordering = sortFieldOrdering(ascending).onResultOf(sortFieldFunction());
      return ordering.immutableSortedCopy(components);
    }
  }

  abstract static class TextSort implements ComponentProcessor {
    @Override
    public Function sortFieldFunction() {
      return new Function<Component, String>() {
        @Override
        public String apply(Component component) {
          return sortField(component);
        }
      };
    }

    abstract String sortField(Component component);

    @Override
    public Ordering sortFieldOrdering(boolean ascending) {
      Ordering<String> ordering = Ordering.from(String.CASE_INSENSITIVE_ORDER).nullsLast();
      if (!ascending) {
        ordering = ordering.reverse();
      }
      return ordering;
    }
  }

  static class NameSort extends TextSort {
    @Override
    String sortField(Component component) {
      return component.name();
    }
  }

}
