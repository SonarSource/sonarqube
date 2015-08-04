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
package org.sonar.api.batch.sensor.internal;

import java.io.File;
import java.io.StringReader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;

public class SensorContextTesterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private SensorContextTester tester;
  private File baseDir;

  @Before
  public void prepare() throws Exception {
    baseDir = temp.newFolder();
    tester = SensorContextTester.create(baseDir);
  }

  @Test
  public void testSettings() {
    Settings settings = new Settings();
    settings.setProperty("foo", "bar");
    tester.setSettings(settings);
    assertThat(tester.settings().getString("foo")).isEqualTo("bar");
  }

  @Test
  public void testActiveRules() {
    ActiveRules activeRules = new ActiveRulesBuilder().create(RuleKey.of("repo", "rule")).activate().build();
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
  public void testAnalysisMode() {
    assertThat(tester.analysisMode().isPreview()).isFalse();
    tester.analysisMode().setPreview(true);
    assertThat(tester.analysisMode().isPreview()).isTrue();
  }

  @Test
  public void testIssues() {
    assertThat(tester.allIssues()).isEmpty();
    NewIssue newIssue = tester.newIssue();
    newIssue
      .at(newIssue.newLocation().on(new DefaultInputFile("foo", "src/Foo.java")))
      .forRule(RuleKey.of("repo", "rule"))
      .save();
    newIssue = tester.newIssue();
    newIssue
      .at(newIssue.newLocation().on(new DefaultInputFile("foo", "src/Foo.java")))
      .forRule(RuleKey.of("repo", "rule"))
      .save();
    assertThat(tester.allIssues()).hasSize(2);
  }

  @Test
  public void testMeasures() {
    assertThat(tester.measures("foo:src/Foo.java")).isEmpty();
    assertThat(tester.measure("foo:src/Foo.java", "ncloc")).isNull();
    tester.<Integer>newMeasure()
      .on(new DefaultInputFile("foo", "src/Foo.java"))
      .forMetric(CoreMetrics.NCLOC)
      .withValue(2)
      .save();
    assertThat(tester.measures("foo:src/Foo.java")).hasSize(1);
    assertThat(tester.measure("foo:src/Foo.java", "ncloc")).isNotNull();
    tester.<Integer>newMeasure()
      .on(new DefaultInputFile("foo", "src/Foo.java"))
      .forMetric(CoreMetrics.LINES)
      .withValue(4)
      .save();
    assertThat(tester.measures("foo:src/Foo.java")).hasSize(2);
    assertThat(tester.measure("foo:src/Foo.java", "ncloc")).isNotNull();
    assertThat(tester.measure("foo:src/Foo.java", "lines")).isNotNull();
    tester.<Integer>newMeasure()
      .on(new DefaultInputModule("foo"))
      .forMetric(CoreMetrics.DIRECTORIES)
      .withValue(4)
      .save();
    assertThat(tester.measures("foo")).hasSize(1);
    assertThat(tester.measure("foo", "directories")).isNotNull();
  }

  @Test
  public void testHighlighting() {
    assertThat(tester.highlightingTypeAt("foo:src/Foo.java", 1, 3)).isEmpty();
    tester.newHighlighting()
      .onFile(new DefaultInputFile("foo", "src/Foo.java").initMetadata(new FileMetadata().readMetadata(new StringReader("annot dsf fds foo bar"))))
      .highlight(0, 4, TypeOfText.ANNOTATION)
      .highlight(8, 10, TypeOfText.CONSTANT)
      .highlight(9, 10, TypeOfText.COMMENT)
      .save();
    assertThat(tester.highlightingTypeAt("foo:src/Foo.java", 1, 3)).containsExactly(TypeOfText.ANNOTATION);
    assertThat(tester.highlightingTypeAt("foo:src/Foo.java", 1, 9)).containsExactly(TypeOfText.CONSTANT, TypeOfText.COMMENT);
  }

  @Test
  public void testDuplication() {
    assertThat(tester.duplications()).isEmpty();
    tester.newDuplication()
      .originBlock(new DefaultInputFile("foo", "src/Foo.java").setLines(40), 1, 30)
      .isDuplicatedBy(new DefaultInputFile("foo", "src/Foo2.java").setLines(40), 3, 33)
      .isDuplicatedBy(new DefaultInputFile("foo", "src/Foo3.java").setLines(40), 4, 34)
      .save();
    assertThat(tester.duplications()).hasSize(1);
  }

  @Test
  public void testLineHits() {
    assertThat(tester.lineHits("foo:src/Foo.java", CoverageType.UNIT, 1)).isNull();
    assertThat(tester.lineHits("foo:src/Foo.java", CoverageType.UNIT, 4)).isNull();
    tester.newCoverage()
      .onFile(new DefaultInputFile("foo", "src/Foo.java").initMetadata(new FileMetadata().readMetadata(new StringReader("annot dsf fds foo bar"))))
      .ofType(CoverageType.UNIT)
      .lineHits(1, 2)
      .lineHits(4, 3)
      .save();
    assertThat(tester.lineHits("foo:src/Foo.java", CoverageType.UNIT, 1)).isEqualTo(2);
    assertThat(tester.lineHits("foo:src/Foo.java", CoverageType.IT, 1)).isNull();
    assertThat(tester.lineHits("foo:src/Foo.java", CoverageType.UNIT, 4)).isEqualTo(3);
  }

  @Test
  public void testConditions() {
    assertThat(tester.conditions("foo:src/Foo.java", CoverageType.UNIT, 1)).isNull();
    assertThat(tester.coveredConditions("foo:src/Foo.java", CoverageType.UNIT, 1)).isNull();
    tester.newCoverage()
      .onFile(new DefaultInputFile("foo", "src/Foo.java").initMetadata(new FileMetadata().readMetadata(new StringReader("annot dsf fds foo bar"))))
      .ofType(CoverageType.UNIT)
      .conditions(1, 4, 2)
      .save();
    assertThat(tester.conditions("foo:src/Foo.java", CoverageType.UNIT, 1)).isEqualTo(4);
    assertThat(tester.coveredConditions("foo:src/Foo.java", CoverageType.UNIT, 1)).isEqualTo(2);

    assertThat(tester.conditions("foo:src/Foo.java", CoverageType.IT, 1)).isNull();
    assertThat(tester.coveredConditions("foo:src/Foo.java", CoverageType.IT, 1)).isNull();
  }
}
