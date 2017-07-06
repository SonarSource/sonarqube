/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.db.event;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class EventQuery {
  private final List<ProjectAndDate> projectAndDates;

  public EventQuery(Builder builder) {
    projectAndDates = IntStream.range(0, builder.componentUuids.size())
      .mapToObj(i -> new ProjectAndDate(builder.componentUuids.get(i), builder.from.get(i)))
      .collect(MoreCollectors.toList(builder.componentUuids.size()));
  }

  public List<ProjectAndDate> getProjectAndDates() {
    return projectAndDates;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<Long> from;
    private List<String> componentUuids;

    private Builder() {
      // enforce static factory method
    }

    public Builder setFrom(List<Long> from) {
      this.from = from;
      return this;
    }

    public Builder setComponentUuids(List<String> componentUuids) {
      this.componentUuids = componentUuids;
      return this;
    }

    public EventQuery build() {
      requireNonNull(componentUuids, "A component uuid must be provided");
      requireNonNull(from, "A from timestamp must be provided");
      checkArgument(componentUuids.size() == from.size(), "The number of components (%s) and from dates (%s) must be the same.",
        String.valueOf(componentUuids.size()),
        String.valueOf(from.size()));
      return new EventQuery(this);
    }
  }

  public static class ProjectAndDate implements Comparable<ProjectAndDate> {
    private final String componentUuid;
    private final long from;

    public ProjectAndDate(String componentUuid, long from) {
      this.componentUuid = requireNonNull(componentUuid);
      this.from = from;
    }

    @Override
    public int compareTo(ProjectAndDate other) {
      if (this == other) {
        return 0;
      }

      int c = componentUuid.compareTo(other.componentUuid);
      if (c == 0) {
        c = Long.compare(from, other.from);
      }

      return c;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ProjectAndDate other = (ProjectAndDate) o;
      return componentUuid.equals(other.componentUuid)
      && from == other.from;
    }

    @Override
    public int hashCode() {
      return Objects.hash(componentUuid, from);
    }
  }
}
