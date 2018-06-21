/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.measure;

import com.google.common.base.Optional;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.ValueCase;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;

import static com.google.common.base.Optional.of;
import static org.apache.commons.lang.StringUtils.trimToNull;

public class BatchMeasureToMeasure {

  public Optional<Measure> toMeasure(@Nullable ScannerReport.Measure batchMeasure, Metric metric) {
    Objects.requireNonNull(metric);
    if (batchMeasure == null) {
      return Optional.absent();
    }

    Measure.NewMeasureBuilder builder = Measure.newMeasureBuilder();
    switch (metric.getType().getValueType()) {
      case INT:
        return toIntegerMeasure(builder, batchMeasure);
      case LONG:
        return toLongMeasure(builder, batchMeasure);
      case DOUBLE:
        return toDoubleMeasure(builder, batchMeasure);
      case BOOLEAN:
        return toBooleanMeasure(builder, batchMeasure);
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

  private static Optional<Measure> toIntegerMeasure(Measure.NewMeasureBuilder builder, ScannerReport.Measure batchMeasure) {
    if (batchMeasure.getValueCase() == ValueCase.VALUE_NOT_SET) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getIntValue().getValue(), trimToNull(batchMeasure.getIntValue().getData())));
  }

  private static Optional<Measure> toLongMeasure(Measure.NewMeasureBuilder builder, ScannerReport.Measure batchMeasure) {
    if (batchMeasure.getValueCase() == ValueCase.VALUE_NOT_SET) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getLongValue().getValue(), trimToNull(batchMeasure.getLongValue().getData())));
  }

  private static Optional<Measure> toDoubleMeasure(Measure.NewMeasureBuilder builder, ScannerReport.Measure batchMeasure) {
    if (batchMeasure.getValueCase() == ValueCase.VALUE_NOT_SET) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getDoubleValue().getValue(),
      // Decimals are not truncated in scanner report, so an arbitrary decimal scale is applied when reading values from report
      org.sonar.api.measures.Metric.MAX_DECIMAL_SCALE, trimToNull(batchMeasure.getDoubleValue().getData())));
  }

  private static Optional<Measure> toBooleanMeasure(Measure.NewMeasureBuilder builder, ScannerReport.Measure batchMeasure) {
    if (batchMeasure.getValueCase() == ValueCase.VALUE_NOT_SET) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getBooleanValue().getValue(), trimToNull(batchMeasure.getBooleanValue().getData())));
  }

  private static Optional<Measure> toStringMeasure(Measure.NewMeasureBuilder builder, ScannerReport.Measure batchMeasure) {
    if (batchMeasure.getValueCase() == ValueCase.VALUE_NOT_SET) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(batchMeasure.getStringValue().getValue()));
  }

  private static Optional<Measure> toLevelMeasure(Measure.NewMeasureBuilder builder, ScannerReport.Measure batchMeasure) {
    if (batchMeasure.getValueCase() == ValueCase.VALUE_NOT_SET) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    Optional<Measure.Level> level = Measure.Level.toLevel(batchMeasure.getStringValue().getValue());
    if (!level.isPresent()) {
      return toNoValueMeasure(builder, batchMeasure);
    }
    return of(builder.create(level.get()));
  }

  private static Optional<Measure> toNoValueMeasure(Measure.NewMeasureBuilder builder, ScannerReport.Measure batchMeasure) {
    return of(builder.createNoValue());
  }
}
