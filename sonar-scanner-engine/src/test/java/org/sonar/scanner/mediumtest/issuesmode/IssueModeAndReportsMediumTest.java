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
package org.sonar.scanner.mediumtest.issuesmode;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.batch.bootstrapper.IssueListener;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.scanner.scan.report.ConsoleReport;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueModeAndReportsMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @org.junit.Rule
  public LogTester logTester = new LogTester();

  private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

  private static Long date(String date) {
    try {
      return sdf.parse(date).getTime();
    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  public ScannerMediumTester tester = ScannerMediumTester.builder()
    .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES))
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", null, "xoo")
    .addActiveRule("xoo", "OneIssueOnDirPerFile", null, "OneIssueOnDirPerFile", "MAJOR", null, "xoo")
    .addActiveRule("xoo", "OneIssuePerModule", null, "OneIssuePerModule", "MAJOR", null, "xoo")
    .setPreviousAnalysisDate(new Date())
    // Existing issue that is still detected
    .mockServerIssue(ServerIssue.newBuilder().setKey("xyz")
      .setModuleKey("sample")
      .setPath("xources/hello/HelloJava.xoo")
      .setRuleRepository("xoo")
      .setRuleKey("OneIssuePerLine")
      .setLine(1)
      .setSeverity(Severity.MAJOR)
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("packagehello;"))
      .setStatus("OPEN")
      .build())
    // Existing issue that is no more detected (will be closed)
    .mockServerIssue(ServerIssue.newBuilder().setKey("resolved")
      .setModuleKey("sample")
      .setPath("xources/hello/HelloJava.xoo")
      .setRuleRepository("xoo")
      .setRuleKey("OneIssuePerLine")
      .setLine(1)
      .setSeverity(Severity.MAJOR)
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("dontexist"))
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
    .build();

  @Before
  public void prepare() {
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
  public void testIssueTracking() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample");

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .start();

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
    assertThat(newIssues).isEqualTo(16);
    assertThat(openIssues).isEqualTo(2);
    assertThat(resolvedIssue).isEqualTo(1);
    
    // progress report
    String logs = StringUtils.join(logTester.logs(LoggerLevel.INFO), "\n");
    
    assertThat(logs).contains("Performing issue tracking");
    assertThat(logs).contains("6/6 components tracked");

    // assert that original fields of a matched issue are kept
    assertThat(result.trackedIssues()).haveExactly(1, new Condition<TrackedIssue>() {
      @Override
      public boolean matches(TrackedIssue value) {
        return value.isNew() == false
          && "resolved-on-project".equals(value.key())
          && "OPEN".equals(value.status())
          && new Date(date("14/03/2004")).equals(value.creationDate());
      }
    });
  }

  @Test
  public void testConsoleReport() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample");

    tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.issuesReport.console.enable", "true")
      .start();

    assertThat(getReportLog()).contains("+16 issues", "+16 major");
  }

  @Test
  public void testPostJob() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample");

    tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.xoo.enablePostJob", "true")
      .start();

    assertThat(logTester.logs()).contains("Resolved issues: 1", "Open issues: 18");
  }

  private String getReportLog() {
    for (String log : logTester.logs()) {
      if (log.contains(ConsoleReport.HEADER)) {
        return log;
      }
    }
    throw new IllegalStateException("No console report");
  }

  @Test
  public void testHtmlReport() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample");

    tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.issuesReport.html.enable", "true")
      .start();

    assertThat(new File(projectDir, ".sonar/issues-report/issues-report.html")).exists();
    assertThat(new File(projectDir, ".sonar/issues-report/issues-report-light.html")).exists();
  }

  @Test
  public void testHtmlReportNoFile() throws Exception {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "sample")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.issuesReport.html.enable", "true")
        .build())
      .start();

    assertThat(FileUtils.readFileToString(new File(baseDir, ".sonar/issues-report/issues-report.html"))).contains("No file analyzed");
    assertThat(FileUtils.readFileToString(new File(baseDir, ".sonar/issues-report/issues-report-light.html"))).contains("No file analyzed");
  }

  @Test
  public void testIssueCallback() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample");
    IssueRecorder issueListener = new IssueRecorder();

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .setIssueListener(issueListener)
      .property("sonar.verbose", "true")
      .start();

    assertThat(result.trackedIssues()).hasSize(19);
    assertThat(issueListener.issueList).hasSize(19);
  }

  private class IssueRecorder implements IssueListener {
    List<Issue> issueList = new LinkedList<>();

    @Override
    public void handle(Issue issue) {
      issueList.add(issue);
    }
  }

  @Test
  public void noSyntaxHighlightingInIssuesMode() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xoohighlightingFile = new File(srcDir, "sample.xoo.highlighting");
    FileUtils.write(xooFile, "Sample xoo\ncontent plop");
    FileUtils.write(xoohighlightingFile, "0:10:s\n11:18:k");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .start();

    assertThat(result.getReportReader().hasSyntaxHighlighting(1)).isFalse();
    assertThat(result.getReportReader().hasSyntaxHighlighting(2)).isFalse();
    assertThat(result.getReportReader().hasSyntaxHighlighting(3)).isFalse();
  }

}
