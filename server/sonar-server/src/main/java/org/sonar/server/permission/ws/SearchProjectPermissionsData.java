/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.permission.ws;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import java.util.List;
import java.util.Set;
import org.sonar.api.utils.Paging;
import org.sonar.db.component.ComponentDto;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableTable.copyOf;

class SearchProjectPermissionsData {
  private final List<ComponentDto> rootComponents;
  private final Paging paging;
  private final Table<Long, String, Integer> userCountByProjectIdAndPermission;
  private final Table<Long, String, Integer> groupCountByProjectIdAndPermission;

  private SearchProjectPermissionsData(Builder builder) {
    this.rootComponents = copyOf(builder.projects);
    this.paging = builder.paging;
    this.userCountByProjectIdAndPermission = copyOf(builder.userCountByProjectIdAndPermission);
    this.groupCountByProjectIdAndPermission = copyOf(builder.groupCountByProjectIdAndPermission);
  }

  static Builder newBuilder() {
    return new Builder();
  }

  List<ComponentDto> rootComponents() {
    return rootComponents;
  }

  Paging paging() {
    return paging;
  }

  int userCount(long rootComponentId, String permission) {
    return firstNonNull(userCountByProjectIdAndPermission.get(rootComponentId, permission), 0);
  }

  int groupCount(long rootComponentId, String permission) {
    return firstNonNull(groupCountByProjectIdAndPermission.get(rootComponentId, permission), 0);
  }

  Set<String> permissions(long rootComponentId) {
    return FluentIterable.from(
      Iterables.concat(
        userCountByProjectIdAndPermission.row(rootComponentId).keySet(),
        groupCountByProjectIdAndPermission.row(rootComponentId).keySet()))
      .toSortedSet(Ordering.natural());
  }

  static class Builder {
    private List<ComponentDto> projects;
    private Paging paging;
    private Table<Long, String, Integer> userCountByProjectIdAndPermission;
    private Table<Long, String, Integer> groupCountByProjectIdAndPermission;

    private Builder() {
      // prevents instantiation outside main class
    }

    SearchProjectPermissionsData build() {
      checkState(projects != null);
      checkState(userCountByProjectIdAndPermission != null);
      checkState(groupCountByProjectIdAndPermission != null);

      return new SearchProjectPermissionsData(this);
    }

    Builder rootComponents(List<ComponentDto> projects) {
      this.projects = projects;
      return this;
    }

    Builder paging(Paging paging) {
      this.paging = paging;
      return this;
    }

    Builder userCountByProjectIdAndPermission(Table<Long, String, Integer> userCountByProjectIdAndPermission) {
      this.userCountByProjectIdAndPermission = userCountByProjectIdAndPermission;
      return this;
    }

    Builder groupCountByProjectIdAndPermission(Table<Long, String, Integer> groupCountByProjectIdAndPermission) {
      this.groupCountByProjectIdAndPermission = groupCountByProjectIdAndPermission;
      return this;
    }
  }
}
