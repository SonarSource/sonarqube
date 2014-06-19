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
package org.sonar.batch.scan2;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.analyzer.internal.DefaultAnalyzerDescriptor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.rule.RuleKey;

import static org.fest.assertions.Assertions.assertThat;

public class AnalyzerOptimizerTest {

  DefaultFileSystem fs = new DefaultFileSystem();

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private AnalyzerOptimizer optimizer;

  @Before
  public void prepare() {
    optimizer = new AnalyzerOptimizer(fs, new ActiveRulesBuilder().build());
  }

  @Test
  public void should_run_analyzer_with_no_metadata() throws Exception {
    DefaultAnalyzerDescriptor descriptor = new DefaultAnalyzerDescriptor();

    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_language() throws Exception {
    DefaultAnalyzerDescriptor descriptor = new DefaultAnalyzerDescriptor()
      .workOnLanguages("java", "php");
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("src/Foo.java").setLanguage("java"));
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_type() throws Exception {
    DefaultAnalyzerDescriptor descriptor = new DefaultAnalyzerDescriptor()
      .workOnFileTypes(InputFile.Type.MAIN);
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("tests/FooTest.java").setType(InputFile.Type.TEST));
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("src/Foo.java").setType(InputFile.Type.MAIN));
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_both_type_and_language() throws Exception {
    DefaultAnalyzerDescriptor descriptor = new DefaultAnalyzerDescriptor()
      .workOnLanguages("java", "php")
      .workOnFileTypes(InputFile.Type.MAIN);
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("tests/FooTest.java").setLanguage("java").setType(InputFile.Type.TEST));
    fs.add(new DefaultInputFile("src/Foo.cbl").setLanguage("cobol").setType(InputFile.Type.MAIN));
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new DefaultInputFile("src/Foo.java").setLanguage("java").setType(InputFile.Type.MAIN));
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_repository() throws Exception {
    DefaultAnalyzerDescriptor descriptor = new DefaultAnalyzerDescriptor()
      .createIssuesForRuleRepositories("squid");
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    ActiveRules activeRules = new ActiveRulesBuilder()
      .create(RuleKey.of("repo1", "foo"))
      .activate()
      .build();
    optimizer = new AnalyzerOptimizer(fs, activeRules);

    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    activeRules = new ActiveRulesBuilder()
      .create(RuleKey.of("repo1", "foo"))
      .activate()
      .create(RuleKey.of("squid", "rule"))
      .activate()
      .build();
    optimizer = new AnalyzerOptimizer(fs, activeRules);
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }
}
