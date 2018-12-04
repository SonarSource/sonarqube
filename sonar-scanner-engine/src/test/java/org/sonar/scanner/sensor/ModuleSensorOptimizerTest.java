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
package org.sonar.scanner.sensor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleSensorOptimizerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private DefaultFileSystem fs;
  private ModuleSensorOptimizer optimizer;
  private MapSettings settings;

  @Before
  public void prepare() throws Exception {
    fs = new DefaultFileSystem(temp.newFolder().toPath());
    settings = new MapSettings();
    optimizer = new ModuleSensorOptimizer(fs, new ActiveRulesBuilder().build(), settings.asConfig());
  }

  @Test
  public void should_run_analyzer_with_no_metadata() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_language() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .onlyOnLanguages("java", "php");
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new TestInputFileBuilder("foo", "src/Foo.java").setLanguage("java").build());
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_type() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .onlyOnFileType(InputFile.Type.MAIN);
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new TestInputFileBuilder("foo", "tests/FooTest.java").setType(InputFile.Type.TEST).build());
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new TestInputFileBuilder("foo", "src/Foo.java").setType(InputFile.Type.MAIN).build());
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_both_type_and_language() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .onlyOnLanguages("java", "php")
      .onlyOnFileType(InputFile.Type.MAIN);
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new TestInputFileBuilder("foo", "tests/FooTest.java").setLanguage("java").setType(InputFile.Type.TEST).build());
    fs.add(new TestInputFileBuilder("foo", "src/Foo.cbl").setLanguage("cobol").setType(InputFile.Type.MAIN).build());
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    fs.add(new TestInputFileBuilder("foo", "src/Foo.java").setLanguage("java").setType(InputFile.Type.MAIN).build());
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_repository() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .createIssuesForRuleRepositories("squid");
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    ActiveRules activeRules = new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("repo1", "foo")).build())
      .build();
    optimizer = new ModuleSensorOptimizer(fs, activeRules, settings.asConfig());

    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    activeRules = new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("repo1", "foo")).build())
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("squid", "rule")).build())
      .build();
    optimizer = new ModuleSensorOptimizer(fs, activeRules, settings.asConfig());
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_settings() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor()
      .requireProperty("sonar.foo.reportPath");
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    settings.setProperty("sonar.foo.reportPath", "foo");
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

}
