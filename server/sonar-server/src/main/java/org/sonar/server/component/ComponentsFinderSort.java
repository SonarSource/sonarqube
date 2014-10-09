/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.api.component.Component;

import java.util.Collection;
import java.util.List;

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

  private ComponentProcessor getComponentProcessor(String sort) {
    if (ComponentQuery.SORT_BY_NAME.equals(sort)) {
      return new NameSort();
    }
    throw new IllegalArgumentException("Cannot sort on field : " + sort);
  }

  abstract static class ComponentProcessor {
    abstract Function sortFieldFunction();

    abstract Ordering sortFieldOrdering(boolean ascending);

    final List<? extends Component> sort(Collection<? extends Component> components, boolean ascending) {
      Ordering<Component> ordering = sortFieldOrdering(ascending).onResultOf(sortFieldFunction());
      return ordering.immutableSortedCopy(components);
    }
  }

  abstract static class TextSort extends ComponentProcessor {
    @Override
    Function sortFieldFunction() {
      return new Function<Component, String>() {
        @Override
        public String apply(Component component) {
          return sortField(component);
        }
      };
    }

    abstract String sortField(Component component);

    @Override
    Ordering sortFieldOrdering(boolean ascending) {
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
