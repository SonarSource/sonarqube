/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.report;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.io.Serializable;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.Constants.MeasureValueType;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.scan.measure.MeasureCache;
import org.sonar.core.metric.BatchMetrics;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

public class MeasuresPublisher implements ReportPublisherStep {

  private static final class MeasureToReportMeasure implements Function<Measure, BatchReport.Measure> {
    private final BatchComponent resource;
    private final BatchReport.Measure.Builder builder = BatchReport.Measure.newBuilder();

    private MeasureToReportMeasure(BatchComponent resource) {
      this.resource = resource;
    }

    @Override
    public BatchReport.Measure apply(@Nonnull Measure input) {
      validateMeasure(input, resource.key());
      return toReportMeasure(builder, input);
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
          builder.setDoubleValue(((Number) value).doubleValue());
          break;
        case INT:
          builder.setIntValue(((Number) value).intValue());
          break;
        case LONG:
          builder.setLongValue(((Number) value).longValue());
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
        case MILLISEC:
          return MeasureValueType.LONG;
        default:
          throw new IllegalStateException("Unknown value type: " + type);
      }
    }

  }

  private static final class IsMetricAllowed implements Predicate<Measure> {
    private final Set<String> allowedMetricKeys;

    private IsMetricAllowed(Set<String> allowedMetricKeys) {
      this.allowedMetricKeys = allowedMetricKeys;
    }

    @Override
    public boolean apply(Measure input) {
      return allowedMetricKeys.contains(input.getMetricKey());
    }
  }

  private static final class MetricToKey implements Function<Metric, String> {
    @Override
    public String apply(Metric input) {
      return input.key();
    }
  }

  private final BatchComponentCache resourceCache;
  private final MeasureCache measureCache;
  private final BatchMetrics batchMetrics;

  public MeasuresPublisher(BatchComponentCache resourceCache, MeasureCache measureCache, BatchMetrics batchMetrics) {
    this.resourceCache = resourceCache;
    this.measureCache = measureCache;
    this.batchMetrics = batchMetrics;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    final Set<String> allowedMetricKeys = newHashSet(transform(batchMetrics.getMetrics(), new MetricToKey()));
    for (final BatchComponent resource : resourceCache.all()) {
      Iterable<Measure> batchMeasures = measureCache.byResource(resource.resource());
      Iterable<org.sonar.batch.protocol.output.BatchReport.Measure> reportMeasures = transform(
        filter(batchMeasures, new IsMetricAllowed(allowedMetricKeys)),
        new MeasureToReportMeasure(resource));
      writer.writeComponentMeasures(resource.batchId(), reportMeasures);
    }
  }

}
