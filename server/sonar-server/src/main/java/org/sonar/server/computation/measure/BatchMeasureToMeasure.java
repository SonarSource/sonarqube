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
import org.sonar.batch.protocol.output.BatchReport;

public class BatchMeasureToMeasure {

  public Optional<Measure> toMeasure(@Nullable BatchReport.Measure batchMeasure, Metric<?> metric) {
    Objects.requireNonNull(metric);
    if (batchMeasure == null) {
      return Optional.absent();
    }

    String data = batchMeasure.hasStringValue() ? batchMeasure.getStringValue() : null;
    Class<?> valueType = metric.valueType();
    if (valueType == Integer.class) {
      if (!batchMeasure.hasIntValue()) {
        return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
      }
      return toMeasure(MeasureImpl.create(batchMeasure.getIntValue(), data), batchMeasure);
    }
    if (valueType == Long.class) {
      if (!batchMeasure.hasLongValue()) {
        return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
      }
      return toMeasure(MeasureImpl.create(batchMeasure.getLongValue(), data), batchMeasure);
    }
    if (valueType == Double.class) {
      if (!batchMeasure.hasDoubleValue()) {
        return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
      }
      return toMeasure(MeasureImpl.create(batchMeasure.getDoubleValue(), data), batchMeasure);
    }
    if (valueType == Boolean.class) {
      if (!batchMeasure.hasBooleanValue()) {
        return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
      }
      return toMeasure(MeasureImpl.create(batchMeasure.getBooleanValue(), data), batchMeasure);
    }
    if (valueType == String.class) {
      if (!batchMeasure.hasStringValue()) {
        return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
      }
      return toMeasure(MeasureImpl.create(batchMeasure.getStringValue()), batchMeasure);
    }
    if (valueType == Metric.Level.class) {
      return toMeasure(MeasureImpl.createNoValue(), batchMeasure);
    }
    return Optional.absent();
  }

  private static Optional<Measure.AlertStatus> toAlertStatus(BatchReport.Measure batchMeasure) {
    if (batchMeasure.hasAlertStatus()) {
      return Measure.AlertStatus.toAlertStatus(batchMeasure.getAlertStatus());
    }
    return Optional.absent();
  }

  private static Optional<Measure> toMeasure(MeasureImpl measure, BatchReport.Measure batchMeasure) {
    if (batchMeasure.hasAlertStatus() && !measure.hasAlertStatus()) {
      Optional<Measure.AlertStatus> alertStatus = toAlertStatus(batchMeasure);
      if (alertStatus.isPresent()) {
        measure.setAlertStatus(alertStatus.get());
      }
    }
    if (batchMeasure.hasAlertText()) {
      measure.setAlertText(batchMeasure.getAlertText());
    }

    return Optional.of((Measure) measure);
  }
}
