/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.externalissue.sarif;

import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.sarif.DefaultConfiguration;
import org.sonar.core.sarif.Driver;
import org.sonar.core.sarif.Extension;
import org.sonar.core.sarif.Result;
import org.sonar.core.sarif.Rule;
import org.sonar.core.sarif.Run;
import org.sonar.core.sarif.Tool;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.externalissue.sarif.ResultMapper.DEFAULT_SEVERITY;
import static org.sonar.scanner.externalissue.sarif.RulesSeverityDetector.UNSUPPORTED_RULE_SEVERITIES_WARNING;

public class RulesSeverityDetectorTest {
  private static final String DRIVER_NAME = "Test";
  private static final String WARNING = "warning";
  private static final String RULE_ID = "RULE_ID";

  @org.junit.Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.TRACE);

  private final Run run = mock(Run.class);
  private final Rule rule = mock(Rule.class);
  private final Tool tool = mock(Tool.class);
  private final Result result = mock(Result.class);
  private final Driver driver = mock(Driver.class);
  private final Extension extension = mock(Extension.class);
  private final DefaultConfiguration defaultConfiguration = mock(DefaultConfiguration.class);

  @Test
  public void detectRulesSeverities_detectsCorrectlyResultDefinedRuleSeverities() {
    Run run = mockResultDefinedRuleSeverities();

    Map<String, String> rulesSeveritiesByRuleId = RulesSeverityDetector.detectRulesSeverities(run, DRIVER_NAME);

    assertNoLogs();
    assertDetectedRuleSeverities(rulesSeveritiesByRuleId, tuple(RULE_ID, WARNING));
  }

  @Test
  public void detectRulesSeverities_detectsCorrectlyDriverDefinedRuleSeverities() {
    Run run = mockDriverDefinedRuleSeverities();

    Map<String, String> rulesSeveritiesByRuleId = RulesSeverityDetector.detectRulesSeverities(run, DRIVER_NAME);

    assertNoLogs();
    assertDetectedRuleSeverities(rulesSeveritiesByRuleId, tuple(RULE_ID, WARNING));
  }

  @Test
  public void detectRulesSeverities_detectsCorrectlyExtensionsDefinedRuleSeverities() {
    Run run = mockExtensionsDefinedRuleSeverities();

    Map<String, String> rulesSeveritiesByRuleId = RulesSeverityDetector.detectRulesSeverities(run, DRIVER_NAME);

    assertNoLogs();
    assertDetectedRuleSeverities(rulesSeveritiesByRuleId, tuple(RULE_ID, WARNING));
  }

  @Test
  public void detectRulesSeverities_returnsEmptyMapAndLogsWarning_whenUnableToDetectSeverities() {
    Run run = mockUnsupportedRuleSeveritiesDefinition();

    Map<String, String> rulesSeveritiesByRuleId = RulesSeverityDetector.detectRulesSeverities(run, DRIVER_NAME);

    assertWarningLog(DRIVER_NAME, DEFAULT_SEVERITY);
    assertDetectedRuleSeverities(rulesSeveritiesByRuleId);
  }

  private Run mockResultDefinedRuleSeverities() {
    when(run.getResults()).thenReturn(Set.of(result));
    when(result.getLevel()).thenReturn(WARNING);
    when(result.getRuleId()).thenReturn(RULE_ID);

    return run;
  }

  private Run mockDriverDefinedRuleSeverities() {
    when(run.getTool()).thenReturn(tool);
    when(tool.getDriver()).thenReturn(driver);
    when(driver.getRules()).thenReturn(Set.of(rule));
    when(rule.getId()).thenReturn(RULE_ID);
    when(rule.getDefaultConfiguration()).thenReturn(defaultConfiguration);
    when(defaultConfiguration.getLevel()).thenReturn(WARNING);

    return run;
  }

  private Run mockExtensionsDefinedRuleSeverities() {
    when(run.getTool()).thenReturn(tool);
    when(tool.getDriver()).thenReturn(driver);
    when(driver.getRules()).thenReturn(Set.of());
    when(tool.getExtensions()).thenReturn(Set.of(extension));
    when(extension.getRules()).thenReturn(Set.of(rule));
    when(rule.getId()).thenReturn(RULE_ID);
    when(rule.getDefaultConfiguration()).thenReturn(defaultConfiguration);
    when(defaultConfiguration.getLevel()).thenReturn(WARNING);

    return run;
  }

  private Run mockUnsupportedRuleSeveritiesDefinition() {
    when(run.getTool()).thenReturn(tool);
    when(tool.getDriver()).thenReturn(driver);
    when(driver.getRules()).thenReturn(Set.of());
    when(tool.getExtensions()).thenReturn(Set.of(extension));
    when(extension.getRules()).thenReturn(Set.of());

    return run;
  }

  private void assertNoLogs() {
    assertThat(logTester.logs()).isEmpty();
  }

  private static void assertDetectedRuleSeverities(Map<String, String> severities, Tuple... expectedSeverities) {
    Assertions.assertThat(severities.entrySet())
      .extracting(Map.Entry::getKey, Map.Entry::getValue)
      .containsExactly(expectedSeverities);
  }

  private void assertWarningLog(String driverName, Severity defaultSeverity) {
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.WARN))
      .containsOnly(format(UNSUPPORTED_RULE_SEVERITIES_WARNING, driverName, defaultSeverity));
  }

}
