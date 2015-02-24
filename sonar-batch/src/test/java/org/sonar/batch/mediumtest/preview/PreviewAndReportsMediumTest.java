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
package org.sonar.batch.mediumtest.preview;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.issue.Issue;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.batch.protocol.Constants.Severity;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class PreviewAndReportsMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

  private static Long date(String date) {
    try {
      return sdf.parse(date).getTime();
    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  public BatchMediumTester tester = BatchMediumTester.builder()
    .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PREVIEW))
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .activateRule(new ActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", null, "xoo"))
    .activateRule(new ActiveRule("manual", "MyManualIssue", null, "My manual issue", "MAJOR", null, null))
    .setPreviousAnalysisDate(new Date())
    // Existing issue
    .mockServerIssue(org.sonar.batch.protocol.input.BatchInput.ServerIssue.newBuilder().setKey("xyz")
      .setComponentKey("sample:xources/hello/HelloJava.xoo")
      .setRuleRepository("xoo")
      .setRuleKey("OneIssuePerLine")
      .setLine(1)
      .setSeverity(Severity.MAJOR)
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("packagehello;"))
      .setStatus("OPEN")
      .build())
    // Resolved issue
    .mockServerIssue(org.sonar.batch.protocol.input.BatchInput.ServerIssue.newBuilder().setKey("resolved")
      .setComponentKey("sample:xources/hello/HelloJava.xoo")
      .setRuleRepository("xoo")
      .setRuleKey("OneIssuePerLine")
      .setLine(1)
      .setSeverity(Severity.MAJOR)
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("dontexist"))
      .setStatus("OPEN")
      .build())
    // Manual issue
    .mockServerIssue(org.sonar.batch.protocol.input.BatchInput.ServerIssue.newBuilder().setKey("manual")
      .setComponentKey("sample:xources/hello/HelloJava.xoo")
      .setRuleRepository("manual")
      .setRuleKey("MyManualIssue")
      .setLine(1)
      .setSeverity(Severity.MAJOR)
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("packagehello;"))
      .setStatus("OPEN")
      .build())
    .build();

  @Before
  public void prepare() {
    tester.start();
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Test
  public void testIssueTracking() throws Exception {
    File projectDir = new File(PreviewAndReportsMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .start();

    int newIssues = 0;
    int openIssues = 0;
    int resolvedIssue = 0;
    for (Issue issue : result.issues()) {
      if (issue.isNew()) {
        newIssues++;
      } else if (issue.resolution() != null) {
        resolvedIssue++;
      } else {
        openIssues++;
      }
    }
    assertThat(newIssues).isEqualTo(13);
    assertThat(openIssues).isEqualTo(2);
    assertThat(resolvedIssue).isEqualTo(1);
  }

  @Test
  public void testConsoleReport() throws Exception {
    File projectDir = new File(PreviewAndReportsMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.issuesReport.console.enable", "true")
      .start();

    // TODO wait for ability to assert on logs
  }

  @Test
  public void testHtmlReport() throws Exception {
    File projectDir = new File(PreviewAndReportsMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.issuesReport.html.enable", "true")
      .start();

    assertThat(new File(projectDir, ".sonar/issues-report/issues-report.html")).exists();
    assertThat(new File(projectDir, ".sonar/issues-report/issues-report-light.html")).exists();
  }

}
