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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.config.Configuration;
import org.sonar.api.internal.apachecommons.io.IOUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalTagsIssueSensorTest {

  private static final String RESOURCE_NAME = "internal_tags.xoo";

  @TempDir
  private File temp;

  private ActiveRules activeRules;
  private DefaultFileSystem fs;
  private Configuration settings;

  private InternalTagsIssueSensor internalTagsIssueSensor;

  @BeforeEach
  void setUp() throws IOException {
    mockRuleKey();
    prepareFileSystem();
    settings = mock(Configuration.class);
    internalTagsIssueSensor = new InternalTagsIssueSensor(settings, fs, activeRules);
  }

  private void mockRuleKey() {
    activeRules = mock(ActiveRules.class);
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, InternalTagsIssueSensor.RULE_KEY);
    when(activeRules.find(ruleKey)).thenReturn(mock(ActiveRule.class));
  }

  private void prepareFileSystem() throws IOException {
    URL url = ofNullable(getClass().getResource(RESOURCE_NAME)).orElseGet(() -> fail("Resource not found: " + RESOURCE_NAME));
    DefaultInputFile inputFile = newTestFile(IOUtils.toString(url, StandardCharsets.UTF_8));
    fs = new DefaultFileSystem(temp);
    fs.add(inputFile);
  }

  private DefaultInputFile newTestFile(String content) {
    return new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setType(InputFile.Type.MAIN)
      .setContents(content)
      .build();
  }

  @Test
  void execute_shouldSetInternalTagsOnIssues() {
    SensorContextTester sensorContextTester = SensorContextTester.create(fs.baseDir());
    sensorContextTester.setFileSystem(fs);

    internalTagsIssueSensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).hasSize(1);
    Issue issue = sensorContextTester.allIssues().iterator().next();
    assertThat(issue.internalTags()).containsExactlyInAnyOrder("advanced", "saast", "security");
  }

  @Test
  void execute_shouldNotSetInternalTags_whenDisabledByProperty() {
    when(settings.get("sonar.enable.internalTags")).thenReturn(Optional.of("false"));
    SensorContextTester sensorContextTester = SensorContextTester.create(fs.baseDir());
    sensorContextTester.setFileSystem(fs);

    internalTagsIssueSensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).hasSize(1);
    Issue issue = sensorContextTester.allIssues().iterator().next();
    assertThat(issue.internalTags()).isEmpty();
  }

}
