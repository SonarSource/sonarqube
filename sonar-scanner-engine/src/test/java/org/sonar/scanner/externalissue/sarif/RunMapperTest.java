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
package org.sonar.scanner.externalissue.sarif;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.sarif.pojo.ReportingDescriptor;
import org.sonar.sarif.pojo.Result;
import org.sonar.sarif.pojo.Run;
import org.sonar.sarif.pojo.ToolComponent;
import org.sonar.scanner.externalissue.sarif.RunMapper.RunMapperResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.sonar.sarif.pojo.Result.Level.WARNING;

@ExtendWith(MockitoExtension.class)
class RunMapperTest {
  private static final String TEST_DRIVER = "Test driver";
  public static final String RULE_ID = "ruleId";

  @Mock
  private ResultMapper resultMapper;

  @Mock
  private RuleMapper ruleMapper;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Run run;

  @Mock
  private ReportingDescriptor rule;

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @InjectMocks
  private RunMapper runMapper;

  @BeforeEach
  void setUp() {
    lenient().when(run.getTool().getDriver().getName()).thenReturn(TEST_DRIVER);
    lenient().when(run.getTool().getExtensions()).thenReturn(null);
    lenient().when(rule.getId()).thenReturn(RULE_ID);
  }

  @Test
  void mapRun_shouldMapExternalIssues() {
    Result result1 = mock(Result.class);
    Result result2 = mock(Result.class);
    when(run.getResults()).thenReturn(List.of(result1, result2));
    NewExternalIssue externalIssue1 = mockMappedExternalIssue(result1);
    NewExternalIssue externalIssue2 = mockMappedExternalIssue(result2);

    try (MockedStatic<RulesSeverityDetector> detector = mockStatic(RulesSeverityDetector.class)) {
      detector.when(() -> RulesSeverityDetector.detectRulesSeverities(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));
      detector.when(() -> RulesSeverityDetector.detectRulesSeveritiesForNewTaxonomy(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));

      RunMapperResult runMapperResult = runMapper.mapRun(run);

      assertThat(runMapperResult.getNewExternalIssues()).containsOnly(externalIssue1, externalIssue2);
      assertThat(logTester.logs()).isEmpty();
    }
  }

  @Test
  void mapRun_shouldMapExternalRules_whenDriverHasRulesAndNoExtensions() {
    when(run.getTool().getDriver().getRules()).thenReturn(Set.of(rule));
    NewAdHocRule externalRule = mockMappedExternalRule();

    try (MockedStatic<RulesSeverityDetector> detector = mockStatic(RulesSeverityDetector.class)) {
      detector.when(() -> RulesSeverityDetector.detectRulesSeverities(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));
      detector.when(() -> RulesSeverityDetector.detectRulesSeveritiesForNewTaxonomy(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));

      RunMapperResult runMapperResult = runMapper.mapRun(run);

      assertThat(runMapperResult.getNewAdHocRules()).containsOnly(externalRule);
      assertThat(logTester.logs()).isEmpty();
    }
  }

  @Test
  void mapRun_shouldMapExternalRules_whenRulesInExtensions() {
    when(run.getTool().getDriver().getRules()).thenReturn(Set.of());
    ToolComponent extension = mock(ToolComponent.class);
    when(extension.getRules()).thenReturn(Set.of(rule));
    when(run.getTool().getExtensions()).thenReturn(Set.of(extension));
    NewAdHocRule externalRule = mockMappedExternalRule();

    try (MockedStatic<RulesSeverityDetector> detector = mockStatic(RulesSeverityDetector.class)) {
      detector.when(() -> RulesSeverityDetector.detectRulesSeverities(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));
      detector.when(() -> RulesSeverityDetector.detectRulesSeveritiesForNewTaxonomy(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));

      RunMapperResult runMapperResult = runMapper.mapRun(run);

      assertThat(runMapperResult.getNewAdHocRules()).containsOnly(externalRule);
      assertThat(logTester.logs()).isEmpty();
    }
  }

  @Test
  void mapRun_shouldNotFail_whenExtensionsDontHaveRules() {
    when(run.getTool().getDriver().getRules()).thenReturn(Set.of(rule));
    ToolComponent extension = mock(ToolComponent.class);
    when(extension.getRules()).thenReturn(null);
    when(run.getTool().getExtensions()).thenReturn(Set.of(extension));

    try (MockedStatic<RulesSeverityDetector> detector = mockStatic(RulesSeverityDetector.class)) {
      detector.when(() -> RulesSeverityDetector.detectRulesSeverities(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));
      detector.when(() -> RulesSeverityDetector.detectRulesSeveritiesForNewTaxonomy(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));

      assertThatNoException().isThrownBy(() -> runMapper.mapRun(run));
    }
  }

