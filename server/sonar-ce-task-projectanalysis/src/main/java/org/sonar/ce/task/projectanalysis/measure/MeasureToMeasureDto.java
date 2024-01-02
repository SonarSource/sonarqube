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
package org.sonar.ce.task.projectanalysis.measure;

import javax.annotation.CheckForNull;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;

public class MeasureToMeasureDto {

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;

  public MeasureToMeasureDto(AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder treeRootHolder) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.treeRootHolder = treeRootHolder;
  }

  public MeasureDto toMeasureDto(Measure measure, Metric metric, Component component) {
    MeasureDto out = new MeasureDto();
    out.setMetricUuid(metric.getUuid());
    out.setComponentUuid(component.getUuid());
    out.setAnalysisUuid(analysisMetadataHolder.getUuid());
    if (measure.hasQualityGateStatus()) {
      setAlert(out, measure.getQualityGateStatus());
    }
    out.setValue(valueAsDouble(measure));
    out.setData(data(measure));
    return out;
  }

  public LiveMeasureDto toLiveMeasureDto(Measure measure, Metric metric, Component component) {
    LiveMeasureDto out = new LiveMeasureDto();
    out.setMetricUuid(metric.getUuid());
    out.setComponentUuid(component.getUuid());
    out.setProjectUuid(treeRootHolder.getRoot().getUuid());
    out.setValue(valueAsDouble(measure));
    out.setData(data(measure));
    return out;
  }

  private static void setAlert(MeasureDto measureDto, QualityGateStatus qualityGateStatus) {
    measureDto.setAlertStatus(qualityGateStatus.getStatus().name());
    measureDto.setAlertText(qualityGateStatus.getText());
  }

  private static String data(Measure in) {
    switch (in.getValueType()) {
      case NO_VALUE, BOOLEAN, INT, LONG, DOUBLE:
        return in.getData();
      case STRING:
        return in.getStringValue();
      case LEVEL:
        return in.getLevelValue().name();
      default:
        return null;
    }
  }

  /**
   * return the numerical value as a double. It's the type used in db.
   * Returns null if no numerical value found
   */
  @CheckForNull
  private static Double valueAsDouble(Measure measure) {
    return switch (measure.getValueType()) {
      case BOOLEAN -> measure.getBooleanValue() ? 1.0D : 0.0D;
      case INT -> (double) measure.getIntValue();
      case LONG -> (double) measure.getLongValue();
      case DOUBLE -> measure.getDoubleValue();
      default -> null;
    };
  }
}
