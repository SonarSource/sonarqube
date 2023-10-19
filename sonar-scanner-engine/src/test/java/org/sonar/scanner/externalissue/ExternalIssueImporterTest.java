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
package org.sonar.scanner.externalissue;

import java.io.File;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.rule.AdHocRule;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.testfixtures.log.LogTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;

public class ExternalIssueImporterTest {

  public static final String RULE_ENGINE_ID = "some_rule_engine_id";
  public static final String RULE_ID = "some_rule_id";
  public static final String RULE_NAME = "some_rule_name";
  public static final CleanCodeAttribute RULE_ATTRIBUTE = CleanCodeAttribute.FORMATTED;

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
  public void execute_whenNewFormatWithZeroIssues() {
    ExternalIssueReport report = new ExternalIssueReport();
    ExternalIssueReport.Rule rule = createRule();
    report.issues = new ExternalIssueReport.Issue[0];
    report.rules = new ExternalIssueReport.Rule[]{rule};

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);
    underTest.execute();

    assertThat(context.allExternalIssues()).isEmpty();
    assertThat(context.allIssues()).isEmpty();
    assertThat(logs.logs(Level.INFO)).contains("Imported 0 issues in 0 files");
  }

  @Test
  public void execute_whenNewFormatWithMinimalInfo() {
    ExternalIssueReport.Issue input = new ExternalIssueReport.Issue();
    input.primaryLocation = new ExternalIssueReport.Location();
    input.primaryLocation.filePath = sourceFile.getProjectRelativePath();
    input.primaryLocation.message = randomAlphabetic(5);

    runOn(input);

    assertThat(context.allExternalIssues()).hasSize(1);
    ExternalIssue output = context.allExternalIssues().iterator().next();
    assertThat(output.engineId()).isEqualTo(RULE_ENGINE_ID);
    assertThat(output.ruleId()).isEqualTo(RULE_ID);
    assertThat(output.severity()).isEqualTo(Severity.CRITICAL); //backmapped
    assertThat(output.type()).isEqualTo(RuleType.VULNERABILITY); //backmapped
    assertThat(output.remediationEffort()).isNull();
    assertThat(logs.logs(Level.INFO)).contains("Imported 1 issue in 1 file");
    assertThat(context.allAdHocRules()).hasSize(1);

    AdHocRule output1 = context.allAdHocRules().iterator().next();
    assertThat(output1.ruleId()).isEqualTo(RULE_ID);
    assertThat(output1.name()).isEqualTo(RULE_NAME);
    assertThat(output1.engineId()).isEqualTo(RULE_ENGINE_ID);
    assertThat(output1.severity()).isEqualTo(Severity.CRITICAL); //backmapped
    assertThat(output1.type()).isEqualTo(RuleType.VULNERABILITY); //backmapped
    assertThat(output1.cleanCodeAttribute()).isEqualTo(RULE_ATTRIBUTE);
    assertThat(output1.defaultImpacts()).containsExactlyInAnyOrderEntriesOf(Map.of(SECURITY, HIGH, MAINTAINABILITY, LOW));
  }

  @Test
  public void execute_whenNewFormatWithCompletePrimaryLocation() {
    ExternalIssueReport.TextRange input = new ExternalIssueReport.TextRange();
    input.startLine = 1;
    input.startColumn = 4;
    input.endLine = 2;
    input.endColumn = 3;

    runOn(newIssue(input));

    assertThat(context.allExternalIssues()).hasSize(1);
    ExternalIssue output = context.allExternalIssues().iterator().next();
    assertSameRange(input, output.primaryLocation().textRange());
  }

  @Test
  public void execute_whenNewFormatWithNoColumns() {
    ExternalIssueReport.TextRange input = new ExternalIssueReport.TextRange();
    input.startLine = 1;
    input.startColumn = null;
    input.endLine = 2;
    input.endColumn = null;

    runOn(newIssue(input));

    assertThat(context.allExternalIssues()).hasSize(1);
    TextRange got = context.allExternalIssues().iterator().next().primaryLocation().textRange();
    assertThat(got.start().line()).isEqualTo(input.startLine);
    assertThat(got.start().lineOffset()).isZero();
    assertThat(got.end().line()).isEqualTo(input.startLine);
    assertThat(got.end().lineOffset()).isEqualTo(sourceFile.selectLine(input.startLine).end().lineOffset());
  }

  @Test
  public void execute_whenNewFormatWithStartButNotEndColumn() {
    ExternalIssueReport.TextRange input = new ExternalIssueReport.TextRange();
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

  @Test
  public void execute_whenNewFormatContainsNonExistentCleanCodeAttribute_shouldThrowException() {
    ExternalIssueReport report = new ExternalIssueReport();
    ExternalIssueReport.Rule rule = createRule("not_existent_attribute", MAINTAINABILITY.name(), HIGH.name());
    report.issues = new ExternalIssueReport.Issue[]{};
    report.rules = new ExternalIssueReport.Rule[]{rule};

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);

    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No enum constant org.sonar.api.rules.CleanCodeAttribute.not_existent_attribute");
  }

  @Test
  public void execute_whenNewFormatContainsNonExistentSoftwareQuality_shouldThrowException() {
    ExternalIssueReport report = new ExternalIssueReport();
    ExternalIssueReport.Rule rule = createRule(CleanCodeAttribute.CONVENTIONAL.name(), "not_existent_software_quality", HIGH.name());
    report.issues = new ExternalIssueReport.Issue[]{};
    report.rules = new ExternalIssueReport.Rule[]{rule};

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);

    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No enum constant org.sonar.api.issue.impact.SoftwareQuality.not_existent_software_quality");
  }

  @Test
  public void execute_whenNewFormatContainsNonExistentImpactSeverity_shouldThrowException() {
    ExternalIssueReport report = new ExternalIssueReport();
    ExternalIssueReport.Rule rule = createRule(CleanCodeAttribute.CONVENTIONAL.name(), SoftwareQuality.RELIABILITY.name(),
      "not_existent_impact_severity");
    report.issues = new ExternalIssueReport.Issue[]{};
    report.rules = new ExternalIssueReport.Rule[]{rule};

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);

    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No enum constant org.sonar.api.issue.impact.Severity.not_existent_impact_severity");
  }

  @Test
  public void execute_whenDeprecatedFormatWithZeroIssues() {
    ExternalIssueReport report = new ExternalIssueReport();
    report.issues = new ExternalIssueReport.Issue[0];

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);
    underTest.execute();
    assertThat(context.allExternalIssues()).isEmpty();
    assertThat(context.allIssues()).isEmpty();
    assertThat(logs.logs(Level.INFO)).contains("Imported 0 issues in 0 files");
  }

  @Test
  public void execute_whenDeprecatedFormatWithMinimalInfo() {
    ExternalIssueReport report = new ExternalIssueReport();
    ExternalIssueReport.Issue input = new ExternalIssueReport.Issue();
    input.engineId = "findbugs";
    input.ruleId = "123";
    input.severity = "CRITICAL";
    input.type = "BUG";
    input.primaryLocation = new ExternalIssueReport.Location();
    input.primaryLocation.filePath = sourceFile.getProjectRelativePath();
    input.primaryLocation.message = randomAlphabetic(5);
    report.issues = new ExternalIssueReport.Issue[]{input};

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);
    underTest.execute();

    assertThat(context.allExternalIssues()).hasSize(1);
    ExternalIssue output = context.allExternalIssues().iterator().next();
    assertThat(output.engineId()).isEqualTo(input.engineId);
    assertThat(output.ruleId()).isEqualTo(input.ruleId);
    assertThat(output.severity()).isEqualTo(Severity.valueOf(input.severity));
    assertThat(output.remediationEffort()).isNull();
    assertThat(logs.logs(Level.INFO)).contains("Imported 1 issue in 1 file");
  }

  @Test
  public void execute_whenDeprecatedFormatWithCompletePrimaryLocation() {
    ExternalIssueReport.TextRange input = new ExternalIssueReport.TextRange();
    input.startLine = 1;
    input.startColumn = 4;
    input.endLine = 2;
    input.endColumn = 3;

    runOnDeprecatedFormat(newIssue(input));

    assertThat(context.allExternalIssues()).hasSize(1);
    ExternalIssue output = context.allExternalIssues().iterator().next();
    assertSameRange(input, output.primaryLocation().textRange());
  }

  @Test
  public void execute_whenDeprecatedFormatWithNoColumns() {
    ExternalIssueReport.TextRange input = new ExternalIssueReport.TextRange();
    input.startLine = 1;
    input.startColumn = null;
    input.endLine = 2;
    input.endColumn = null;

    runOnDeprecatedFormat(newIssue(input));

    assertThat(context.allExternalIssues()).hasSize(1);
    TextRange got = context.allExternalIssues().iterator().next().primaryLocation().textRange();
    assertThat(got.start().line()).isEqualTo(input.startLine);
    assertThat(got.start().lineOffset()).isZero();
    assertThat(got.end().line()).isEqualTo(input.startLine);
    assertThat(got.end().lineOffset()).isEqualTo(sourceFile.selectLine(input.startLine).end().lineOffset());
  }

  @Test
  public void execute_whenDeprecatedFormatWithStartButNotEndColumn() {
    ExternalIssueReport.TextRange input = new ExternalIssueReport.TextRange();
    input.startLine = 1;
    input.startColumn = 3;
    input.endLine = 2;
    input.endColumn = null;

    runOnDeprecatedFormat(newIssue(input));

    assertThat(context.allExternalIssues()).hasSize(1);
    TextRange got = context.allExternalIssues().iterator().next().primaryLocation().textRange();
    assertThat(got.start().line()).isEqualTo(input.startLine);
    assertThat(got.start().lineOffset()).isEqualTo(3);
    assertThat(got.end().line()).isEqualTo(input.endLine);
    assertThat(got.end().lineOffset()).isEqualTo(sourceFile.selectLine(input.endLine).end().lineOffset());
  }

  private static ExternalIssueReport.Rule createRule() {
    return createRule(RULE_ATTRIBUTE.name(), SECURITY.name(), HIGH.name());
  }

  private static ExternalIssueReport.Rule createRule(String cleanCodeAttribute, String softwareQuality, String impactSeverity) {
    ExternalIssueReport.Rule rule = new ExternalIssueReport.Rule();
    rule.id = RULE_ID;
    rule.name = RULE_NAME;
    rule.engineId = RULE_ENGINE_ID;
    rule.cleanCodeAttribute = cleanCodeAttribute;
    ExternalIssueReport.Impact impact1 = new ExternalIssueReport.Impact();
    impact1.severity = impactSeverity;
    impact1.softwareQuality = softwareQuality;
    ExternalIssueReport.Impact impact2 = new ExternalIssueReport.Impact();
    impact2.severity = LOW.name();
    impact2.softwareQuality = MAINTAINABILITY.name();
    rule.impacts = new ExternalIssueReport.Impact[]{impact1, impact2};
    return rule;
  }

  private void assertSameRange(ExternalIssueReport.TextRange expected, TextRange got) {
    assertThat(got.start().line()).isEqualTo(expected.startLine);
    assertThat(got.start().lineOffset()).isEqualTo(defaultIfNull(expected.startColumn, 0));
    assertThat(got.end().line()).isEqualTo(expected.endLine);
    assertThat(got.end().lineOffset()).isEqualTo(defaultIfNull(expected.endColumn, 0));
  }

  private void runOnDeprecatedFormat(ExternalIssueReport.Issue input) {
    ExternalIssueReport report = new ExternalIssueReport();
    report.issues = new ExternalIssueReport.Issue[]{input};

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);
    underTest.execute();
  }

  private void runOn(ExternalIssueReport.Issue input) {
    ExternalIssueReport report = new ExternalIssueReport();
    ExternalIssueReport.Rule rule = createRule();
    input.ruleId = rule.id;
    report.issues = new ExternalIssueReport.Issue[]{input};
    report.rules = new ExternalIssueReport.Rule[]{rule};

    ExternalIssueImporter underTest = new ExternalIssueImporter(this.context, report);
    underTest.execute();
  }

  private ExternalIssueReport.Issue newIssue(@Nullable ExternalIssueReport.TextRange textRange) {
    ExternalIssueReport.Issue input = new ExternalIssueReport.Issue();
    input.engineId = randomAlphabetic(5);
    input.ruleId = randomAlphabetic(5);
    input.severity = "CRITICAL";
    input.type = "BUG";
    input.effortMinutes = RandomUtils.nextInt();
    input.primaryLocation = new ExternalIssueReport.Location();
    input.primaryLocation.filePath = sourceFile.getProjectRelativePath();
    input.primaryLocation.message = randomAlphabetic(5);
    input.primaryLocation.textRange = textRange;
    return input;
  }
}
