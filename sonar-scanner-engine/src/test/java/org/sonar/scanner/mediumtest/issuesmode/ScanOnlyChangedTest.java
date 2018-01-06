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
package org.sonar.scanner.mediumtest.issuesmode;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.utils.log.LogTester;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.ScannerMediumTester.TaskBuilder;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;
import org.sonar.scanner.repository.FileData;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ScanOnlyChangedTest {
  private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

  /*
   * Run with branch too to reproduce SONAR-9141 and SONAR-9160
   */
  @Parameters(name = "branch enabled:{0} projectKey: {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {false, "sample"}, {true, "sample:branch"}, {false, "sample:project"}
    });
  }

  @Parameter(0)
  public boolean branch;

  @Parameter(1)
  public String projectKey;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES))
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", null, "xoo")
    .addActiveRule("xoo", "OneIssueOnDirPerFile", null, "OneIssueOnDirPerFile", "MAJOR", null, "xoo")
    .addActiveRule("xoo", "OneIssuePerModule", null, "OneIssuePerModule", "MAJOR", null, "xoo");

  @Before
  public void prepare() throws IOException, URISyntaxException {
    String filePath = "xources/hello/HelloJava.xoo";
    Path path = Paths.get(Resources.getResource("mediumtest/xoo/sample/" + filePath).toURI());
    String md5sum = new FileMetadata()
      .readMetadata(Files.newInputStream(path), StandardCharsets.UTF_8, filePath)
      .hash();

    tester
      // this will cause the file to have status==SAME
      .addFileData(projectKey, filePath, new FileData(md5sum, null))
      .setPreviousAnalysisDate(new Date())
      // Existing issue that is copied
      .mockServerIssue(ServerIssue.newBuilder().setKey("xyz")
        .setModuleKey(projectKey)
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
        .setModuleKey(projectKey)
        .setRuleRepository("xoo")
        .setRuleKey("OneIssuePerModule")
        .setSeverity(Severity.CRITICAL)
        .setCreationDate(date("14/03/2004"))
        .setStatus("OPEN")
        .build());
  }

  private File copyProject(String path) throws Exception {
    File projectDir = temp.newFolder();
    File originalProjectDir = new File(IssueModeAndReportsMediumTest.class.getResource(path).toURI());
    FileUtils.copyDirectory(originalProjectDir, projectDir, FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter(".sonar")));
    return projectDir;
  }

  @Test
  public void testScanOnlyChangedFiles() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample");

    TaskBuilder taskBuilder = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.report.export.path", "report.json");
    if (branch) {
      taskBuilder.property("sonar.branch", "branch");
    } else {
      taskBuilder.property("sonar.projectKey", projectKey);
    }

    TaskResult result = taskBuilder.execute();
    /*
     * We have:
     * 6 new issues per line (open) in helloscala.xoo
     * 2 new issues per file in helloscala.xoo / ClassOneTest.xoo
     * 1 server issue (open, not new) copied from server in HelloJava.xoo (this file is unchanged)
     * 1 manual issue (open, not new) in HelloJava.xoo
     * 1 existing issue on the project (open, not new)
     */
    assertNumberIssues(result, 8, 2, 0);

    // should only have server issues (HelloJava.xoo should not have been analyzed)
    assertNumberIssuesOnFile(result, "HelloJava.xoo", 1);
  }

  @Test
  public void testScanAll() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample");

    TaskBuilder taskBuilder = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.scanAllFiles", "true");
    if (branch) {
      taskBuilder.property("sonar.branch", "branch");
    } else {
      taskBuilder.property("sonar.projectKey", projectKey);
    }

    TaskResult result = taskBuilder.execute();

    assertNumberIssues(result, 16, 2, 0);

    /*
     * 8 new per line
     */
    assertNumberIssuesOnFile(result, "HelloJava.xoo", 8);
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

  private static Long date(String date) {
    try {
      return sdf.parse(date).getTime();
    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }
}
