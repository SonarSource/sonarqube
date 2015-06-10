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

public class BatchMeasureToMeasure {

  public Optional<Measure> toMeasure(@Nullable BatchReport.Measure batchMeasure, Metric metric) {
    Objects.requireNonNull(metric);
    if (batchMeasure == null) {
      return Optional.absent();
    }

    String data = batchMeasure.hasStringValue() ? batchMeasure.getStringValue() : null;
    switch (metric.getType().getValueType()) {
      case INT:
        return toIntegerMeasure(batchMeasure, data);
      case LONG:
        return toLongMeasure(batchMeasure, data);
      case DOUBLE:
        return toDoubleMeasure(batchMeasure, data);
      case BOOLEAN:
        return toBooleanMeasure(batchMeasure, data);
      case STRING:
        return toStringMeasure(batchMeasure);
      case LEVEL:
        return toLevelMeasure(batchMeasure);
      case NO_VALUE:
        return toNoValueMeasure(batchMeasure);
      default:
        throw new IllegalArgumentException("Unsupported Measure.ValueType " + metric.getType().getValueType());
    }
  }

  private static Optional<Measure> toIntegerMeasure(BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasIntValue()) {
      return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
    }
    return toMeasure(MeasureImpl.create(batchMeasure.getIntValue(), data), batchMeasure);
  }

  private static Optional<Measure> toLongMeasure(BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasLongValue()) {
      return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
    }
    return toMeasure(MeasureImpl.create(batchMeasure.getLongValue(), data), batchMeasure);
  }

  private static Optional<Measure> toDoubleMeasure(BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasDoubleValue()) {
      return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
    }
    return toMeasure(MeasureImpl.create(batchMeasure.getDoubleValue(), data), batchMeasure);
  }

  private static Optional<Measure> toBooleanMeasure(BatchReport.Measure batchMeasure, @Nullable String data) {
    if (!batchMeasure.hasBooleanValue()) {
      return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
    }
    return toMeasure(MeasureImpl.create(batchMeasure.getBooleanValue(), data), batchMeasure);
  }

  private static Optional<Measure> toStringMeasure(BatchReport.Measure batchMeasure) {
    if (!batchMeasure.hasStringValue()) {
      return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
    }
    return toMeasure(MeasureImpl.create(batchMeasure.getStringValue()), batchMeasure);
  }
  
  private static Optional<Measure> toLevelMeasure(BatchReport.Measure batchMeasure) {
    if (!batchMeasure.hasStringValue()) {
      return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
    }
    Optional<Measure.Level> level = Measure.Level.toLevel(batchMeasure.getStringValue());
    if (!level.isPresent()) {
      return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
    }
    return toMeasure(MeasureImpl.create(level.get()), batchMeasure);
  }

  private static Optional<Measure> toNoValueMeasure(BatchReport.Measure batchMeasure) {
    return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
  }

  private static Optional<Measure> toMeasure(MeasureImpl measure, BatchReport.Measure batchMeasure) {
    if (batchMeasure.hasAlertStatus() && !measure.hasQualityGateStatus()) {
      Optional<Measure.Level> qualityGateStatus = Measure.Level.toLevel(batchMeasure.getAlertStatus());
      if (qualityGateStatus.isPresent()) {
        String text = batchMeasure.hasAlertText() ? batchMeasure.getAlertText() : null;
        measure.setQualityGateStatus(new Measure.QualityGateStatus(qualityGateStatus.get(), text));
      }
    }
    return Optional.of((Measure) measure);
  }

}
