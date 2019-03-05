/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Objects;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.api.test.TestCase.Status;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS;
import static org.sonar.api.measures.CoreMetrics.TESTS;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES;
import static org.sonar.scanner.sensor.DefaultSensorStorage.toReportMeasure;

public class TestExecutionPublisher implements ReportPublisherStep {

  private final InputComponentStore componentStore;
  private final TestPlanBuilder testPlanBuilder;

  public TestExecutionPublisher(InputComponentStore componentStore, TestPlanBuilder testPlanBuilder) {
    this.componentStore = componentStore;
    this.testPlanBuilder = testPlanBuilder;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    final ScannerReport.Measure.Builder builder = ScannerReport.Measure.newBuilder();

    for (final InputComponent c : componentStore.all()) {
      DefaultInputComponent component = (DefaultInputComponent) c;
      if (component.isFile()) {
        DefaultInputFile file = (DefaultInputFile) component;
        // Recompute test execution measures from MutableTestPlan to take into account the possible merge of several reports
        updateTestExecutionFromTestPlan(file, writer);
      }
    }
  }

  private void updateTestExecutionFromTestPlan(final InputFile inputFile, ScannerReportWriter writer) {
    final MutableTestPlan testPlan = testPlanBuilder.getTestPlanByFile(inputFile);
    if (testPlan == null || Iterables.isEmpty(testPlan.testCases())) {
      return;
    }
    long nonSkippedTests = StreamSupport.stream(testPlan.testCases().spliterator(), false).filter(t -> t.status() != Status.SKIPPED).count();
    appendMeasure(inputFile, writer, new DefaultMeasure<Integer>().forMetric(TESTS).withValue((int) nonSkippedTests));
    long executionTime = StreamSupport.stream(testPlan.testCases().spliterator(), false).map(TestCase::durationInMs).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
    appendMeasure(inputFile, writer, new DefaultMeasure<Long>().forMetric(TEST_EXECUTION_TIME).withValue(executionTime));
    long errorTests = StreamSupport.stream(testPlan.testCases().spliterator(), false).filter(t -> t.status() == Status.ERROR).count();
    appendMeasure(inputFile, writer, new DefaultMeasure<Integer>().forMetric(TEST_ERRORS).withValue((int) errorTests));
    long skippedTests = StreamSupport.stream(testPlan.testCases().spliterator(), false).filter(t -> t.status() == Status.SKIPPED).count();
    appendMeasure(inputFile, writer, new DefaultMeasure<Integer>().forMetric(SKIPPED_TESTS).withValue((int) skippedTests));
    long failedTests = StreamSupport.stream(testPlan.testCases().spliterator(), false).filter(t -> t.status() == Status.FAILURE).count();
    appendMeasure(inputFile, writer, new DefaultMeasure<Integer>().forMetric(TEST_FAILURES).withValue((int) failedTests));
  }

  private void appendMeasure(InputFile inputFile, ScannerReportWriter writer, DefaultMeasure measure) {
    writer.appendComponentMeasure(((DefaultInputComponent) inputFile).scannerId(), toReportMeasure(measure));
  }

}
