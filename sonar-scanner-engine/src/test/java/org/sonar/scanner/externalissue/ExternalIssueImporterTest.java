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
package org.sonar.scanner.externalissue;

import java.io.File;
import javax.annotation.Nullable;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class ExternalIssueImporterTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public LogTester logs = new LogTester();

  private DefaultInputFile sourceFile;
  private SensorContextTester context;

  @Before
  public void prepare() throws Exception {
    File baseDir = temp.newFolder();
    context = SensorContextTester.create(baseDir);
    sourceFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .setModuleBaseDir(baseDir.toPath())
      .initMetadata("the first line\nthe second line")
      .setCharset(UTF_8)
      .setLanguage("java")
      .build();
    context.fileSystem().add(sourceFile);
  }

  @Test
  public void import_zero_issues() {
    ReportParser.Report report = new ReportParser.Report();
    report.issues = new ReportParser.Issue[0];

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);
    underTest.execute();

    assertThat(context.allExternalIssues()).isEmpty();
    assertThat(context.allIssues()).isEmpty();
    assertThat(logs.logs(LoggerLevel.INFO)).contains("Imported 0 issues in 0 files");
  }

  @Test
  public void import_issue_with_minimal_info() {
    ReportParser.Report report = new ReportParser.Report();
    ReportParser.Issue input = new ReportParser.Issue();
    input.engineId = "findbugs";
    input.ruleId = "123";
    input.severity = "CRITICAL";
    input.type = "BUG";
    input.primaryLocation = new ReportParser.Location();
    input.primaryLocation.filePath = sourceFile.getProjectRelativePath();
    input.primaryLocation.message = randomAlphabetic(5);
    report.issues = new ReportParser.Issue[] {input};

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);
    underTest.execute();

    assertThat(context.allExternalIssues()).hasSize(1);
    ExternalIssue output = context.allExternalIssues().iterator().next();
    assertThat(output.engineId()).isEqualTo(input.engineId);
    assertThat(output.ruleId()).isEqualTo(input.ruleId);
    assertThat(output.severity()).isEqualTo(Severity.valueOf(input.severity));
    assertThat(output.remediationEffort()).isNull();
    assertThat(logs.logs(LoggerLevel.INFO)).contains("Imported 1 issue in 1 file");
  }

  @Test
  public void import_issue_with_complete_primary_location() {
    ReportParser.TextRange input = new ReportParser.TextRange();
    input.startLine = 1;
    input.startColumn = 4;
    input.endLine = 2;
    input.endColumn = 3;

    runOn(newIssue(input));

    assertThat(context.allExternalIssues()).hasSize(1);
    ExternalIssue output = context.allExternalIssues().iterator().next();
    assertSameRange(input, output.primaryLocation().textRange());
  }

  /**
   * If columns are not defined, then issue is assumed to be on the full first line.
   * The end line is ignored.
   */
  @Test
  public void import_issue_with_no_columns() {
    ReportParser.TextRange input = new ReportParser.TextRange();
    input.startLine = 1;
    input.startColumn = null;
    input.endLine = 2;
    input.endColumn = null;

    runOn(newIssue(input));

    assertThat(context.allExternalIssues()).hasSize(1);
    TextRange got = context.allExternalIssues().iterator().next().primaryLocation().textRange();
    assertThat(got.start().line()).isEqualTo(input.startLine);
    assertThat(got.start().lineOffset()).isEqualTo(0);
    assertThat(got.end().line()).isEqualTo(input.startLine);
    assertThat(got.end().lineOffset()).isEqualTo(sourceFile.selectLine(input.startLine).end().lineOffset());
  }

  /**
   * If end column is not defined, then issue is assumed to be until the last character of the end line.
   */
  @Test
  public void import_issue_with_start_but_not_end_column() {
    ReportParser.TextRange input = new ReportParser.TextRange();
    input.startLine = 1;
    input.startColumn = 3;
    input.endLine = 2;
    input.endColumn = null;

    runOn(newIssue(input));

    assertThat(context.allExternalIssues()).hasSize(1);
    TextRange got = context.allExternalIssues().iterator().next().primaryLocation().textRange();
    assertThat(got.start().line()).isEqualTo(input.startLine);
    assertThat(got.start().lineOffset()).isEqualTo(3);
    assertThat(got.end().line()).isEqualTo(input.endLine);
    assertThat(got.end().lineOffset()).isEqualTo(sourceFile.selectLine(input.endLine).end().lineOffset());
  }

  private void assertSameRange(ReportParser.TextRange expected, TextRange got) {
    assertThat(got.start().line()).isEqualTo(expected.startLine);
    assertThat(got.start().lineOffset()).isEqualTo(defaultIfNull(expected.startColumn, 0));
    assertThat(got.end().line()).isEqualTo(expected.endLine);
    assertThat(got.end().lineOffset()).isEqualTo(defaultIfNull(expected.endColumn, 0));
  }

  private void runOn(ReportParser.Issue input) {
    ReportParser.Report report = new ReportParser.Report();
    report.issues = new ReportParser.Issue[] {input};

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);
    underTest.execute();
  }

  private ReportParser.Issue newIssue(@Nullable ReportParser.TextRange textRange) {
    ReportParser.Issue input = new ReportParser.Issue();
    input.engineId = randomAlphabetic(5);
    input.ruleId = randomAlphabetic(5);
    input.severity = "CRITICAL";
    input.type = "BUG";
    input.effortMinutes = RandomUtils.nextInt();
    input.primaryLocation = new ReportParser.Location();
    input.primaryLocation.filePath = sourceFile.getProjectRelativePath();
    input.primaryLocation.message = randomAlphabetic(5);
    input.primaryLocation.textRange = textRange;
    return input;
  }
}
