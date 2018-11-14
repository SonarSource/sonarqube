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
package org.sonar.scanner.mediumtest.issues;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.ExternalIssue;
import org.sonar.scanner.protocol.output.ScannerReport.Issue;
import org.sonar.scanner.protocol.output.ScannerReport.IssueType;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.OneExternalIssuePerLineSensor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ExternalIssuesMediumTest {
  @Rule
  public LogTester logs = new LogTester();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin());

  @Test
  public void testOneIssuePerLine() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .property(OneExternalIssuePerLineSensor.ACTIVATE, "true")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues).isEmpty();

    List<ExternalIssue> externalIssues = result.externalIssuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(externalIssues).hasSize(8 /* lines */);

    ExternalIssue issue = externalIssues.get(0);
    assertThat(issue.getTextRange().getStartLine()).isEqualTo(issue.getTextRange().getStartLine());

    assertThat(result.adHocRules()).isEmpty();
  }

  @Test
  public void testOneIssuePerLine_register_ad_hoc_rule() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .property(OneExternalIssuePerLineSensor.ACTIVATE, "true")
      .property(OneExternalIssuePerLineSensor.REGISTER_AD_HOC_RULE, "true")
      .execute();

    assertThat(result.adHocRules()).extracting(
      ScannerReport.AdHocRule::getEngineId,
      ScannerReport.AdHocRule::getRuleId,
      ScannerReport.AdHocRule::getName,
      ScannerReport.AdHocRule::getDescription,
      ScannerReport.AdHocRule::getSeverity,
      ScannerReport.AdHocRule::getType)
      .containsExactlyInAnyOrder(
        tuple(
          OneExternalIssuePerLineSensor.ENGINE_ID,
          OneExternalIssuePerLineSensor.RULE_ID,
          "An ad hoc rule",
          "blah blah",
          Severity.BLOCKER,
          IssueType.BUG));
  }

  @Test
  public void testLoadIssuesFromJsonReport() throws URISyntaxException, IOException {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .property("sonar.externalIssuesReportPaths", "externalIssues.json")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues).isEmpty();

    List<ExternalIssue> externalIssues = result.externalIssuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(externalIssues).hasSize(2);

    // precise issue location
    ExternalIssue issue = externalIssues.get(0);
    assertThat(issue.getFlowCount()).isZero();
    assertThat(issue.getMsg()).isEqualTo("fix the issue here");
    assertThat(issue.getEngineId()).isEqualTo("externalXoo");
    assertThat(issue.getRuleId()).isEqualTo("rule1");
    assertThat(issue.getSeverity()).isEqualTo(Severity.MAJOR);
    assertThat(issue.getEffort()).isEqualTo(50l);
    assertThat(issue.getType()).isEqualTo(IssueType.CODE_SMELL);
    assertThat(issue.getTextRange().getStartLine()).isEqualTo(5);
    assertThat(issue.getTextRange().getEndLine()).isEqualTo(5);
    assertThat(issue.getTextRange().getStartOffset()).isEqualTo(3);
    assertThat(issue.getTextRange().getEndOffset()).isEqualTo(41);

    // location on a line
    issue = externalIssues.get(1);
    assertThat(issue.getFlowCount()).isZero();
    assertThat(issue.getMsg()).isEqualTo("fix the bug here");
    assertThat(issue.getEngineId()).isEqualTo("externalXoo");
    assertThat(issue.getRuleId()).isEqualTo("rule2");
    assertThat(issue.getSeverity()).isEqualTo(Severity.CRITICAL);
    assertThat(issue.getType()).isEqualTo(IssueType.BUG);
    assertThat(issue.getEffort()).isZero();
    assertThat(issue.getTextRange().getStartLine()).isEqualTo(3);
    assertThat(issue.getTextRange().getEndLine()).isEqualTo(3);
    assertThat(issue.getTextRange().getStartOffset()).isEqualTo(0);
    assertThat(issue.getTextRange().getEndOffset()).isEqualTo(24);

    // One file-level issue in helloscala, with secondary location
    List<ExternalIssue> externalIssues2 = result.externalIssuesFor(result.inputFile("xources/hello/helloscala.xoo"));
    assertThat(externalIssues2).hasSize(1);

    issue = externalIssues2.iterator().next();
    assertThat(issue.getFlowCount()).isEqualTo(2);
    assertThat(issue.getMsg()).isEqualTo("fix the bug here");
    assertThat(issue.getEngineId()).isEqualTo("externalXoo");
    assertThat(issue.getRuleId()).isEqualTo("rule3");
    assertThat(issue.getSeverity()).isEqualTo(Severity.MAJOR);
    assertThat(issue.getType()).isEqualTo(IssueType.BUG);
    assertThat(issue.hasTextRange()).isFalse();
    assertThat(issue.getFlow(0).getLocationCount()).isOne();
    assertThat(issue.getFlow(0).getLocation(0).getTextRange().getStartLine()).isOne();
    assertThat(issue.getFlow(1).getLocationCount()).isOne();
    assertThat(issue.getFlow(1).getLocation(0).getTextRange().getStartLine()).isEqualTo(3);

    // one issue is located in a non-existing file
    assertThat(logs.logs()).contains("External issues ignored for 1 unknown files, including: invalidFile");

  }
}
