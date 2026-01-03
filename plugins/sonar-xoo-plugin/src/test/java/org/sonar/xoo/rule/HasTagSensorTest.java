/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.xoo.rule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HasTagSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File baseDir;

  @Before
  public void setUp() throws IOException {
    baseDir = temp.newFolder();
  }

  @Test
  public void testDescriptor() throws IOException {
    ActiveRules activeRules = new ActiveRulesBuilder().build();
    SensorContextTester context = SensorContextTester.create(baseDir);
    HasTagSensor sensor = new HasTagSensor(context.fileSystem(), activeRules);
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.languages()).containsOnly(Xoo.KEY);
  }

  @Test
  public void testNoIssuesWhenRuleNotActive() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdirs();
    File xooFile = new File(srcDir, "Foo.xoo");
    FileUtils.write(xooFile, "TODO fix this\nTODO fix that\n", StandardCharsets.UTF_8);

    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setModuleBaseDir(baseDir.toPath())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata("TODO fix this\nTODO fix that\n")
      .build();

    SensorContextTester context = SensorContextTester.create(baseDir);
    context.fileSystem().add(inputFile);

    ActiveRules activeRules = new ActiveRulesBuilder().build();
    HasTagSensor sensor = new HasTagSensor(context.fileSystem(), activeRules);
    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  public void testIssuesWhenTagFound() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdirs();
    File xooFile = new File(srcDir, "Foo.xoo");
    FileUtils.write(xooFile, "TODO fix this\nno tag here\nTODO fix that\n", StandardCharsets.UTF_8);

    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setModuleBaseDir(baseDir.toPath())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata("TODO fix this\nno tag here\nTODO fix that\n")
      .build();

    SensorContextTester context = SensorContextTester.create(baseDir);
    context.fileSystem().add(inputFile);

    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, HasTagSensor.RULE_KEY);
    ActiveRules activeRules = new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder()
        .setRuleKey(ruleKey)
        .setParam("tag", "TODO")
        .build())
      .build();

    HasTagSensor sensor = new HasTagSensor(context.fileSystem(), activeRules);
    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(2);
    for (Issue issue : context.allIssues()) {
      assertThat(issue.ruleKey()).isEqualTo(ruleKey);
    }
  }

  @Test
  public void testMultipleTagsOnSameLine() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdirs();
    File xooFile = new File(srcDir, "Foo.xoo");
    FileUtils.write(xooFile, "TODO fix this TODO fix that\n", StandardCharsets.UTF_8);

    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setModuleBaseDir(baseDir.toPath())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata("TODO fix this TODO fix that\n")
      .build();

    SensorContextTester context = SensorContextTester.create(baseDir);
    context.fileSystem().add(inputFile);

    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, HasTagSensor.RULE_KEY);
    ActiveRules activeRules = new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder()
        .setRuleKey(ruleKey)
        .setParam("tag", "TODO")
        .build())
      .build();

    HasTagSensor sensor = new HasTagSensor(context.fileSystem(), activeRules);
    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(2);
  }

  @Test
  public void testFailsWhenTagParamMissing() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdirs();
    File xooFile = new File(srcDir, "Foo.xoo");
    FileUtils.write(xooFile, "TODO fix this\n", StandardCharsets.UTF_8);

    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setModuleBaseDir(baseDir.toPath())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata("TODO fix this\n")
      .build();

    SensorContextTester context = SensorContextTester.create(baseDir);
    context.fileSystem().add(inputFile);

    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, HasTagSensor.RULE_KEY);
    ActiveRules activeRules = new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder()
        .setRuleKey(ruleKey)
        .build())
      .build();

    HasTagSensor sensor = new HasTagSensor(context.fileSystem(), activeRules);

    assertThatThrownBy(() -> sensor.execute(context))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("tag");
  }

  @Test
  public void testEffortToFix() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdirs();
    File xooFile = new File(srcDir, "Foo.xoo");
    FileUtils.write(xooFile, "TODO fix this\n", StandardCharsets.UTF_8);

    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setModuleBaseDir(baseDir.toPath())
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata("TODO fix this\n")
      .build();

    SensorContextTester context = SensorContextTester.create(baseDir);
    context.fileSystem().add(inputFile);
    context.setSettings(new MapSettings().setProperty("sonar.hasTag.effortToFix", "2.5"));

    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, HasTagSensor.RULE_KEY);
    ActiveRules activeRules = new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder()
        .setRuleKey(ruleKey)
        .setParam("tag", "TODO")
        .build())
      .build();

    HasTagSensor sensor = new HasTagSensor(context.fileSystem(), activeRules);
    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(1);
    Issue issue = context.allIssues().iterator().next();
    assertThat(issue.gap()).isEqualTo(2.5);
  }
}
