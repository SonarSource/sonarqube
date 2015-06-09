/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.measure;

import com.google.common.base.Optional;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;
import org.sonar.core.measure.db.MeasureDto;

import static org.sonar.server.computation.measure.Measure.AlertStatus.toAlertStatus;

public class MeasureDtoToMeasure {

  public Optional<Measure> toMeasure(@Nullable MeasureDto measureDto, Metric<?> metric) {
    Objects.requireNonNull(metric);
    if (measureDto == null) {
      return Optional.absent();
    }

    Double value = measureDto.getValue();
    String data = measureDto.getData();
    Class<?> valueType = metric.valueType();
    if (valueType == Integer.class) {
      if (value == null) {
        return toMeasure(MeasureImpl.createNoValue(), measureDto);
      }
      return toMeasure(MeasureImpl.create(value.intValue(), data), measureDto);
    }
    if (valueType == Long.class) {
      if (value == null) {
        return toMeasure(MeasureImpl.createNoValue(), measureDto);
      }
      return toMeasure(MeasureImpl.create(value.longValue(), data), measureDto);
    }
    if (valueType == Double.class) {
      if (value == null) {
        return toMeasure(MeasureImpl.createNoValue(), measureDto);
      }
      return toMeasure(MeasureImpl.create(value.doubleValue(), data), measureDto);
    }
    if (valueType == Boolean.class) {
      if (value == null) {
        return toMeasure(MeasureImpl.createNoValue(), measureDto);
      }
      return toMeasure(MeasureImpl.create(value == 1.0d, data), measureDto);
    }
    if (valueType == String.class) {
      if (data == null) {
        return toMeasure(MeasureImpl.createNoValue(), measureDto);
      }
      return toMeasure(MeasureImpl.create(data), measureDto);
    }
    if (valueType == Metric.Level.class) {
      return toMeasure(MeasureImpl.createNoValue(), measureDto);
    }
    return Optional.absent();
  }

  private static Optional<Measure> toMeasure(MeasureImpl measure, MeasureDto measureDto) {
    if (measureDto.getAlertStatus() != null && !measure.hasAlertStatus()) {
      Optional<Measure.AlertStatus> alertStatus = toAlertStatus(measureDto.getAlertStatus());
      if (alertStatus.isPresent()) {
        measure.setAlertStatus(alertStatus.get());
      }
    }
    if (measureDto.getAlertText() != null) {
      measure.setAlertText(measureDto.getAlertText());
    }

    return Optional.of((Measure) measure);
  }

}
