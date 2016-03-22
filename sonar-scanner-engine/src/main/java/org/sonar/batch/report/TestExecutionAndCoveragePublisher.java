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
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.test.CoverageBlock;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.test.DefaultTestable;
import org.sonar.batch.test.TestPlanBuilder;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.CoverageDetail;
import org.sonar.scanner.protocol.output.ScannerReport.Test;
import org.sonar.scanner.protocol.output.ScannerReport.Test.TestStatus;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

public class TestExecutionAndCoveragePublisher implements ReportPublisherStep {

  private static final class TestConverter implements Function<MutableTestCase, ScannerReport.Test> {
    private final Set<String> testNamesWithCoverage;
    private ScannerReport.Test.Builder builder = ScannerReport.Test.newBuilder();

    private TestConverter(Set<String> testNamesWithCoverage) {
      this.testNamesWithCoverage = testNamesWithCoverage;
    }

    @Override
    public Test apply(@Nonnull MutableTestCase testCase) {
      builder.clear();
      builder.setName(testCase.name());
      if (testCase.doesCover()) {
        testNamesWithCoverage.add(testCase.name());
      }
      Long durationInMs = testCase.durationInMs();
      if (durationInMs != null) {
        builder.setDurationInMs(durationInMs.longValue());
      }
      String msg = testCase.message();
      if (msg != null) {
        builder.setMsg(msg);
      }
      String stack = testCase.stackTrace();
      if (stack != null) {
        builder.setStacktrace(stack);
      }
      TestCase.Status status = testCase.status();
      if (status != null) {
        builder.setStatus(TestStatus.valueOf(status.name()));
      }
      return builder.build();
    }
  }

  private final class TestCoverageConverter implements Function<String, CoverageDetail> {
    private final MutableTestPlan testPlan;
    private ScannerReport.CoverageDetail.Builder builder = ScannerReport.CoverageDetail.newBuilder();
    private ScannerReport.CoverageDetail.CoveredFile.Builder coveredBuilder = ScannerReport.CoverageDetail.CoveredFile.newBuilder();

    private TestCoverageConverter(MutableTestPlan testPlan) {
      this.testPlan = testPlan;
    }

    @Override
    public CoverageDetail apply(@Nonnull String testName) {
      // Take first test with provided name
      MutableTestCase testCase = testPlan.testCasesByName(testName).iterator().next();
      builder.clear();
      builder.setTestName(testName);
      for (CoverageBlock block : testCase.coverageBlocks()) {
        coveredBuilder.clear();
        coveredBuilder.setFileRef(componentCache.get(((DefaultTestable) block.testable()).inputFile().key()).batchId());
        for (int line : block.lines()) {
          coveredBuilder.addCoveredLine(line);
        }
        builder.addCoveredFile(coveredBuilder.build());
      }
      return builder.build();
    }
  }

  private final BatchComponentCache componentCache;
  private final TestPlanBuilder testPlanBuilder;

  public TestExecutionAndCoveragePublisher(BatchComponentCache resourceCache, TestPlanBuilder testPlanBuilder) {
    this.componentCache = resourceCache;
    this.testPlanBuilder = testPlanBuilder;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    for (final BatchComponent component : componentCache.all()) {
      if (!component.isFile()) {
        continue;
      }

      DefaultInputFile inputFile = (DefaultInputFile) component.inputComponent();
      if (inputFile.type() != Type.TEST) {
        continue;
      }

      final MutableTestPlan testPlan = testPlanBuilder.loadPerspective(MutableTestPlan.class, component);
      if (testPlan == null || Iterables.isEmpty(testPlan.testCases())) {
        continue;
      }

      final Set<String> testNamesWithCoverage = new HashSet<>();

      writer.writeTests(component.batchId(), Iterables.transform(testPlan.testCases(), new TestConverter(testNamesWithCoverage)));

      writer.writeCoverageDetails(component.batchId(), Iterables.transform(testNamesWithCoverage, new TestCoverageConverter(testPlan)));
    }
  }
}
