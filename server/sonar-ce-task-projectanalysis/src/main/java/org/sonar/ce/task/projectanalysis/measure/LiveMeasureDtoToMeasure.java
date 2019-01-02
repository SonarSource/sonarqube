/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.measure.LiveMeasureDto;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.toLevel;

public class LiveMeasureDtoToMeasure {

  public Optional<Measure> toMeasure(@Nullable LiveMeasureDto measureDto, Metric metric) {
    requireNonNull(metric);
    if (measureDto == null) {
      return Optional.empty();
    }
    Double value = measureDto.getValue();
    String data = measureDto.getDataAsString();
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

  private static Optional<Measure> toIntegerMeasure(LiveMeasureDto measureDto, @Nullable Double value, @Nullable String data) {
    if (value == null) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(value.intValue(), data));
  }

  private static Optional<Measure> toLongMeasure(LiveMeasureDto measureDto, @Nullable Double value, @Nullable String data) {
    if (value == null) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(value.longValue(), data));
  }

  private static Optional<Measure> toDoubleMeasure(LiveMeasureDto measureDto, @Nullable Double value, @Nullable String data) {
    if (value == null) {
      return toNoValueMeasure(measureDto);
    }

    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto)
      .create(value.doubleValue(), org.sonar.api.measures.Metric.MAX_DECIMAL_SCALE, data));
  }

  private static Optional<Measure> toBooleanMeasure(LiveMeasureDto measureDto, @Nullable Double value, @Nullable String data) {
    if (value == null) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(value == 1.0d, data));
  }

  private static Optional<Measure> toStringMeasure(LiveMeasureDto measureDto, @Nullable String data) {
    if (data == null) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(data));
  }

  private static Optional<Measure> toLevelMeasure(LiveMeasureDto measureDto, @Nullable String data) {
    if (data == null) {
      return toNoValueMeasure(measureDto);
    }
    Optional<Measure.Level> level = toLevel(data);
    if (!level.isPresent()) {
      return toNoValueMeasure(measureDto);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).create(level.get()));
  }

  private static Optional<Measure> toNoValueMeasure(LiveMeasureDto measureDto) {
    return of(setCommonProperties(Measure.newMeasureBuilder(), measureDto).createNoValue());
  }

  private static Measure.NewMeasureBuilder setCommonProperties(Measure.NewMeasureBuilder builder, LiveMeasureDto measureDto) {
    Double variation = measureDto.getVariation();
    if (variation != null) {
      builder.setVariation(variation);
    }
    return builder;
  }

}
