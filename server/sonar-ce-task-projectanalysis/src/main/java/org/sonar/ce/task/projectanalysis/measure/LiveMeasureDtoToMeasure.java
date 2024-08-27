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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.db.measure.LiveMeasureDto;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.toLevel;

public class LiveMeasureDtoToMeasure {

  private LiveMeasureDtoToMeasure() {
    // utility class
  }

  public static Optional<Measure> toMeasure(@Nullable LiveMeasureDto measureDto, Metric metric) {
    requireNonNull(metric);
    if (measureDto == null) {
      return Optional.empty();
    }
    Double value = measureDto.getValue();
    String data = measureDto.getDataAsString();
    switch (metric.getType().getValueType()) {
      case INT:
        return toIntegerMeasure(value, data);
      case LONG:
        return toLongMeasure(value, data);
      case DOUBLE:
        return toDoubleMeasure(value, data);
      case BOOLEAN:
        return toBooleanMeasure(value, data);
      case STRING:
        return toStringMeasure(data);
      case LEVEL:
        return toLevelMeasure(data);
      case NO_VALUE:
        return toNoValueMeasure();
      default:
        throw new IllegalArgumentException("Unsupported Measure.ValueType " + metric.getType().getValueType());
    }
  }

  private static Optional<Measure> toIntegerMeasure(@Nullable Double value, @Nullable String data) {
    if (value == null) {
      return toNoValueMeasure();
    }
    return of(Measure.newMeasureBuilder().create(value.intValue(), data));
  }

  private static Optional<Measure> toLongMeasure(@Nullable Double value, @Nullable String data) {
    if (value == null) {
      return toNoValueMeasure();
    }
    return of(Measure.newMeasureBuilder().create(value.longValue(), data));
  }

  private static Optional<Measure> toDoubleMeasure(@Nullable Double value, @Nullable String data) {
    if (value == null) {
      return toNoValueMeasure();
    }

    return of(Measure.newMeasureBuilder().create(value, org.sonar.api.measures.Metric.MAX_DECIMAL_SCALE, data));
  }

  private static Optional<Measure> toBooleanMeasure(@Nullable Double value, @Nullable String data) {
    if (value == null) {
      return toNoValueMeasure();
    }
    return of(Measure.newMeasureBuilder().create(Double.compare(value, 1.0D) == 0, data));
  }

  private static Optional<Measure> toStringMeasure(@Nullable String data) {
    if (data == null) {
      return toNoValueMeasure();
    }
    return of(Measure.newMeasureBuilder().create(data));
  }

  private static Optional<Measure> toLevelMeasure(@Nullable String data) {
    if (data == null) {
      return toNoValueMeasure();
    }
    Optional<Measure.Level> level = toLevel(data);
    if (!level.isPresent()) {
      return toNoValueMeasure();
    }
    return of(Measure.newMeasureBuilder().create(level.get()));
  }

  private static Optional<Measure> toNoValueMeasure() {
    return of(Measure.newMeasureBuilder().createNoValue());
  }
}
