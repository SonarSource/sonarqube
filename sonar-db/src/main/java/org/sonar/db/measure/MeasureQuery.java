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

import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class MeasureQuery {
  private final String analysisUuid;

  @CheckForNull
  private final List<String> projectUuids;

  @CheckForNull
  private final List<String> componentUuids;

  @CheckForNull
  private final List<Integer> metricIds;

  @CheckForNull
  private final List<String> metricKeys;

  @CheckForNull
  private final Long personId;

  private MeasureQuery(Builder builder) {
    this(builder.analysisUuid, builder.projectUuids, builder.componentUuids, builder.metricIds, builder.metricKeys, builder.personId);
  }

  private MeasureQuery(@Nullable String analysisUuid,
    @Nullable List<String> projectUuids,
    @Nullable List<String> componentUuids,
    @Nullable List<Integer> metricIds,
    @Nullable List<String> metricKeys,
    @Nullable Long personId) {
    checkArgument(metricIds == null || metricKeys == null, "Metric IDs and keys must not be set both");
    checkArgument(projectUuids != null || componentUuids != null, "At least one filter on component UUID is expected");
    checkArgument(componentUuids == null || componentUuids.size() == 1 || (projectUuids != null && projectUuids.size() == 1),
      "Component UUIDs can only be used when a single project UUID is set");

    this.analysisUuid = analysisUuid;
    this.projectUuids = projectUuids;
    this.componentUuids = componentUuids;
    this.metricIds = metricIds;
    this.metricKeys = metricKeys;
    this.personId = personId;
  }

  public String getAnalysisUuid() {
    return analysisUuid;
  }

  @CheckForNull
  public List<String> getProjectUuids() {
    return projectUuids;
  }

  @CheckForNull
  public String getProjectUuid() {
    return isOnComponents() ? projectUuids.get(0) : null;
  }

  @CheckForNull
  public List<String> getComponentUuids() {
    return componentUuids;
  }

  @CheckForNull
  public String getComponentUuid() {
    return isOnSingleComponent() ? componentUuids.get(0) : null;
  }

  @CheckForNull
  public List<Integer> getMetricIds() {
    return metricIds;
  }

  @CheckForNull
  public List<String> getMetricKeys() {
    return metricKeys;
  }

  @CheckForNull
  public Long getPersonId() {
    return personId;
  }

  public boolean returnsEmpty() {
    return (projectUuids != null && projectUuids.isEmpty())
      || (componentUuids != null && componentUuids.isEmpty())
      || (metricIds != null && metricIds.isEmpty())
      || (metricKeys != null && metricKeys.isEmpty());
  }

  public boolean isOnProjects() {
    return projectUuids != null && componentUuids == null;
  }

  public boolean isOnComponents() {
    return projectUuids != null && projectUuids.size() == 1 && componentUuids != null;
  }

  public boolean isOnSingleComponent() {
    return projectUuids == null && componentUuids != null && componentUuids.size() == 1;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MeasureQuery that = (MeasureQuery) o;
    return analysisUuid.equals(that.analysisUuid) &&
      Objects.equals(projectUuids, that.projectUuids) &&
      Objects.equals(componentUuids, that.componentUuids) &&
      Objects.equals(metricIds, that.metricIds) &&
      Objects.equals(metricKeys, that.metricKeys) &&
      Objects.equals(personId, that.personId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(analysisUuid, componentUuids, metricIds, metricKeys, personId);
  }

  public static Builder builder() {
    return new Builder();
  }

  static MeasureQuery copyWithSubsetOfProjectUuids(MeasureQuery query, List<String> projectUuids) {
    return new MeasureQuery(query.analysisUuid, projectUuids, query.componentUuids, query.metricIds, query.metricKeys, query.personId);
  }

  static MeasureQuery copyWithSubsetOfComponentUuids(MeasureQuery query, List<String> componentUuids) {
    return new MeasureQuery(query.analysisUuid, query.projectUuids, componentUuids, query.metricIds, query.metricKeys, query.personId);
  }

  public static final class Builder {
    private String analysisUuid;
    private List<String> projectUuids;
    private List<String> componentUuids;
    private List<Integer> metricIds;
    private List<String> metricKeys;
    private Long personId;

    private Builder() {
      // see MeasureQuery#builder()
    }

    public Builder setAnalysisUuid(String analysisUuid) {
      this.analysisUuid = analysisUuid;
      return this;
    }

    /**
     * List of projects
     */
    public Builder setProjectUuids(@Nullable List<String> projectUuids) {
      this.projectUuids = projectUuids;
      return this;
    }

    /**
     * List of components of a project
     */
    public Builder setComponentUuids(String projectUuid, List<String> componentUuids) {
      setProjectUuids(singletonList(requireNonNull(projectUuid)));
      this.componentUuids = componentUuids;
      return this;
    }

    /**
     * Single component
     */
    public Builder setComponentUuid(String componentUuid) {
      this.componentUuids = singletonList(componentUuid);
      return this;
    }

    /**
     * All the measures are returned if parameter is {@code null}.
     */
    public Builder setMetricIds(@Nullable List<Integer> metricIds) {
      this.metricIds = metricIds;
      return this;
    }

    public Builder setMetricId(int metricId) {
      this.metricIds = singletonList(metricId);
      return this;
    }

    /**
     * All the measures are returned if parameter is {@code null}.
     */
    public Builder setMetricKeys(@Nullable List<String> s) {
      this.metricKeys = s;
      return this;
    }

    public Builder setMetricKey(String s) {
      this.metricKeys = singletonList(s);
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
