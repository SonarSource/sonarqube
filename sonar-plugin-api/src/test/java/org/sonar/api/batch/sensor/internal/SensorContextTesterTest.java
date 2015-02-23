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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;

import java.io.File;

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
    assertThat(tester.analysisMode().isIncremental()).isFalse();
    assertThat(tester.analysisMode().isPreview()).isFalse();
    tester.analysisMode().setIncremental(true);
    assertThat(tester.analysisMode().isIncremental()).isTrue();
    tester.analysisMode().setPreview(true);
    assertThat(tester.analysisMode().isPreview()).isTrue();
  }

  @Test
  public void testIssues() {
    assertThat(tester.issues("foo:src/Foo.java")).isEmpty();
    assertThat(tester.allIssues()).isEmpty();
    tester.newIssue()
      .onFile(new DefaultInputFile("foo", "src/Foo.java").setLines(10))
      .forRule(RuleKey.of("repo", "rule"))
      .atLine(1)
      .save();
    tester.newIssue()
      .onFile(new DefaultInputFile("foo", "src/Foo.java").setLines(10))
      .forRule(RuleKey.of("repo", "rule"))
      .atLine(3)
      .save();
    assertThat(tester.issues("foo:src/Foo.java")).hasSize(2);
    assertThat(tester.allIssues()).hasSize(2);
    tester.newIssue()
      .onDir(new DefaultInputDir("foo", "src"))
      .forRule(RuleKey.of("repo", "rule"))
      .save();
    assertThat(tester.issues("foo:src")).hasSize(1);
    assertThat(tester.allIssues()).hasSize(3);
    tester.newIssue()
      .onProject()
      .forRule(RuleKey.of("repo", "rule"))
      .save();
    assertThat(tester.issues(null)).hasSize(1);
    assertThat(tester.allIssues()).hasSize(4);
  }

  @Test
  public void testMeasures() {
    assertThat(tester.measures("foo:src/Foo.java")).isEmpty();
    assertThat(tester.measure("foo:src/Foo.java", "ncloc")).isNull();
    tester.<Integer>newMeasure()
      .onFile(new DefaultInputFile("foo", "src/Foo.java"))
      .forMetric(CoreMetrics.NCLOC)
      .withValue(2)
      .save();
    assertThat(tester.measures("foo:src/Foo.java")).hasSize(1);
    assertThat(tester.measure("foo:src/Foo.java", "ncloc")).isNotNull();
    tester.<Integer>newMeasure()
      .onFile(new DefaultInputFile("foo", "src/Foo.java"))
      .forMetric(CoreMetrics.LINES)
      .withValue(4)
      .save();
    assertThat(tester.measures("foo:src/Foo.java")).hasSize(2);
    assertThat(tester.measure("foo:src/Foo.java", "ncloc")).isNotNull();
    assertThat(tester.measure("foo:src/Foo.java", "lines")).isNotNull();
    tester.<Integer>newMeasure()
      .onProject()
      .forMetric(CoreMetrics.DIRECTORIES)
      .withValue(4)
      .save();
    assertThat(tester.measures(null)).hasSize(1);
    assertThat(tester.measure(null, "directories")).isNotNull();
  }

  @Test
  public void testHighlighting() {
    assertThat(tester.highlightingTypeFor("foo:src/Foo.java", 3)).isEmpty();
    tester.newHighlighting()
      .onFile(new DefaultInputFile("foo", "src/Foo.java").setLastValidOffset(100))
      .highlight(0, 4, TypeOfText.ANNOTATION)
      .highlight(8, 10, TypeOfText.CONSTANT)
      .highlight(9, 10, TypeOfText.COMMENT)
      .save();
    assertThat(tester.highlightingTypeFor("foo:src/Foo.java", 3)).containsExactly(TypeOfText.ANNOTATION);
    assertThat(tester.highlightingTypeFor("foo:src/Foo.java", 9)).containsExactly(TypeOfText.CONSTANT, TypeOfText.COMMENT);
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
  public void testDependencies() {
    assertThat(tester.dependencies()).isEmpty();
    tester.newDependency()
      .from(new DefaultInputFile("foo", "src/Foo.java"))
      .to(new DefaultInputFile("foo", "src/Foo2.java"))
      .weight(3)
      .save();
    assertThat(tester.dependencies()).hasSize(1);
  }
}
