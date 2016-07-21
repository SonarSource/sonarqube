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
package org.sonar.server.computation.task.projectanalysis.measure;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.DbIdsRepository;
import org.sonar.server.computation.task.projectanalysis.component.Developer;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;

public class MeasureToMeasureDto {

  private final DbIdsRepository dbIdsRepository;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public MeasureToMeasureDto(DbIdsRepository dbIdsRepository, AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbIdsRepository = dbIdsRepository;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Nonnull
  public MeasureDto toMeasureDto(Measure measure, Metric metric, Component component) {
    MeasureDto out = new MeasureDto();
    out.setMetricId(metric.getId());
    out.setComponentUuid(component.getUuid());
    out.setAnalysisUuid(analysisMetadataHolder.getUuid());
    if (measure.hasVariations()) {
      setVariations(out, measure.getVariations());
    }
    if (measure.hasQualityGateStatus()) {
      setAlert(out, measure.getQualityGateStatus());
    }
    Developer developer = measure.getDeveloper();
    if (developer != null) {
      out.setDeveloperId(dbIdsRepository.getDeveloperId(developer));
    }
    out.setDescription(measure.getDescription());
    out.setValue(valueAsDouble(measure));
    out.setData(data(measure));
    return out;
  }

  private static void setVariations(MeasureDto measureDto, MeasureVariations variations) {
    measureDto.setVariation(1, variations.hasVariation1() ? variations.getVariation1() : null);
    measureDto.setVariation(2, variations.hasVariation2() ? variations.getVariation2() : null);
    measureDto.setVariation(3, variations.hasVariation3() ? variations.getVariation3() : null);
    measureDto.setVariation(4, variations.hasVariation4() ? variations.getVariation4() : null);
    measureDto.setVariation(5, variations.hasVariation5() ? variations.getVariation5() : null);
  }

  private static void setAlert(MeasureDto measureDto, QualityGateStatus qualityGateStatus) {
    measureDto.setAlertStatus(qualityGateStatus.getStatus().name());
    measureDto.setAlertText(qualityGateStatus.getText());
  }

  private static String data(Measure in) {
    switch (in.getValueType()) {
      case NO_VALUE:
      case BOOLEAN:
      case INT:
      case LONG:
      case DOUBLE:
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
    switch (measure.getValueType()) {
      case BOOLEAN:
        return measure.getBooleanValue() ? 1.0d : 0.0d;
      case INT:
        return (double) measure.getIntValue();
      case LONG:
        return (double) measure.getLongValue();
      case DOUBLE:
        return measure.getDoubleValue();
      case NO_VALUE:
      case STRING:
      case LEVEL:
      default:
        return null;
    }
  }
}
