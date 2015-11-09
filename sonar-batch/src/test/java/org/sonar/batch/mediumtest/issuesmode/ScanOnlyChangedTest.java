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
package org.sonar.batch.mediumtest.issuesmode;

import org.sonar.batch.issue.tracking.TrackedIssue;

import org.assertj.core.api.Condition;
import com.google.common.io.Resources;
import org.sonar.batch.repository.FileData;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.After;
import org.junit.Before;
import org.sonar.api.CoreProperties;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.protocol.Constants.Severity;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.log.LogTester;
import org.junit.Test;
import org.sonar.batch.mediumtest.TaskResult;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanOnlyChangedTest {
  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @org.junit.Rule
  public LogTester logTester = new LogTester();

  private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

  private BatchMediumTester tester;

  private static Long date(String date) {
    try {
      return sdf.parse(date).getTime();
    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  @Before
  public void prepare() throws IOException {
    String filePath = "xources/hello/HelloJava.xoo";
    String md5sum = DigestUtils.md5Hex(FileUtils.readFileToString(new File(
      Resources.getResource("mediumtest/xoo/sample/" + filePath).getPath())));

    tester = BatchMediumTester.builder()
      .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES))
      .registerPlugin("xoo", new XooPlugin())
      .addDefaultQProfile("xoo", "Sonar Way")
      .addRules(new XooRulesDefinition())
      .addRule("manual:MyManualIssue", "manual", "MyManualIssue", "My manual issue")
      .addRule("manual:MyManualIssueDup", "manual", "MyManualIssue", "My manual issue")
      .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", null, "xoo")
      .addActiveRule("xoo", "OneIssueOnDirPerFile", null, "OneIssueOnDirPerFile", "MAJOR", null, "xoo")
      .addActiveRule("xoo", "OneIssuePerModule", null, "OneIssuePerModule", "MAJOR", null, "xoo")
      .addActiveRule("manual", "MyManualIssue", null, "My manual issue", "MAJOR", null, null)
      // this will cause the file to have status==SAME
      .addFileData("sample", filePath, new FileData(md5sum, null))
      .setPreviousAnalysisDate(new Date())
      // Existing issue that is copied
      .mockServerIssue(ServerIssue.newBuilder().setKey("xyz")
        .setModuleKey("sample")
        .setMsg("One issue per Line copied")
        .setPath("xources/hello/HelloJava.xoo")
        .setRuleRepository("xoo")
        .setRuleKey("OneIssuePerLine")
        .setLine(1)
        .setSeverity(Severity.MAJOR)
        .setCreationDate(date("14/03/2004"))
        .setChecksum(DigestUtils.md5Hex("packagehello;"))
        .setStatus("OPEN")
        .build())
      // Existing issue on project that is still detected
      .mockServerIssue(ServerIssue.newBuilder().setKey("resolved-on-project")
        .setModuleKey("sample")
        .setRuleRepository("xoo")
        .setRuleKey("OneIssuePerModule")
        .setSeverity(Severity.CRITICAL)
        .setCreationDate(date("14/03/2004"))
        .setStatus("OPEN")
        .build())
      // Manual issue
      .mockServerIssue(ServerIssue.newBuilder().setKey("manual")
        .setModuleKey("sample")
        .setPath("xources/hello/HelloJava.xoo")
        .setRuleRepository("manual")
        .setRuleKey("MyManualIssue")
        .setLine(1)
        .setSeverity(Severity.MAJOR)
        .setCreationDate(date("14/03/2004"))
        .setChecksum(DigestUtils.md5Hex("packagehello;"))
        .setStatus("OPEN")
        .build())
      .build();
    tester.start();
  }

  @After
  public void stop() {
    tester.stop();
  }

  private File copyProject(String path) throws Exception {
    File projectDir = temp.newFolder();
    File originalProjectDir = new File(IssueModeAndReportsMediumTest.class.getResource(path).toURI());
    FileUtils.copyDirectory(originalProjectDir, projectDir, FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter(".sonar")));
    return projectDir;
  }

  @Test
  public void testScanAll() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample");

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.scanAllFiles", "true")
      .start();

    assertNumberIssues(result, 16, 3, 0);

    /*
     * 8 new per line
     * 1 manual
     */
    assertNumberIssuesOnFile(result, "HelloJava.xoo", 9);
  }

  @Test
  public void testScanOnlyChangedFiles() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample");

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .start();

    /*
     * We have:
     * 6 new issues per line (open) in helloscala.xoo
     * 2 new issues per file in helloscala.xoo / ClassOneTest.xoo
     * 1 server issue (open, not new) copied from server in HelloJava.xoo (this file is unchanged)
     * 1 manual issue (open, not new) in HelloJava.xoo
     * 1 existing issue on the project (open, not new)
     */
    assertNumberIssues(result, 8, 3, 0);

    // should only have server issues (HelloJava.xoo should not have been analyzed)
    assertNumberIssuesOnFile(result, "HelloJava.xoo", 2);
  }

  private static void assertNumberIssuesOnFile(TaskResult result, final String fileNameEndsWith, int issues) {
    assertThat(result.trackedIssues()).haveExactly(issues, new Condition<TrackedIssue>() {
      @Override
      public boolean matches(TrackedIssue value) {
        return value.componentKey().endsWith(fileNameEndsWith);
      }
    });
  }

  private static void assertNumberIssues(TaskResult result, int expectedNew, int expectedOpen, int expectedResolved) {
    int newIssues = 0;
    int openIssues = 0;
    int resolvedIssue = 0;
    for (TrackedIssue issue : result.trackedIssues()) {
      System.out
        .println(issue.getMessage() + " " + issue.key() + " " + issue.getRuleKey() + " " + issue.isNew() + " " + issue.resolution() + " " + issue.componentKey() + " "
          + issue.startLine());
      if (issue.isNew()) {
        newIssues++;
      } else if (issue.resolution() != null) {
        resolvedIssue++;
      } else {
        openIssues++;
      }
    }

    System.out.println("new: " + newIssues + " open: " + openIssues + " resolved " + resolvedIssue);
    assertThat(newIssues).isEqualTo(expectedNew);
    assertThat(openIssues).isEqualTo(expectedOpen);
    assertThat(resolvedIssue).isEqualTo(expectedResolved);
  }
}
