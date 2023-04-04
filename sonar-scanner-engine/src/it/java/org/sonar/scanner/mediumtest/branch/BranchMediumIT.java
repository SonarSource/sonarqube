/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.mediumtest.branch;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;
import org.sonarqube.ws.NewCodePeriods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BranchMediumIT {

  private static final String PROJECT_KEY = "sample";
  private static final String FILE_PATH = "HelloJava.xoo";
  private static final String FILE_CONTENT = "xoooo";
  public static final String ONE_ISSUE_PER_LINE_IS_RESTRICTED_TO_CHANGED_FILES_ONLY = "Sensor One Issue Per Line is restricted to changed files only";
  private File baseDir;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo");

  @Before
  public void prepare() throws IOException {
    logTester.setLevel(Level.DEBUG);
    baseDir = temp.newFolder();
    Path filepath = baseDir.toPath().resolve(FILE_PATH);
    Files.write(filepath, FILE_CONTENT.getBytes());

    Path xooUtCoverageFile = baseDir.toPath().resolve(FILE_PATH + ".coverage");
    FileUtils.write(xooUtCoverageFile.toFile(), "1:2:2:1", StandardCharsets.UTF_8);

    String md5sum = new FileMetadata(mock(AnalysisWarnings.class))
      .readMetadata(Files.newInputStream(filepath), StandardCharsets.UTF_8, FILE_PATH)
      .hash();
    tester.addFileData(FILE_PATH, new FileData(md5sum, "1.1"));
    tester.setNewCodePeriod(NewCodePeriods.NewCodePeriodType.PREVIOUS_VERSION, "");
  }

  @Test
  public void should_not_skip_report_for_unchanged_files_in_pr() {
    // sanity check, normally report gets generated
    AnalysisResult result = getResult(tester);
    final DefaultInputFile file = (DefaultInputFile) result.inputFile(FILE_PATH);
    assertThat(getResult(tester).getReportComponent(file)).isNotNull();
    int fileId = file.scannerId();
    assertThat(result.getReportReader().readChangesets(fileId)).isNotNull();
    assertThat(result.getReportReader().hasCoverage(fileId)).isTrue();
    assertThat(result.getReportReader().readFileSource(fileId)).isNotNull();

    // file is not skipped for pull requests (need coverage, duplications coming soon)
    AnalysisResult result2 = getResult(tester.setBranchType(BranchType.PULL_REQUEST));
    final DefaultInputFile fileInPr = (DefaultInputFile) result2.inputFile(FILE_PATH);
    assertThat(result2.getReportComponent(fileInPr)).isNotNull();
    fileId = fileInPr.scannerId();
    assertThat(result2.getReportReader().readChangesets(fileId)).isNull();
    assertThat(result2.getReportReader().hasCoverage(fileId)).isTrue();
    assertThat(result2.getReportReader().readFileSource(fileId)).isNull();
  }

  @Test
  public void shouldSkipSensorForUnchangedFilesOnPr() {
    AnalysisResult result = getResult(tester
            .setBranchName("myBranch")
            .setBranchTarget("main")
            .setBranchType(BranchType.PULL_REQUEST));
    final DefaultInputFile file = (DefaultInputFile) result.inputFile(FILE_PATH);

    List<ScannerReport.Issue> issues = result.issuesFor(file);
    assertThat(issues).isEmpty();

    assertThat(logTester.logs()).contains(ONE_ISSUE_PER_LINE_IS_RESTRICTED_TO_CHANGED_FILES_ONLY);
  }

  @Test
  public void shouldNotSkipSensorForUnchangedFilesOnBranch() throws Exception {
    AnalysisResult result = getResult(tester
            .setBranchName("myBranch")
            .setBranchTarget("main")
            .setBranchType(BranchType.BRANCH));
    final DefaultInputFile file = (DefaultInputFile) result.inputFile(FILE_PATH);

    List<ScannerReport.Issue> issues = result.issuesFor(file);
    assertThat(issues).isNotEmpty();

    assertThat(logTester.logs()).doesNotContain(ONE_ISSUE_PER_LINE_IS_RESTRICTED_TO_CHANGED_FILES_ONLY);
  }

  @Test
  public void verify_metadata() {
    String branchName = "feature";
    String branchTarget = "branch-1.x";

    AnalysisResult result = getResult(tester
      .setBranchName(branchName)
      .setBranchTarget(branchTarget)
      .setReferenceBranchName(branchTarget)
      .setBranchType(BranchType.BRANCH));

    ScannerReport.Metadata metadata = result.getReportReader().readMetadata();
    assertThat(metadata.getBranchName()).isEqualTo(branchName);
    assertThat(metadata.getBranchType()).isEqualTo(ScannerReport.Metadata.BranchType.BRANCH);
    assertThat(metadata.getReferenceBranchName()).isEqualTo(branchTarget);
  }

  private AnalysisResult getResult(ScannerMediumTester tester) {
    return tester
      .newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", PROJECT_KEY)
        .put("sonar.scm.provider", "xoo")
        .build())
      .execute();
  }
}
