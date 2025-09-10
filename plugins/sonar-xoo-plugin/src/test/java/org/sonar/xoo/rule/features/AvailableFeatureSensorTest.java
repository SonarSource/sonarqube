/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.xoo.rule.features;

import java.io.File;
import java.util.List;
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
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvailableFeatureSensorTest {

  @TempDir
  private File temp;

  private ActiveRules activeRules;
  private DefaultFileSystem fs;
  private AvailableFeatureSensor availableFeatureSensor;
  private SensorContextTester sensorContextTester;

  private static final String FILE_CONTENT = """
    This line has issue only when [REQUIRED_FEATURE](sca) is available.
    This line has issue only when [REQUIRED_FEATURE](asast) is available.
    This line has issue only when [REQUIRED_FEATURE](otherFeature) is available.
    """;

  @BeforeEach
  void setUp() {
    mockRuleKey();
    fs = new DefaultFileSystem(temp);
    sensorContextTester = SensorContextTester.create(fs.baseDir());
    sensorContextTester.setFileSystem(fs);
    availableFeatureSensor = new AvailableFeatureSensor(fs, activeRules);
  }

  private void mockRuleKey() {
    activeRules = mock(ActiveRules.class);
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, AvailableFeatureSensor.RULE_KEY);
    when(activeRules.find(ruleKey)).thenReturn(mock(ActiveRule.class));
  }

  private DefaultInputFile newTestFile(String content) {
    return new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setType(InputFile.Type.MAIN)
      .setContents(content)
      .build();
  }

  @Test
  void execute_shouldRaiseIssue_whenFeatureIsAvailable() {
    DefaultInputFile inputFile = newTestFile(FILE_CONTENT);
    fs.add(inputFile);
    sensorContextTester.addAvailableFeatures("sca", "asast");

    availableFeatureSensor.execute(sensorContextTester);

    List<Issue> issues = sensorContextTester.allIssues().stream().toList();
    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).primaryLocation().message()).isEqualTo("Issue raised because feature 'sca' is available");
    assertThat(issues.get(0).primaryLocation().textRange().start().line()).isEqualTo(1);
    assertThat(issues.get(1).primaryLocation().message()).isEqualTo("Issue raised because feature 'asast' is available");
    assertThat(issues.get(1).primaryLocation().textRange().start().line()).isEqualTo(2);
  }

  @Test
  void execute_shouldNotRaiseIssue_whenFeatureIsNotAvailable() {
    DefaultInputFile inputFile = newTestFile(FILE_CONTENT);
    fs.add(inputFile);

    availableFeatureSensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).isEmpty();
  }

  @Test
  void execute_shouldNotRaiseIssue_whenNoRequiredFeaturePattern() {
    String content = "This file has no required feature patterns.\n" +
      "Just some regular text.";
    DefaultInputFile inputFile = newTestFile(content);
    fs.add(inputFile);

    availableFeatureSensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).isEmpty();
  }

  @Test
  void execute_shouldHandleUnderscoresAndDashesInFeatureNames() {
    String content = "[REQUIRED_FEATURE](feature-with-dashes) and [REQUIRED_FEATURE](feature_with_underscores)";
    DefaultInputFile inputFile = newTestFile(content);
    fs.add(inputFile);
    sensorContextTester.addAvailableFeatures("feature-with-dashes", "feature_with_underscores");

    availableFeatureSensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).hasSize(2);
  }

  @Test
  void execute_shouldNotRaiseIssue_whenRuleNotActive() {
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, AvailableFeatureSensor.RULE_KEY);
    when(activeRules.find(ruleKey)).thenReturn(null);
    DefaultInputFile inputFile = newTestFile(FILE_CONTENT);
    fs.add(inputFile);
    sensorContextTester.addAvailableFeatures("sca", "asast");

    availableFeatureSensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).isEmpty();
  }
}
