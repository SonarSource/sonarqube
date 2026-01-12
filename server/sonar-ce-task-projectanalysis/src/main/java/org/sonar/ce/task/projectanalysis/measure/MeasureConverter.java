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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.toLevel;

/**
 * Common converter logic for transforming DTOs into Measure objects.
 * Uses a MeasureDataAdapter to abstract away the differences between ProjectMeasureDto and MeasureDto.
 */
public class MeasureConverter {

  private MeasureConverter() {
    // utility class
  }

  /**
   * Adapter interface to abstract data access from different measure DTO types.
   */
  public interface MeasureDataAdapter {
    @Nullable
    Integer getIntValue();

    @Nullable
    Long getLongValue();

    @Nullable
    Double getDoubleValue();

    @Nullable
    String getStringValue();

    @Nullable
    String getData();

    @Nullable
    QualityGateStatus getQualityGateStatus();

    /**
     * Controls how missing or {@code null} values are represented.
     * When {@code true}, a {@link Measure} with {@link Measure.ValueType#NO_VALUE} is returned.
     * This is used for historical {@code PROJECT_MEASURES} rows where the record exists but
     * the value is stored as explicit {@code NULL} in the database columns.
     * When {@code false}, {@link Optional#empty()} is returned. This is used for live
     * {@code MEASURES}, where measures are stored in JSON and a missing value is represented
     * by the absence of the corresponding key in the JSON payload.
     */
    boolean useNoValueForMissing();
  }

  public static Optional<Measure> toMeasure(MeasureDataAdapter adapter, Metric metric) {
    requireNonNull(metric);
    requireNonNull(adapter);

    switch (metric.getType().getValueType()) {
      case INT:
        return toIntegerMeasure(adapter);
      case LONG:
        return toLongMeasure(adapter);
      case DOUBLE:
        return toDoubleMeasure(adapter);
      case BOOLEAN:
        return toBooleanMeasure(adapter);
      case STRING:
        return toStringMeasure(adapter);
      case LEVEL:
        return toLevelMeasure(adapter);
      case NO_VALUE:
        return toNoValueMeasure(adapter);
      default:
        throw new IllegalArgumentException("Unsupported Measure.ValueType " + metric.getType().getValueType());
    }
  }

  private static Optional<Measure> toIntegerMeasure(MeasureDataAdapter adapter) {
    Integer value = adapter.getIntValue();
    if (value == null) {
      return handleMissingValue(adapter);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), adapter).create(value, adapter.getData()));
  }

  private static Optional<Measure> toLongMeasure(MeasureDataAdapter adapter) {
    Long value = adapter.getLongValue();
    if (value == null) {
      return handleMissingValue(adapter);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), adapter).create(value, adapter.getData()));
  }

  private static Optional<Measure> toDoubleMeasure(MeasureDataAdapter adapter) {
    Double value = adapter.getDoubleValue();
    if (value == null) {
      return handleMissingValue(adapter);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), adapter)
      .create(value, org.sonar.api.measures.Metric.MAX_DECIMAL_SCALE, adapter.getData()));
  }

  private static Optional<Measure> toBooleanMeasure(MeasureDataAdapter adapter) {
    Double value = adapter.getDoubleValue();
    if (value == null) {
      return handleMissingValue(adapter);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), adapter)
      .create(Double.compare(value, 1.0D) == 0, adapter.getData()));
  }

  private static Optional<Measure> toStringMeasure(MeasureDataAdapter adapter) {
    String value = adapter.getStringValue();
    if (value == null) {
      return handleMissingValue(adapter);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), adapter).create(value));
  }

  private static Optional<Measure> toLevelMeasure(MeasureDataAdapter adapter) {
    String value = adapter.getStringValue();
    if (value == null) {
      return handleMissingValue(adapter);
    }
    Optional<Measure.Level> level = toLevel(value);
    if (level.isEmpty()) {
      return handleMissingValue(adapter);
    }
    return of(setCommonProperties(Measure.newMeasureBuilder(), adapter).create(level.get()));
  }

  private static Optional<Measure> toNoValueMeasure(MeasureDataAdapter adapter) {
    return of(setCommonProperties(Measure.newMeasureBuilder(), adapter).createNoValue());
  }

  private static Optional<Measure> handleMissingValue(MeasureDataAdapter adapter) {
    return adapter.useNoValueForMissing() ? toNoValueMeasure(adapter) : Optional.empty();
  }

  private static Measure.NewMeasureBuilder setCommonProperties(Measure.NewMeasureBuilder builder, MeasureDataAdapter adapter) {
    QualityGateStatus qgStatus = adapter.getQualityGateStatus();
    if (qgStatus != null) {
      builder.setQualityGateStatus(qgStatus);
    }
    return builder;
  }

}
