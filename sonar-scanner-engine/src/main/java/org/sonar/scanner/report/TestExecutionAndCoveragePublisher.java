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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.scanner.deprecated.test.DefaultTestable;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.CoverageDetail;
import org.sonar.scanner.protocol.output.ScannerReport.Test;
import org.sonar.scanner.protocol.output.ScannerReport.Test.TestStatus;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static java.util.stream.Collectors.toList;

public class TestExecutionAndCoveragePublisher implements ReportPublisherStep {

  private final InputComponentStore componentStore;
  private final TestPlanBuilder testPlanBuilder;
  private final BranchConfiguration branchConfiguration;

  public TestExecutionAndCoveragePublisher(InputComponentStore componentStore, TestPlanBuilder testPlanBuilder, BranchConfiguration branchConfiguration) {
    this.componentStore = componentStore;
    this.testPlanBuilder = testPlanBuilder;
    this.branchConfiguration = branchConfiguration;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    if (branchConfiguration.isShortLivingBranch()) {
      return;
    }
    final ScannerReport.Test.Builder testBuilder = ScannerReport.Test.newBuilder();
    final ScannerReport.CoverageDetail.Builder builder = ScannerReport.CoverageDetail.newBuilder();
    final ScannerReport.CoverageDetail.CoveredFile.Builder coveredBuilder = ScannerReport.CoverageDetail.CoveredFile.newBuilder();
    for (final InputComponent c : componentStore.all()) {
      DefaultInputComponent component = (DefaultInputComponent) c;
      final MutableTestPlan testPlan = testPlanBuilder.loadPerspective(MutableTestPlan.class, component);
      if (testPlan == null || Iterables.isEmpty(testPlan.testCases())) {
        continue;
      }

      final Set<String> testNamesWithCoverage = new HashSet<>();

      writer.writeTests(component.batchId(),
        StreamSupport.stream(testPlan.testCases().spliterator(), false)
          .map(testCase -> toProtobufTest(testBuilder, testNamesWithCoverage, testCase))
          .collect(toList()));

      writer.writeCoverageDetails(component.batchId(), testNamesWithCoverage.stream()
        .map(testName -> toProtobufCoverageDetails(builder, coveredBuilder, testPlan, testName))
        .collect(toList()));
    }
  }

  private CoverageDetail toProtobufCoverageDetails(final ScannerReport.CoverageDetail.Builder builder, final ScannerReport.CoverageDetail.CoveredFile.Builder coveredBuilder,
    final MutableTestPlan testPlan, String testName) {
    // Take first test with provided name
    MutableTestCase testCase = testPlan.testCasesByName(testName).iterator().next();
    builder.clear();
    builder.setTestName(testName);
    for (CoverageBlock block : testCase.coverageBlocks()) {
      coveredBuilder.clear();
      DefaultInputComponent c = (DefaultInputComponent) componentStore.getByKey(((DefaultTestable) block.testable()).inputFile().key());
      coveredBuilder.setFileRef(c.batchId());
      for (int line : block.lines()) {
        coveredBuilder.addCoveredLine(line);
      }
      builder.addCoveredFile(coveredBuilder.build());
    }
    return builder.build();
  }

  private static Test toProtobufTest(final ScannerReport.Test.Builder testBuilder, final Set<String> testNamesWithCoverage, MutableTestCase testCase) {
    testBuilder.clear();
    testBuilder.setName(testCase.name());
    if (testCase.doesCover()) {
      testNamesWithCoverage.add(testCase.name());
    }
    Long durationInMs = testCase.durationInMs();
    if (durationInMs != null) {
      testBuilder.setDurationInMs(durationInMs.longValue());
    }
    String msg = testCase.message();
    if (msg != null) {
      testBuilder.setMsg(msg);
    }
    String stack = testCase.stackTrace();
    if (stack != null) {
      testBuilder.setStacktrace(stack);
    }
    TestCase.Status status = testCase.status();
    if (status != null) {
      testBuilder.setStatus(TestStatus.valueOf(status.name()));
    }
    return testBuilder.build();
  }
}
