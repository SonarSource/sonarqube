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
package org.sonar.db.measure;

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

public class MeasureQuery {
  private final List<String> componentUuids;
  private final Collection<Integer> metricIds;
  private final Long personId;

  private MeasureQuery(Builder builder) {
    this(builder.componentUuids, builder.metricIds, builder.personId);
  }

  private MeasureQuery(List<String> componentUuids, @CheckForNull Collection<Integer> metricIds, @Nullable Long personId) {
    checkState(componentUuids != null, "Component UUIDs must be set");
    this.componentUuids = componentUuids;
    this.metricIds = metricIds;
    this.personId = personId;
  }

  public List<String> getComponentUuids() {
    return componentUuids;
  }

  @CheckForNull
  public Collection<Integer> getMetricIds() {
    return metricIds;
  }

  @CheckForNull
  public Long getPersonId() {
    return personId;
  }

  public boolean returnsEmpty() {
    return componentUuids.isEmpty()
      || (metricIds != null && metricIds.isEmpty());
  }

  public static MeasureQuery copyWithSubsetOfComponentUuids(MeasureQuery query, List<String> componentUuids) {
    return new MeasureQuery(componentUuids, query.metricIds, query.personId);
  }

  public static final class Builder {
    private List<String> componentUuids;
    private Collection<Integer> metricIds;
    private Long personId;

    public Builder setComponentUuids(List<String> componentUuids) {
      this.componentUuids = componentUuids;
      return this;
    }

    /**
     * All the measures are returned if parameter is {@code null}.
     */
    public Builder setMetricIds(@Nullable Collection<Integer> metricIds) {
      this.metricIds = metricIds;
      return this;
    }

    public Builder setPersonId(@Nullable Long l) {
      this.personId = l;
      return this;
    }

    public MeasureQuery build() {
      return new MeasureQuery(this);
    }
  }
}
