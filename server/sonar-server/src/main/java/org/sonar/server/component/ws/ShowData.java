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
package org.sonar.server.component.ws;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentDtoFunctions;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotDtoFunctions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

class ShowData {
  private final List<ComponentDto> components;

  private ShowData(List<ComponentDto> components) {
    this.components = components;
  }

  static Builder builder(SnapshotDto snapshot) {
    return new Builder(snapshot);
  }

  List<ComponentDto> getComponents() {
    return components;
  }

  static class Builder {
    private Ordering<SnapshotDto> snapshotOrdering;
    private List<Long> orderedSnapshotIds;
    private List<Long> orderedComponentIds;

    private Builder(SnapshotDto snapshot) {
      List<String> orderedSnapshotIdsAsString = snapshot.getPath() == null ? Collections.<String>emptyList() : Splitter.on(".").omitEmptyStrings().splitToList(snapshot.getPath());
      orderedSnapshotIds = Lists.transform(orderedSnapshotIdsAsString, StringToLongFunction.INSTANCE);
      snapshotOrdering = Ordering
        .explicit(orderedSnapshotIds)
        .onResultOf(SnapshotDtoFunctions.toId())
        .reverse();
    }

    Builder withAncestorsSnapshots(List<SnapshotDto> ancestorsSnapshots) {
      checkNotNull(snapshotOrdering, "Snapshot must be set before the ancestors");
      checkState(orderedSnapshotIds.size() == ancestorsSnapshots.size(), "Missing ancestor");

      orderedComponentIds = Lists.transform(
        snapshotOrdering.immutableSortedCopy(ancestorsSnapshots),
        SnapshotDtoFunctions.toComponentId());

      return this;
    }

    ShowData andAncestorComponents(List<ComponentDto> ancestorComponents) {
      checkNotNull(orderedComponentIds, "Snapshot ancestors must be set before the component ancestors");
      checkState(orderedComponentIds.size() == ancestorComponents.size(), "Missing ancestor");

      return new ShowData(Ordering
        .explicit(orderedComponentIds)
        .onResultOf(ComponentDtoFunctions.toId())
        .immutableSortedCopy(ancestorComponents));
    }

    List<Long> getOrderedSnapshotIds() {
      return orderedSnapshotIds;
    }

    List<Long> getOrderedComponentIds() {
      return orderedComponentIds;
    }
  }

  private enum StringToLongFunction implements Function<String, Long> {
    INSTANCE;

    @Override
    public Long apply(@Nonnull String input) {
      return Long.parseLong(input);
    }
  }
}
