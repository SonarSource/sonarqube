/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.measure;

import java.security.SecureRandom;
import java.util.Random;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;

import static com.google.common.base.Preconditions.checkNotNull;

public class MeasureTesting {

  private static final Random RANDOM = new SecureRandom();

  private static int cursor = RANDOM.nextInt(100);

  private MeasureTesting() {
    // static methods only
  }

  public static ProjectMeasureDto newProjectMeasureDto(MetricDto metricDto, ComponentDto component, SnapshotDto analysis) {
    return newProjectMeasureDto(metricDto, component.uuid(), analysis);
  }

  public static ProjectMeasureDto newProjectMeasureDto(MetricDto metricDto, String branchUuid, SnapshotDto analysis) {
    checkNotNull(metricDto.getUuid());
    checkNotNull(metricDto.getKey());
    checkNotNull(branchUuid);
    checkNotNull(analysis.getUuid());
    return new ProjectMeasureDto()
      .setMetricUuid(metricDto.getUuid())
      .setComponentUuid(branchUuid)
      .setAnalysisUuid(analysis.getUuid());
  }

  public static ProjectMeasureDto newProjectMeasure() {
    return new ProjectMeasureDto()
      .setMetricUuid(String.valueOf(cursor++))
      .setComponentUuid(String.valueOf(cursor++))
      .setAnalysisUuid(String.valueOf(cursor++))
      .setData(String.valueOf(cursor++))
      .setAlertStatus(String.valueOf(cursor++))
      .setAlertText(String.valueOf(cursor++))
      .setValue((double) cursor++);
  }

  public static LiveMeasureDto newLiveMeasure() {
    return new LiveMeasureDto()
      .setMetricUuid(String.valueOf(cursor++))
      .setComponentUuid(String.valueOf(cursor++))
      .setProjectUuid(String.valueOf(cursor++))
      .setData(String.valueOf(cursor++))
      .setValue((double) cursor++);
  }

  public static LiveMeasureDto newLiveMeasure(ComponentDto component, MetricDto metric) {
    return new LiveMeasureDto()
      .setMetricUuid(metric.getUuid())
      .setComponentUuid(component.uuid())
      .setProjectUuid(component.branchUuid())
      .setData(String.valueOf(cursor++))
      .setValue((double) cursor++);
  }

  public static LiveMeasureDto newLiveMeasure(BranchDto branchDto, MetricDto metric) {
    return new LiveMeasureDto()
      .setMetricUuid(metric.getUuid())
      .setComponentUuid(branchDto.getUuid())
      .setProjectUuid(branchDto.getProjectUuid())
      .setData(String.valueOf(cursor++))
      .setValue((double) cursor++);
  }

  public static MeasureDto newMeasure() {
    return newMeasure(String.valueOf(cursor++), String.valueOf(cursor++), "metric" + cursor++, (double) cursor++);
  }

  public static MeasureDto newMeasure(ComponentDto component, MetricDto metric, Object value) {
    return newMeasure(component.uuid(), component.branchUuid(), metric.getKey(), value);
  }

  public static MeasureDto newMeasure(BranchDto branch, MetricDto metric, Object value) {
    return newMeasure(branch.getUuid(), branch.getUuid(), metric.getKey(), value);
  }

  private static MeasureDto newMeasure(String componentUuid, String branchUuid, String metricKey, Object value) {
    return new MeasureDto()
      .setComponentUuid(componentUuid)
      .setBranchUuid(branchUuid)
      .addValue(metricKey, value);
  }
}
