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
package org.sonar.server.measure.ws;

import com.google.common.collect.Table;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentDtoWithSnapshotId;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.WsMeasures;

import static java.util.Objects.requireNonNull;

class ComponentTreeData {
  private final ComponentDto baseComponent;
  private final List<ComponentDtoWithSnapshotId> components;
  private final int componentCount;
  private final Map<Long, ComponentDto> referenceComponentsById;
  private final List<MetricDto> metrics;
  private final List<WsMeasures.Period> periods;
  private final Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;

  private ComponentTreeData(Builder builder) {
    this.baseComponent = builder.baseComponent;
    this.components = builder.componentsFromDb;
    this.componentCount = builder.componentCount;
    this.referenceComponentsById = builder.referenceComponentsById;
    this.metrics = builder.metrics;
    this.measuresByComponentUuidAndMetric = builder.measuresByComponentUuidAndMetric;
    this.periods = builder.periods;
  }

  public ComponentDto getBaseComponent() {
    return baseComponent;
  }

  @CheckForNull
  List<ComponentDtoWithSnapshotId> getComponents() {
    return components;
  }

  @CheckForNull
  int getComponentCount() {
    return componentCount;
  }

  @CheckForNull
  public Map<Long, ComponentDto> getReferenceComponentsById() {
    return referenceComponentsById;
  }

  @CheckForNull
  List<MetricDto> getMetrics() {
    return metrics;
  }

  @CheckForNull
  List<WsMeasures.Period> getPeriods() {
    return periods;
  }

  @CheckForNull
  Table<String, MetricDto, MeasureDto> getMeasuresByComponentUuidAndMetric() {
    return measuresByComponentUuidAndMetric;
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private ComponentDto baseComponent;
    private List<ComponentDtoWithSnapshotId> componentsFromDb;
    private Map<Long, ComponentDto> referenceComponentsById;
    private int componentCount;
    private List<MetricDto> metrics;
    private List<WsMeasures.Period> periods;
    private Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;

    private Builder() {
      // private constructor
    }

    public Builder setBaseComponent(ComponentDto baseComponent) {
      this.baseComponent = baseComponent;
      return this;
    }

    public Builder setComponentsFromDb(List<ComponentDtoWithSnapshotId> componentsFromDbQuery) {
      this.componentsFromDb = componentsFromDbQuery;
      return this;
    }

    public Builder setComponentCount(int componentCount) {
      this.componentCount = componentCount;
      return this;
    }

    public Builder setMetrics(List<MetricDto> metrics) {
      this.metrics = metrics;
      return this;
    }

    public Builder setPeriods(List<WsMeasures.Period> periods) {
      this.periods = periods;
      return this;
    }

    public Builder setMeasuresByComponentUuidAndMetric(Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
      return this;
    }

    public Builder setReferenceComponentsById(Map<Long, ComponentDto> referenceComponentsById) {
      this.referenceComponentsById = referenceComponentsById;
      return this;
    }

    public ComponentTreeData build() {
      requireNonNull(baseComponent);
      return new ComponentTreeData(this);
    }
  }
}
