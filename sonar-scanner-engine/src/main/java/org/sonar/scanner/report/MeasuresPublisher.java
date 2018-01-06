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
package org.sonar.scanner.report;

import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase.Status;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.BoolValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.DoubleValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.IntValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.LongValue;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.StringValue;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scan.measure.MeasureCache;

import static org.sonar.api.measures.CoreMetrics.CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER_KEY;
import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS;
import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME_KEY;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES_KEY;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_CONDITIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES_KEY;

public class MeasuresPublisher implements ReportPublisherStep {

  private final InputComponentStore componentStore;
  private final MeasureCache measureCache;
  private final TestPlanBuilder testPlanBuilder;

  public MeasuresPublisher(InputComponentStore componentStore, MeasureCache measureCache, TestPlanBuilder testPlanBuilder) {
    this.componentStore = componentStore;
    this.measureCache = measureCache;
    this.testPlanBuilder = testPlanBuilder;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    final ScannerReport.Measure.Builder builder = ScannerReport.Measure.newBuilder();

    for (final InputComponent c : componentStore.all()) {
      DefaultInputComponent component = (DefaultInputComponent) c;
      if (component.isFile()) {
        DefaultInputFile file = (DefaultInputFile) component;
        // Recompute all coverage measures from line data to take into account the possible merge of several reports
        updateCoverageFromLineData(file);
        // Recompute test execution measures from MutableTestPlan to take into account the possible merge of several reports
        updateTestExecutionFromTestPlan(file);
      }

      Iterable<DefaultMeasure<?>> scannerMeasures = measureCache.byComponentKey(component.key());
      if (scannerMeasures.iterator().hasNext()) {
        writer.writeComponentMeasures(component.batchId(), StreamSupport.stream(scannerMeasures.spliterator(), false)
          .map(input -> {
            if (input.value() == null) {
              throw new IllegalArgumentException(
                String.format("Measure on metric '%s' and component '%s' has no value, but it's not allowed", input.metric().key(), component.key()));
            }
            builder.clear();
            builder.setMetricKey(input.metric().key());
            setValueAccordingToType(builder, input);
            return builder.build();
          }).collect(Collectors.toList()));
      }
    }
  }

  private static void setValueAccordingToType(ScannerReport.Measure.Builder builder, DefaultMeasure<?> measure) {
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

  private void updateTestExecutionFromTestPlan(final InputFile inputFile) {
    final MutableTestPlan testPlan = testPlanBuilder.getTestPlanByFile(inputFile);
    if (testPlan == null || Iterables.isEmpty(testPlan.testCases())) {
      return;
    }
    long nonSkippedTests = StreamSupport.stream(testPlan.testCases().spliterator(), false).filter(t -> t.status() != Status.SKIPPED).count();
    measureCache.put(inputFile.key(), TESTS_KEY, new DefaultMeasure<Integer>().forMetric(TESTS).withValue((int) nonSkippedTests));
    long executionTime = StreamSupport.stream(testPlan.testCases().spliterator(), false).mapToLong(t -> t.durationInMs() != null ? t.durationInMs().longValue() : 0L).sum();
    measureCache.put(inputFile.key(), TEST_EXECUTION_TIME_KEY, new DefaultMeasure<Long>().forMetric(TEST_EXECUTION_TIME).withValue(executionTime));
    long errorTests = StreamSupport.stream(testPlan.testCases().spliterator(), false).filter(t -> t.status() == Status.ERROR).count();
    measureCache.put(inputFile.key(), TEST_ERRORS_KEY, new DefaultMeasure<Integer>().forMetric(TEST_ERRORS).withValue((int) errorTests));
    long skippedTests = StreamSupport.stream(testPlan.testCases().spliterator(), false).filter(t -> t.status() == Status.SKIPPED).count();
    measureCache.put(inputFile.key(), SKIPPED_TESTS_KEY, new DefaultMeasure<Integer>().forMetric(SKIPPED_TESTS).withValue((int) skippedTests));
    long failedTests = StreamSupport.stream(testPlan.testCases().spliterator(), false).filter(t -> t.status() == Status.FAILURE).count();
    measureCache.put(inputFile.key(), TEST_FAILURES_KEY, new DefaultMeasure<Integer>().forMetric(TEST_FAILURES).withValue((int) failedTests));
  }

  private void updateCoverageFromLineData(final InputFile inputFile) {
    if (inputFile.type() != Type.MAIN) {
      return;
    }
    DefaultMeasure<String> lineHitsMeasure = (DefaultMeasure<String>) measureCache.byMetric(inputFile.key(), CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY);
    if (lineHitsMeasure != null) {
      Map<Integer, Integer> lineHits = KeyValueFormat.parseIntInt(lineHitsMeasure.value());
      measureCache.put(inputFile.key(), LINES_TO_COVER_KEY, new DefaultMeasure<Integer>().forMetric(LINES_TO_COVER).withValue(lineHits.keySet().size()));
      measureCache.put(inputFile.key(), UNCOVERED_LINES_KEY,
        new DefaultMeasure<Integer>().forMetric(UNCOVERED_LINES).withValue((int) lineHits.values()
          .stream()
          .filter(hit -> hit == 0)
          .count()));
    }
    DefaultMeasure<String> conditionsMeasure = (DefaultMeasure<String>) measureCache.byMetric(inputFile.key(), CoreMetrics.CONDITIONS_BY_LINE_KEY);
    DefaultMeasure<String> coveredConditionsMeasure = (DefaultMeasure<String>) measureCache.byMetric(inputFile.key(), CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY);
    if (conditionsMeasure != null) {
      Map<Integer, Integer> conditions = KeyValueFormat.parseIntInt(conditionsMeasure.value());
      Map<Integer, Integer> coveredConditions = coveredConditionsMeasure != null ? KeyValueFormat.parseIntInt(coveredConditionsMeasure.value()) : Collections.emptyMap();
      measureCache.put(inputFile.key(), CONDITIONS_TO_COVER_KEY, new DefaultMeasure<Integer>().forMetric(CONDITIONS_TO_COVER).withValue(conditions
        .values()
        .stream()
        .mapToInt(Integer::intValue)
        .sum()));
      measureCache.put(inputFile.key(), UNCOVERED_CONDITIONS_KEY,
        new DefaultMeasure<Integer>().forMetric(UNCOVERED_CONDITIONS)
          .withValue((int) conditions.keySet()
            .stream()
            .mapToInt(line -> conditions.get(line) - coveredConditions.get(line))
            .sum()));
    }
  }

}
