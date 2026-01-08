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
import org.sonar.db.measure.MeasureDto;

import static java.util.Objects.requireNonNull;

/**
 * Converts MeasureDto (from MEASURES table - current live state) to Measure objects.
 */
public class MeasureDtoToMeasure {

  private MeasureDtoToMeasure() {
    // utility class
  }

  public static Optional<Measure> toMeasure(@Nullable MeasureDto measureDto, Metric metric) {
    requireNonNull(metric);
    if (measureDto == null) {
      return Optional.empty();
    }
    return MeasureConverter.toMeasure(new LiveMeasureAdapter(measureDto, metric.getKey()), metric);
  }

  /**
   * Adapter for MeasureDto to work with the common MeasureConverter.
   * MeasureDto stores values in a JSON map, so we need the metric key to extract values.
   */
  private static class LiveMeasureAdapter implements MeasureConverter.MeasureDataAdapter {
    private final MeasureDto dto;
    private final String metricKey;

    LiveMeasureAdapter(MeasureDto dto, String metricKey) {
      this.dto = dto;
      this.metricKey = metricKey;
    }

    @Override
    public Integer getIntValue() {
      return dto.getInt(metricKey);
    }

    @Override
    public Long getLongValue() {
      return dto.getLong(metricKey);
    }

    @Override
    public Double getDoubleValue() {
      return dto.getDouble(metricKey);
    }

    @Override
    public String getStringValue() {
      return dto.getString(metricKey);
    }

    @Override
    public String getData() {
      // MeasureDto doesn't support additional data field
      return null;
    }

    @Override
    public QualityGateStatus getQualityGateStatus() {
      // MeasureDto doesn't store QG status separately - it's just another metric value
      return null;
    }

    @Override
    public boolean useNoValueForMissing() {
      // MeasureDto: key doesn't exist in JSON map -> no measure data exists
      return false;
    }
  }

}
