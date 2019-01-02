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
package org.sonar.api.batch.sensor.internal;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SensorContextTesterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private SensorContextTester tester;
  private File baseDir;

  @Before
  public void prepare() throws Exception {
    baseDir = temp.newFolder();
    tester = SensorContextTester.create(baseDir);
  }

  @Test
  public void testSettings() {
    Settings settings = new MapSettings();
    settings.setProperty("foo", "bar");
    tester.setSettings(settings);
    assertThat(tester.settings().getString("foo")).isEqualTo("bar");
  }

  @Test
  public void testActiveRules() {
    NewActiveRule activeRule = new NewActiveRule.Builder()
      .setRuleKey(RuleKey.of("foo", "bar"))
      .build();
    ActiveRules activeRules = new ActiveRulesBuilder().addRule(activeRule).build();
    tester.setActiveRules(activeRules);
    assertThat(tester.activeRules().findAll()).hasSize(1);
  }

  @Test
  public void testFs() throws Exception {
    DefaultFileSystem fs = new DefaultFileSystem(temp.newFolder());
    tester.setFileSystem(fs);
    assertThat(tester.fileSystem().baseDir()).isNotEqualTo(baseDir);
  }

  @Test
  public void testIssues() {
    assertThat(tester.allIssues()).isEmpty();
    NewIssue newIssue = tester.newIssue();
    newIssue
      .at(newIssue.newLocation().on(new TestInputFileBuilder("foo", "src/Foo.java").build()))
      .forRule(RuleKey.of("repo", "rule"))
      .save();
    newIssue = tester.newIssue();
    newIssue
      .at(newIssue.newLocation().on(new TestInputFileBuilder("foo", "src/Foo.java").build()))
      .forRule(RuleKey.of("repo", "rule"))
      .save();
    assertThat(tester.allIssues()).hasSize(2);
  }

  @Test
  public void testExternalIssues() {
    assertThat(tester.allExternalIssues()).isEmpty();
    NewExternalIssue newExternalIssue = tester.newExternalIssue();
    newExternalIssue
      .at(newExternalIssue.newLocation().message("message").on(new TestInputFileBuilder("foo", "src/Foo.java").build()))
      .forRule(RuleKey.of("repo", "rule"))
      .type(RuleType.BUG)
      .severity(Severity.BLOCKER)
      .save();
    newExternalIssue = tester.newExternalIssue();
    newExternalIssue
      .at(newExternalIssue.newLocation().message("message").on(new TestInputFileBuilder("foo", "src/Foo.java").build()))
      .type(RuleType.BUG)
      .severity(Severity.BLOCKER)
      .forRule(RuleKey.of("repo", "rule"))
      .save();
    assertThat(tester.allExternalIssues()).hasSize(2);
  }

  @Test
  public void testAnalysisErrors() {
    assertThat(tester.allAnalysisErrors()).isEmpty();
    NewAnalysisError newAnalysisError = tester.newAnalysisError();

    InputFile file = new TestInputFileBuilder("foo", "src/Foo.java").build();
    newAnalysisError.onFile(file)
      .message("error")
      .at(new DefaultTextPointer(5, 2))
      .save();

    assertThat(tester.allAnalysisErrors()).hasSize(1);
    AnalysisError analysisError = tester.allAnalysisErrors().iterator().next();

    assertThat(analysisError.inputFile()).isEqualTo(file);
    assertThat(analysisError.message()).isEqualTo("error");
    assertThat(analysisError.location()).isEqualTo(new DefaultTextPointer(5, 2));

  }

  @Test
  public void testMeasures() throws IOException {
    assertThat(tester.measures("foo:src/Foo.java")).isEmpty();
    assertThat(tester.measure("foo:src/Foo.java", "ncloc")).isNull();
    tester.<Integer>newMeasure()
      .on(new TestInputFileBuilder("foo", "src/Foo.java").build())
      .forMetric(CoreMetrics.NCLOC)
      .withValue(2)
      .save();
    assertThat(tester.measures("foo:src/Foo.java")).hasSize(1);
    assertThat(tester.measure("foo:src/Foo.java", "ncloc")).isNotNull();
    tester.<Integer>newMeasure()
      .on(new TestInputFileBuilder("foo", "src/Foo.java").build())
      .forMetric(CoreMetrics.LINES)
      .withValue(4)
      .save();
    assertThat(tester.measures("foo:src/Foo.java")).hasSize(2);
    assertThat(tester.measure("foo:src/Foo.java", "ncloc")).isNotNull();
    assertThat(tester.measure("foo:src/Foo.java", "lines")).isNotNull();
    tester.<Integer>newMeasure()
      .on(new DefaultInputModule(ProjectDefinition.create().setKey("foo").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder())))
      .forMetric(CoreMetrics.DIRECTORIES)
      .withValue(4)
      .save();
    assertThat(tester.measures("foo")).hasSize(1);
    assertThat(tester.measure("foo", "directories")).isNotNull();
  }

  @Test(expected = IllegalStateException.class)
  public void duplicateMeasures() {
    tester.<Integer>newMeasure()
      .on(new TestInputFileBuilder("foo", "src/Foo.java").build())
      .forMetric(CoreMetrics.NCLOC)
      .withValue(2)
      .save();
    tester.<Integer>newMeasure()
      .on(new TestInputFileBuilder("foo", "src/Foo.java").build())
      .forMetric(CoreMetrics.NCLOC)
      .withValue(2)
      .save();
  }

  @Test
  public void testHighlighting() {
    assertThat(tester.highlightingTypeAt("foo:src/Foo.java", 1, 3)).isEmpty();
    tester.newHighlighting()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar").build())
      .highlight(1, 0, 1, 5, TypeOfText.ANNOTATION)
      .highlight(8, 10, TypeOfText.CONSTANT)
      .highlight(9, 10, TypeOfText.COMMENT)
      .save();
    assertThat(tester.highlightingTypeAt("foo:src/Foo.java", 1, 3)).containsExactly(TypeOfText.ANNOTATION);
    assertThat(tester.highlightingTypeAt("foo:src/Foo.java", 1, 9)).containsExactly(TypeOfText.CONSTANT, TypeOfText.COMMENT);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void duplicateHighlighting() {
    tester.newHighlighting()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar").build())
      .highlight(1, 0, 1, 5, TypeOfText.ANNOTATION)
      .save();
    tester.newHighlighting()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar").build())
      .highlight(1, 0, 1, 5, TypeOfText.ANNOTATION)
      .save();
  }

  @Test
  public void testSymbolReferences() {
    assertThat(tester.referencesForSymbolAt("foo:src/Foo.java", 1, 0)).isNull();

    NewSymbolTable symbolTable = tester.newSymbolTable()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar").build());
    symbolTable
      .newSymbol(1, 8, 1, 10);

    symbolTable
      .newSymbol(1, 1, 1, 5)
      .newReference(6, 9)
      .newReference(1, 10, 1, 13);

    symbolTable.save();

    assertThat(tester.referencesForSymbolAt("foo:src/Foo.java", 1, 0)).isNull();
    assertThat(tester.referencesForSymbolAt("foo:src/Foo.java", 1, 8)).isEmpty();
    assertThat(tester.referencesForSymbolAt("foo:src/Foo.java", 1, 3)).extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset").containsExactly(tuple(1, 6, 1, 9),
      tuple(1, 10, 1, 13));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void duplicateSymbolReferences() {
    NewSymbolTable symbolTable = tester.newSymbolTable()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar").build());
    symbolTable
      .newSymbol(1, 8, 1, 10);

    symbolTable.save();

    symbolTable = tester.newSymbolTable()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar").build());
    symbolTable
      .newSymbol(1, 8, 1, 10);

    symbolTable.save();
  }

  @Test
  public void testCoverageAtLineZero() {
    assertThat(tester.lineHits("foo:src/Foo.java", 1)).isNull();
    assertThat(tester.lineHits("foo:src/Foo.java", 4)).isNull();

    exception.expect(IllegalStateException.class);
    tester.newCoverage()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar").build())
      .lineHits(0, 3);
  }

  @Test
  public void testCoverageAtLineOutOfRange() {
    assertThat(tester.lineHits("foo:src/Foo.java", 1)).isNull();
    assertThat(tester.lineHits("foo:src/Foo.java", 4)).isNull();
    exception.expect(IllegalStateException.class);

    tester.newCoverage()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar").build())
      .lineHits(4, 3);
  }

  @Test
  public void testLineHits() {
    assertThat(tester.lineHits("foo:src/Foo.java", 1)).isNull();
    assertThat(tester.lineHits("foo:src/Foo.java", 4)).isNull();
    tester.newCoverage()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar\nasdas").build())
      .lineHits(1, 2)
      .lineHits(2, 3)
      .save();
    assertThat(tester.lineHits("foo:src/Foo.java", 1)).isEqualTo(2);
    assertThat(tester.lineHits("foo:src/Foo.java", 2)).isEqualTo(3);
  }

  public void multipleCoverage() {
    tester.newCoverage()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar\nasdas").build())
      .lineHits(1, 2)
      .conditions(3, 4, 2)
      .save();
    tester.newCoverage()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java").initMetadata("annot dsf fds foo bar\nasdas").build())
      .lineHits(1, 2)
      .conditions(3, 4, 3)
      .save();
    assertThat(tester.lineHits("foo:src/Foo.java", 1)).isEqualTo(4);
    assertThat(tester.conditions("foo:src/Foo.java", 3)).isEqualTo(4);
    assertThat(tester.coveredConditions("foo:src/Foo.java", 3)).isEqualTo(3);
  }

  @Test
  public void testConditions() {
    assertThat(tester.conditions("foo:src/Foo.java", 1)).isNull();
    assertThat(tester.coveredConditions("foo:src/Foo.java", 1)).isNull();
    tester.newCoverage()
      .onFile(new TestInputFileBuilder("foo", "src/Foo.java")
        .initMetadata("annot dsf fds foo bar\nasd\nasdas\nasdfas")
        .build())
      .conditions(1, 4, 2)
      .save();
    assertThat(tester.conditions("foo:src/Foo.java", 1)).isEqualTo(4);
    assertThat(tester.coveredConditions("foo:src/Foo.java", 1)).isEqualTo(2);
  }

  @Test
  public void testCpdTokens() {
    assertThat(tester.cpdTokens("foo:src/Foo.java")).isNull();
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .initMetadata("public class Foo {\n\n}")
      .build();
    tester.newCpdTokens()
      .onFile(inputFile)
      .addToken(inputFile.newRange(0, 6), "public")
      .addToken(inputFile.newRange(7, 12), "class")
      .addToken(inputFile.newRange(13, 16), "$IDENTIFIER")
      .addToken(inputFile.newRange(17, 18), "{")
      .addToken(inputFile.newRange(3, 0, 3, 1), "}")
      .save();
    assertThat(tester.cpdTokens("foo:src/Foo.java")).extracting("value", "startLine", "startUnit", "endUnit")
      .containsExactly(
        tuple("publicclass$IDENTIFIER{", 1, 1, 4),
        tuple("}", 3, 5, 5));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void duplicateCpdTokens() {
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .initMetadata("public class Foo {\n\n}")
      .build();
    tester.newCpdTokens()
      .onFile(inputFile)
      .addToken(inputFile.newRange(0, 6), "public")
      .save();

    tester.newCpdTokens()
      .onFile(inputFile)
      .addToken(inputFile.newRange(0, 6), "public")
      .save();
  }

  @Test
  public void testCancellation() {
    assertThat(tester.isCancelled()).isFalse();
    tester.setCancelled(true);
    assertThat(tester.isCancelled()).isTrue();
  }

  @Test
  public void testContextProperties() {
    assertThat(tester.getContextProperties()).isEmpty();

    tester.addContextProperty("foo", "bar");
    assertThat(tester.getContextProperties()).containsOnly(entry("foo", "bar"));
  }
}
