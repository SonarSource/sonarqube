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
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.server.computation.metric.Metric;

import static org.sonar.server.computation.measure.Measure.Level.toLevel;

public class MeasureDtoToMeasure {

  public Optional<Measure> toMeasure(@Nullable MeasureDto measureDto, Metric metric) {
    Objects.requireNonNull(metric);
    if (measureDto == null) {
      return Optional.absent();
    }

    Double value = measureDto.getValue();
    String data = measureDto.getData();
    switch (metric.getType().getValueType()) {
      case INT:
        return toIntegerMeasure(measureDto, value, data);
      case LONG:
        return toLongMeasure(measureDto, value, data);
      case DOUBLE:
        return toDoubleMeasure(measureDto, value, data);
      case BOOLEAN:
        return toBooleanMeasure(measureDto, value, data);
      case STRING:
        return toStringMeasure(measureDto, data);
      case LEVEL:
        return toLevelMeasure(measureDto, data);
      case NO_VALUE:
        return toNoValueMeasure(measureDto);
      default:
        throw new IllegalArgumentException("Unsupported Measure.ValueType " + metric.getType().getValueType());
    }
  }

  private static Optional<Measure> toIntegerMeasure(MeasureDto measureDto, @Nullable Double value, String data) {
    if (value == null) {
      return toMeasure(MeasureImpl.createNoValue(), measureDto);
    }
    return toMeasure(MeasureImpl.create(value.intValue(), data), measureDto);
  }

  private static Optional<Measure> toLongMeasure(MeasureDto measureDto, @Nullable Double value, String data) {
    if (value == null) {
      return toMeasure(MeasureImpl.createNoValue(), measureDto);
    }
    return toMeasure(MeasureImpl.create(value.longValue(), data), measureDto);
  }

  private static Optional<Measure> toDoubleMeasure(MeasureDto measureDto, @Nullable Double value, String data) {
    if (value == null) {
      return toMeasure(MeasureImpl.createNoValue(), measureDto);
    }
    return toMeasure(MeasureImpl.create(value.doubleValue(), data), measureDto);
  }

  private static Optional<Measure> toBooleanMeasure(MeasureDto measureDto, @Nullable Double value, String data) {
    if (value == null) {
      return toMeasure(MeasureImpl.createNoValue(), measureDto);
    }
    return toMeasure(MeasureImpl.create(value == 1.0d, data), measureDto);
  }

  private static Optional<Measure> toStringMeasure(MeasureDto measureDto, @Nullable String data) {
    if (data == null) {
      return toMeasure(MeasureImpl.createNoValue(), measureDto);
    }
    return toMeasure(MeasureImpl.create(data), measureDto);
  }

  private static Optional<Measure> toLevelMeasure(MeasureDto measureDto, @Nullable String data) {
    if (data == null) {
      return toMeasure(MeasureImpl.createNoValue(), measureDto);
    }
    Optional<Measure.Level> level = toLevel(data);
    if (!level.isPresent()) {
      return toMeasure(MeasureImpl.createNoValue(), measureDto);
    }
    return toMeasure(MeasureImpl.create(level.get()), measureDto);
  }

  private static Optional<Measure> toNoValueMeasure(MeasureDto measureDto) {
    return toMeasure(MeasureImpl.createNoValue(), measureDto);
  }

  private static Optional<Measure> toMeasure(MeasureImpl measure, MeasureDto measureDto) {
    if (measureDto.getAlertStatus() != null && !measure.hasQualityGateStatus()) {
      Optional<Measure.Level> qualityGateStatus = toLevel(measureDto.getAlertStatus());
      if (qualityGateStatus.isPresent()) {
        measure.setQualityGateStatus(new Measure.QualityGateStatus(qualityGateStatus.get(), measureDto.getAlertText()));
      }
    }

    return Optional.of((Measure) measure);
  }

}
