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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.component.Component;
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.api.utils.Paging.forPageIndex;

/**
 * @since 3.7
 */
public class DefaultComponentFinder {

  private static final Logger LOG = Loggers.get(DefaultComponentFinder.class);

  public DefaultComponentQueryResult find(ComponentQuery query, List<Component> allComponents) {
    LOG.debug("ComponentQuery : {}", query);
    Collection<Component> foundComponents = search(query, allComponents);

    // Sort components
    Collection<? extends Component> sortedComponents = new ComponentsFinderSort(foundComponents, query).sort();

    // Apply pagination if needed
    if (ComponentQuery.NO_PAGINATION == query.pageSize()) {
      return new DefaultComponentQueryResult(sortedComponents).setQuery(query);
    } else {
      Paging paging = forPageIndex(query.pageIndex()).withPageSize(query.pageSize()).andTotal(foundComponents.size());
      Collection<? extends Component> pagedComponents = pagedComponents(sortedComponents, paging);
      return new DefaultComponentQueryResult(pagedComponents).setPaging(paging).setQuery(query);
    }
  }

  private static Collection<Component> search(ComponentQuery query, List<? extends Component> allComponents) {
    return newArrayList(Iterables.filter(allComponents, new MatchQuery(query)));
  }

  abstract static class Filter {

    abstract String field(Component component);

    final boolean accept(Component component, Collection<String> collections) {
      if (!collections.isEmpty()) {
        for (String item : collections) {
          if (field(component).toLowerCase().contains(item.toLowerCase())) {
            return true;
          }
        }
        return false;
      }
      return true;
    }
  }

  static class NameFilter extends Filter {
    @Override
    String field(Component component) {
      return component.name();
    }
  }

  static class KeyFilter extends Filter {
    @Override
    String field(Component component) {
      return component.key();
    }
  }

  private static Collection<? extends Component> pagedComponents(Collection<? extends Component> components, Paging paging) {
    Set<Component> pagedComponents = Sets.newLinkedHashSet();
    int index = 0;
    for (Component component : components) {
      if (index >= paging.offset() && pagedComponents.size() < paging.pageSize()) {
        pagedComponents.add(component);
      } else if (pagedComponents.size() >= paging.pageSize()) {
        break;
      }
      index++;
    }
    return pagedComponents;
  }

  private static class MatchQuery implements Predicate<Component> {
    private final ComponentQuery query;

    public MatchQuery(ComponentQuery query) {
      this.query = query;
    }

    @Override
    public boolean apply(@Nonnull Component component) {
      return new KeyFilter().accept(component, query.keys()) &&
        new NameFilter().accept(component, query.names());
    }
  }

}
