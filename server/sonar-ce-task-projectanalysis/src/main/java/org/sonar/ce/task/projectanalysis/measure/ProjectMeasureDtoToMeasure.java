/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.db.measure.ProjectMeasureDto;

import static java.util.Objects.requireNonNull;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.toLevel;

/**
 * Converts ProjectMeasureDto (from PROJECT_MEASURES table - historical analysis data) to Measure objects.
 */
public class ProjectMeasureDtoToMeasure {

  private ProjectMeasureDtoToMeasure() {
    // utility class
  }

  public static Optional<Measure> toMeasure(@Nullable ProjectMeasureDto measureDto, Metric metric) {
    requireNonNull(metric);
    if (measureDto == null) {
      return Optional.empty();
    }
    return MeasureConverter.toMeasure(new ProjectMeasureAdapter(measureDto), metric);
  }

  /**
   * Adapter for ProjectMeasureDto to work with the common MeasureConverter.
   */
  private static class ProjectMeasureAdapter implements MeasureConverter.MeasureDataAdapter {
    private final ProjectMeasureDto dto;

    ProjectMeasureAdapter(ProjectMeasureDto dto) {
      this.dto = dto;
    }

    @Override
    public Integer getIntValue() {
      Double value = dto.getValue();
      return value != null ? value.intValue() : null;
    }

    @Override
    public Long getLongValue() {
      Double value = dto.getValue();
      return value != null ? value.longValue() : null;
    }

    @Override
    public Double getDoubleValue() {
      return dto.getValue();
    }

    @Override
    public String getStringValue() {
      return dto.getData();
    }

    @Override
    public String getData() {
      return dto.getData();
    }

    @Override
    public QualityGateStatus getQualityGateStatus() {
      if (dto.getAlertStatus() != null) {
        Optional<Measure.Level> qualityGateStatus = toLevel(dto.getAlertStatus());
        if (qualityGateStatus.isPresent()) {
          return new QualityGateStatus(qualityGateStatus.get(), dto.getAlertText());
        }
      }
      return null;
    }

    @Override
    public boolean useNoValueForMissing() {
      // ProjectMeasureDto: a row exists but value is null -> NoValue measure
      return true;
    }
  }

}
