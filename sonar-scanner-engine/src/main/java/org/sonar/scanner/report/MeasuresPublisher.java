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
package org.sonar.scanner.report;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.io.Serializable;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.measures.Measure;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.BoolValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.DoubleValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.IntValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.LongValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.StringValue;
import org.sonar.scanner.scan.measure.MeasureCache;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;

public class MeasuresPublisher implements ReportPublisherStep {

  private static final class MeasureToReportMeasure implements Function<Measure, ScannerReport.Measure> {
    private final BatchComponent resource;
    private final ScannerReport.Measure.Builder builder = ScannerReport.Measure.newBuilder();

    private MeasureToReportMeasure(BatchComponent resource) {
      this.resource = resource;
    }

    @Override
    public ScannerReport.Measure apply(@Nonnull Measure input) {
      validateMeasure(input, resource.key());
      return toReportMeasure(builder, input);
    }

    private static void validateMeasure(Measure measure, String componentKey) {
      if (measure.getValue() == null && measure.getData() == null) {
        throw new IllegalArgumentException(String.format("Measure on metric '%s' and component '%s' has no value, but it's not allowed", measure.getMetricKey(), componentKey));
      }
    }

    private ScannerReport.Measure toReportMeasure(ScannerReport.Measure.Builder builder, Measure measure) {
      builder.clear();
      builder.setMetricKey(measure.getMetricKey());
      setValueAccordingToType(builder, measure);
      return builder.build();
    }

    private void setValueAccordingToType(ScannerReport.Measure.Builder builder, Measure measure) {
      Serializable value = measure.value();
      String data = measure.getData() != null ? measure.getData() : "";
      switch (measure.getMetric().getType()) {
        case INT:
        case RATING:
          builder.setIntValue(IntValue.newBuilder().setValue(((Number) value).intValue()).setData(data));
          break;
        case FLOAT:
        case PERCENT:
          builder.setDoubleValue(DoubleValue.newBuilder().setValue(((Number) value).doubleValue()).setData(data));
          break;
        case BOOL:
          builder.setBooleanValue(BoolValue.newBuilder().setValue(((Boolean) value).booleanValue()).setData(data));
          break;
        case WORK_DUR:
        case MILLISEC:
          builder.setLongValue(LongValue.newBuilder().setValue(((Number) value).longValue()).setData(data));
          break;
        case STRING:
        case DATA:
        case LEVEL:
        case DISTRIB:
          builder.setStringValue(StringValue.newBuilder().setValue((String) value));
          break;
        default:
          throw new IllegalStateException("Unknown metric type: " + measure.getMetric().getType());
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
  private final ScannerMetrics scannerMetrics;

  public MeasuresPublisher(BatchComponentCache resourceCache, MeasureCache measureCache, ScannerMetrics scannerMetrics) {
    this.resourceCache = resourceCache;
    this.measureCache = measureCache;
    this.scannerMetrics = scannerMetrics;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    final Set<String> allowedMetricKeys = newHashSet(transform(scannerMetrics.getMetrics(), new MetricToKey()));
    for (final BatchComponent resource : resourceCache.all()) {
      Iterable<Measure> batchMeasures = measureCache.byResource(resource.resource());
      Iterable<org.sonar.scanner.protocol.output.ScannerReport.Measure> reportMeasures = transform(
        filter(batchMeasures, new IsMetricAllowed(allowedMetricKeys)),
        new MeasureToReportMeasure(resource));
      writer.writeComponentMeasures(resource.batchId(), reportMeasures);
    }
  }

}
