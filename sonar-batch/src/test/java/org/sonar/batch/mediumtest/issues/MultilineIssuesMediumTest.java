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
package org.sonar.batch.mediumtest.issues;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.batch.protocol.output.BatchReport.Flow;
import org.sonar.batch.protocol.output.BatchReport.Issue;
import org.sonar.batch.protocol.output.BatchReport.IssueLocation;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class MultilineIssuesMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addRules(new XooRulesDefinition())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addActiveRule("xoo", "MultilineIssue", null, "Multinile Issue", "MAJOR", null, "xoo")
    .build();

  private TaskResult result;

  @Before
  public void prepare() throws Exception {
    tester.start();

    File projectDir = new File(MultilineIssuesMediumTest.class.getResource("/mediumtest/xoo/sample-multiline").toURI());
    File tmpDir = temp.getRoot();
    FileUtils.copyDirectory(projectDir, tmpDir);

    result = tester
      .newScanTask(new File(tmpDir, "sonar-project.properties"))
      .start();
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Test
  public void testIssueRange() throws Exception {
    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/Single.xoo"));
    assertThat(issues).hasSize(1);
    Issue issue = issues.get(0);
    assertThat(issue.getLine()).isEqualTo(6);
    assertThat(issue.getMsg()).isEqualTo("Primary location");
    assertThat(issue.getTextRange().getStartLine()).isEqualTo(6);
    assertThat(issue.getTextRange().getStartOffset()).isEqualTo(23);
    assertThat(issue.getTextRange().getEndLine()).isEqualTo(6);
    assertThat(issue.getTextRange().getEndOffset()).isEqualTo(50);
  }

  @Test
  public void testMultilineIssueRange() throws Exception {
    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/Multiline.xoo"));
    assertThat(issues).hasSize(1);
    Issue issue = issues.get(0);
    assertThat(issue.getLine()).isEqualTo(6);
    assertThat(issue.getMsg()).isEqualTo("Primary location");
    assertThat(issue.getTextRange().getStartLine()).isEqualTo(6);
    assertThat(issue.getTextRange().getStartOffset()).isEqualTo(23);
    assertThat(issue.getTextRange().getEndLine()).isEqualTo(7);
    assertThat(issue.getTextRange().getEndOffset()).isEqualTo(23);
  }

  @Test
  public void testFlowWithSingleLocation() throws Exception {
    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/Multiple.xoo"));
    assertThat(issues).hasSize(1);
    Issue issue = issues.get(0);
    assertThat(issue.getLine()).isEqualTo(6);
    assertThat(issue.getMsg()).isEqualTo("Primary location");
    assertThat(issue.getTextRange().getStartLine()).isEqualTo(6);
    assertThat(issue.getTextRange().getStartOffset()).isEqualTo(23);
    assertThat(issue.getTextRange().getEndLine()).isEqualTo(6);
    assertThat(issue.getTextRange().getEndOffset()).isEqualTo(50);

    assertThat(issue.getFlowList()).hasSize(1);
    Flow flow = issue.getFlow(0);
    assertThat(flow.getLocationList()).hasSize(1);
    IssueLocation additionalLocation = flow.getLocation(0);
    assertThat(additionalLocation.getMsg()).isEqualTo("Flow step #1");
    assertThat(additionalLocation.getTextRange().getStartLine()).isEqualTo(7);
    assertThat(additionalLocation.getTextRange().getStartOffset()).isEqualTo(26);
    assertThat(additionalLocation.getTextRange().getEndLine()).isEqualTo(7);
    assertThat(additionalLocation.getTextRange().getEndOffset()).isEqualTo(53);
  }

  @Test
  public void testFlowsWithMultipleElements() throws Exception {
    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/WithFlow.xoo"));
    assertThat(issues).hasSize(1);
    Issue issue = issues.get(0);
    assertThat(issue.getFlowList()).hasSize(1);

    Flow flow = issue.getFlow(0);
    assertThat(flow.getLocationList()).hasSize(2);
    // TODO more assertions
  }
}
