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
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.metric.Metric;

import static com.google.common.base.Optional.of;

public class BatchMeasureToMeasure {

  public Optional<Measure> toMeasure(@Nullable BatchReport.Measure batchMeasure, Metric metric) {
    Objects.requireNonNull(metric);
    if (batchMeasure == null) {
      return Optional.absent();
    }

    Measure.NewMeasureBuilder builder = Measure.newMeasureBuilder();
    String data = batchMeasure.hasStringValue() ? batchMeasure.getStringValue() : null;
    switch (metric.getType().getValueType()) {
      case INT:
        return toIntegerMeasure(builder, batchMeasure, data);
      case LONG:
        return toLongMeasure(builder, batchMeasure, data);
      case DOUBLE:
        return toDoubleMeasure(builder, batchMeasure, data);
      case BOOLEAN:
        return toBooleanMeasure(builder, batchMeasure, data);
      case STRING:
        return toStringMeasure(builder, batchMeasure);
      case LEVEL:
        return toLevelMeasure(builder, batchMeasure);
      case NO_VALUE:
        return toNoValueMeasure(builder, batchMeasure);
      default:
        throw new IllegalArgumentException("Unsupported Measure.ValueType " + metric.getType().getValueType());
    }
  }

  private static Optional<Measure> toIntegerMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasIntValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getIntValue(), data));
  }

  private static Optional<Measure> toLongMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasLongValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getLongValue(), data));
  }

  private static Optional<Measure> toDoubleMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasDoubleValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getDoubleValue(),
      // Decimals are not truncated in scanner report, so an arbitrary decimal scale is applied when reading values from report
      org.sonar.api.measures.Metric.MAX_DECIMAL_SCALE,
      data));
  }

  private static Optional<Measure> toBooleanMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasBooleanValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getBooleanValue(), data));
  }

  private static Optional<Measure> toStringMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure) {
    if (!batchMeasure.hasStringValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getStringValue()));
  }

  private static Optional<Measure> toLevelMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure) {
    if (!batchMeasure.hasStringValue()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    Optional<Measure.Level> level = Measure.Level.toLevel(batchMeasure.getStringValue());
    if (!level.isPresent()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(level.get()));
  }

  private static Optional<Measure> toNoValueMeasure(Measure.NewMeasureBuilder builder, BatchReport.Measure batchMeasure) {
    return of(builder.createNoValue());
  }
}