  @Test
  void mapRun_shouldNotFail_whenExtensionsHaveEmptyRules() {
    when(run.getTool().getDriver().getRules()).thenReturn(Set.of(rule));
    ToolComponent extension = mock(ToolComponent.class);
    when(extension.getRules()).thenReturn(Set.of());
    when(run.getTool().getExtensions()).thenReturn(Set.of(extension));

    try (MockedStatic<RulesSeverityDetector> detector = mockStatic(RulesSeverityDetector.class)) {
      detector.when(() -> RulesSeverityDetector.detectRulesSeverities(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));
      detector.when(() -> RulesSeverityDetector.detectRulesSeveritiesForNewTaxonomy(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));

      assertThatNoException().isThrownBy(() -> runMapper.mapRun(run));
    }
  }

  @Test
  void mapRun_ifRunIsEmpty_returnsEmptyList() {
    when(run.getResults()).thenReturn(List.of());

    RunMapperResult runMapperResult = runMapper.mapRun(run);

    assertThat(runMapperResult.getNewExternalIssues()).isEmpty();
  }

  @Test
  void mapRun_ifExceptionThrownByResultMapper_logsThemAndContinueProcessing() {
    Result result1 = mock(Result.class);
    Result result2 = mock(Result.class);
    when(run.getResults()).thenReturn(List.of(result1, result2));
    NewExternalIssue externalIssue2 = mockMappedExternalIssue(result2);
    when(result1.getRuleId()).thenReturn(RULE_ID);
    when(resultMapper.mapResult(TEST_DRIVER, WARNING, WARNING, result1)).thenThrow(new IllegalArgumentException("test"));

    try (MockedStatic<RulesSeverityDetector> detector = mockStatic(RulesSeverityDetector.class)) {
      detector.when(() -> RulesSeverityDetector.detectRulesSeverities(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));
      detector.when(() -> RulesSeverityDetector.detectRulesSeveritiesForNewTaxonomy(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));

      RunMapperResult runMapperResult = runMapper.mapRun(run);

      assertThat(runMapperResult.getNewExternalIssues())
        .containsExactly(externalIssue2);
      assertThat(logTester.logs(Level.WARN)).containsOnly("Failed to import an issue raised by tool Test driver, error: test");
    }
  }

  @Test
  void mapRun_failsIfToolNotSet() {
    when(run.getTool()).thenReturn(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> runMapper.mapRun(run))
      .withMessage("The run does not have a tool driver name defined.");
  }

  @Test
  void mapRun_failsIfDriverNotSet() {
    when(run.getTool().getDriver()).thenReturn(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> runMapper.mapRun(run))
      .withMessage("The run does not have a tool driver name defined.");
  }

  @Test
  void mapRun_failsIfDriverNameIsNotSet() {
    when(run.getTool().getDriver().getName()).thenReturn(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> runMapper.mapRun(run))
      .withMessage("The run does not have a tool driver name defined.");
  }

  @Test
  void mapRun_shouldNotFail_whenDriverRulesNullAndExtensionsRulesNotNull() {
    when(run.getTool().getDriver().getRules()).thenReturn(null);
    ToolComponent extension = mock(ToolComponent.class);
    when(extension.getRules()).thenReturn(Set.of(rule));
    when(run.getTool().getExtensions()).thenReturn(Set.of(extension));
    NewAdHocRule expectedRule = mock(NewAdHocRule.class);
    when(ruleMapper.mapRule(rule, TEST_DRIVER, WARNING, WARNING)).thenReturn(expectedRule);

    try (MockedStatic<RulesSeverityDetector> detector = mockStatic(RulesSeverityDetector.class)) {
      detector.when(() -> RulesSeverityDetector.detectRulesSeverities(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));
      detector.when(() -> RulesSeverityDetector.detectRulesSeveritiesForNewTaxonomy(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));

      RunMapperResult runMapperResult = runMapper.mapRun(run);
      assertThat(runMapperResult.getNewAdHocRules()).hasSize(1);
      assertThat(runMapperResult.getNewAdHocRules().get(0)).isEqualTo(expectedRule);
    }
  }

  private NewExternalIssue mockMappedExternalIssue(Result result) {
    NewExternalIssue externalIssue = mock(NewExternalIssue.class);
    when(result.getRuleId()).thenReturn(RULE_ID);
    when(resultMapper.mapResult(TEST_DRIVER, WARNING, WARNING, result)).thenReturn(externalIssue);
    return externalIssue;
  }

  private NewAdHocRule mockMappedExternalRule() {
    NewAdHocRule externalRule = mock(NewAdHocRule.class);
    when(ruleMapper.mapRule(rule, TEST_DRIVER, WARNING, WARNING)).thenReturn(externalRule);
    return externalRule;
  }

}
