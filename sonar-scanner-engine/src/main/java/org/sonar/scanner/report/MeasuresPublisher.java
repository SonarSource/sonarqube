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
import java.io.Serializable;
import javax.annotation.Nonnull;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.core.metric.ScannerMetrics;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.BoolValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.DoubleValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.IntValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.LongValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.StringValue;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.measure.MeasureCache;

import static com.google.common.collect.Iterables.transform;

public class MeasuresPublisher implements ReportPublisherStep {

  private static final class MeasureToReportMeasure implements Function<DefaultMeasure, ScannerReport.Measure> {
    private final BatchComponent resource;
    private final ScannerReport.Measure.Builder builder = ScannerReport.Measure.newBuilder();

    private MeasureToReportMeasure(BatchComponent resource) {
      this.resource = resource;
    }

    @Override
    public ScannerReport.Measure apply(@Nonnull DefaultMeasure input) {
      validateMeasure(input, resource.key());
      return toReportMeasure(builder, input);
    }

    private static void validateMeasure(DefaultMeasure measure, String componentKey) {
      if (measure.value() == null) {
        throw new IllegalArgumentException(String.format("Measure on metric '%s' and component '%s' has no value, but it's not allowed", measure.metric().key(), componentKey));
      }
    }

    private static ScannerReport.Measure toReportMeasure(ScannerReport.Measure.Builder builder, DefaultMeasure measure) {
      builder.clear();
      builder.setMetricKey(measure.metric().key());
      setValueAccordingToType(builder, measure);
      return builder.build();
    }

    private static void setValueAccordingToType(ScannerReport.Measure.Builder builder, DefaultMeasure measure) {
      Serializable value = measure.value();
      Metric<?> metric = measure.metric();
      if (Boolean.class.equals(metric.valueType())) {
        builder.setBooleanValue(BoolValue.newBuilder().setValue(((Boolean) value).booleanValue()));
      } else if (Integer.class.equals(metric.valueType())) {
        builder.setIntValue(IntValue.newBuilder().setValue(((Number) value).intValue()));
      } else if (Double.class.equals(metric.valueType())) {
        builder.setDoubleValue(DoubleValue.newBuilder().setValue(((Number) value).doubleValue()));
      } else if (String.class.equals(metric.valueType())) {
        builder.setStringValue(StringValue.newBuilder().setValue((String) value));
      } else if (Long.class.equals(metric.valueType())) {
        builder.setLongValue(LongValue.newBuilder().setValue(((Number) value).longValue()));
      } else {
        throw new UnsupportedOperationException("Unsupported type :" + metric.valueType());
      }
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
    for (final BatchComponent resource : resourceCache.all()) {
      Iterable<DefaultMeasure<?>> scannerMeasures = measureCache.byComponentKey(resource.key());
      Iterable<ScannerReport.Measure> reportMeasures = transform(scannerMeasures, new MeasureToReportMeasure(resource));
      writer.writeComponentMeasures(resource.batchId(), reportMeasures);
    }
  }

}
