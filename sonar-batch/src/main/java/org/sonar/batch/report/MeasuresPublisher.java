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
package org.sonar.batch.report;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import javax.annotation.Nonnull;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.Constants.MeasureValueType;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.measure.MeasureCache;

public class MeasuresPublisher implements ReportPublisherStep {

  private final BatchComponentCache resourceCache;
  private final MeasureCache measureCache;

  public MeasuresPublisher(BatchComponentCache resourceCache, MeasureCache measureCache) {
    this.resourceCache = resourceCache;
    this.measureCache = measureCache;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    for (final BatchComponent resource : resourceCache.all()) {
      Iterable<Measure> batchMeasures = measureCache.byResource(resource.resource());
      Iterable<org.sonar.batch.protocol.output.BatchReport.Measure> reportMeasures = Iterables.transform(batchMeasures, new Function<Measure, BatchReport.Measure>() {
        private final BatchReport.Measure.Builder builder = BatchReport.Measure.newBuilder();

        @Override
        public BatchReport.Measure apply(@Nonnull Measure input) {
          validateMeasure(input, resource.key());
          return toReportMeasure(builder, input);
        }
      });
      writer.writeComponentMeasures(resource.batchId(), reportMeasures);
    }
  }

  private static void validateMeasure(Measure measure, String componentKey) {
    if (measure.getValue() == null && measure.getData() == null) {
      throw new IllegalArgumentException(String.format("Measure on metric '%s' and component '%s' has no value, but it's not allowed", measure.getMetricKey(), componentKey));
    }
  }

  private BatchReport.Measure toReportMeasure(BatchReport.Measure.Builder builder, Measure measure) {
    builder.clear();

    builder.setValueType(getMeasureValueType(measure.getMetric().getType()));
    setValueAccordingToType(builder, measure);
    // Because some numeric measures also have a data (like Sqale rating)
    String data = measure.getData();
    if (data != null) {
      builder.setStringValue(data);
    }
    builder.setMetricKey(measure.getMetricKey());
    return builder.build();
  }

  private void setValueAccordingToType(BatchReport.Measure.Builder builder, Measure measure) {
    Serializable value = measure.value();
    switch (builder.getValueType()) {
      case BOOLEAN:
        builder.setBooleanValue((Boolean) value);
        break;
      case DOUBLE:
        builder.setDoubleValue((Double) value);
        break;
      case INT:
        builder.setIntValue((Integer) value);
        break;
      case LONG:
        builder.setLongValue((Long) value);
        break;
      case STRING:
        builder.setStringValue((String) value);
        break;
      default:
        throw new IllegalStateException("Unknown value type: " + builder.getValueType());
    }
  }

  private MeasureValueType getMeasureValueType(ValueType type) {
    switch (type) {
      case INT:
      case MILLISEC:
      case RATING:
        return MeasureValueType.INT;
      case FLOAT:
      case PERCENT:
        return MeasureValueType.DOUBLE;
      case BOOL:
        return MeasureValueType.BOOLEAN;
      case STRING:
      case DATA:
      case LEVEL:
      case DISTRIB:
        return MeasureValueType.STRING;
      case WORK_DUR:
        return MeasureValueType.LONG;
      default:
        throw new IllegalStateException("Unknown value type: " + type);
    }
  }

}
