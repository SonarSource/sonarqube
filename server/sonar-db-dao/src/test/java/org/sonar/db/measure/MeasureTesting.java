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
package org.sonar.db.measure;

import org.apache.commons.lang.math.RandomUtils;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;

import static com.google.common.base.Preconditions.checkNotNull;

public class MeasureTesting {

  private static int cursor = RandomUtils.nextInt(100);

  private MeasureTesting() {
    // static methods only
  }

  public static MeasureDto newMeasureDto(MetricDto metricDto, ComponentDto component, SnapshotDto analysis) {
    checkNotNull(metricDto.getId());
    checkNotNull(metricDto.getKey());
    checkNotNull(component.uuid());
    checkNotNull(analysis.getUuid());
    return new MeasureDto()
      .setMetricId(metricDto.getId())
      .setComponentUuid(component.uuid())
      .setAnalysisUuid(analysis.getUuid());
  }

  public static MeasureDto newMeasure() {
    return new MeasureDto()
      .setMetricId(cursor++)
      .setComponentUuid(String.valueOf(cursor++))
      .setAnalysisUuid(String.valueOf(cursor++))
      .setDeveloperId(null)
      .setData(String.valueOf(cursor++))
      .setAlertStatus(String.valueOf(cursor++))
      .setAlertText(String.valueOf(cursor++))
      .setValue((double)cursor++);
  }
}
