/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.batch.bootstrapper.IssueListener;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuesIssuesModeMediumTest {
  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public ScannerMediumTester testerPreview = ScannerMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES))
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo")
    .setLastBuildDate(new Date())
    .build();

  @Before
  public void prepare() {
    testerPreview.start();
  }

  @After
  public void stop() {
    testerPreview.stop();
  }

  @Test
  public void testIssueCallback() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());
    File tmpDir = temp.getRoot();
    FileUtils.copyDirectory(projectDir, tmpDir);
    IssueRecorder issueListener = new IssueRecorder();

    TaskResult result1 = testerPreview
      .newScanTask(new File(tmpDir, "sonar-project.properties"))
      .setIssueListener(issueListener)
      .property(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES)
      .start();

    assertThat(result1.trackedIssues()).hasSize(14);
    assertThat(issueListener.issueList).hasSize(14);
    issueListener = new IssueRecorder();

    TaskResult result2 = testerPreview
      .newScanTask(new File(tmpDir, "sonar-project.properties"))
      .setIssueListener(issueListener)
      .property(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES)
      .start();

    assertThat(result2.trackedIssues()).hasSize(14);
    assertThat(issueListener.issueList).hasSize(14);
  }

  private class IssueRecorder implements IssueListener {
    List<Issue> issueList = new LinkedList<>();

    @Override
    public void handle(Issue issue) {
      issueList.add(issue);
    }
  }
}
