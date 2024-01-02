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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.sarif.Result;
import org.sonar.core.sarif.Run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RunMapperTest {
  private static final String WARNING = "warning";
  private static final String TEST_DRIVER = "Test driver";
  public static final String RULE_ID = "ruleId";

  @Mock
  private ResultMapper resultMapper;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Run run;

  @Rule
  public LogTester logTester = new LogTester();

  @InjectMocks
  private RunMapper runMapper;

  @Test
  public void mapRun_delegatesToMapResult() {
    when(run.getTool().getDriver().getName()).thenReturn(TEST_DRIVER);
    Result result1 = mock(Result.class);
    Result result2 = mock(Result.class);
    when(run.getResults()).thenReturn(Set.of(result1, result2));
    NewExternalIssue externalIssue1 = mockMappedResult(result1);
    NewExternalIssue externalIssue2 = mockMappedResult(result2);

    try (MockedStatic<RulesSeverityDetector> detector = mockStatic(RulesSeverityDetector.class)) {
      detector.when(() -> RulesSeverityDetector.detectRulesSeverities(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));

      List<NewExternalIssue> newExternalIssues = runMapper.mapRun(run);

      assertThat(newExternalIssues)
        .containsOnly(externalIssue1, externalIssue2);
    }
  }

  @Test
  public void mapRun_ifExceptionThrownByResultMapper_logsThemAndContinueProcessing() {
    when(run.getTool().getDriver().getName()).thenReturn(TEST_DRIVER);
    Result result1 = mock(Result.class);
    Result result2 = mock(Result.class);
    when(run.getResults()).thenReturn(Set.of(result1, result2));
    NewExternalIssue externalIssue2 = mockMappedResult(result2);
    when(result1.getRuleId()).thenReturn(RULE_ID);
    when(resultMapper.mapResult(TEST_DRIVER, WARNING, result1)).thenThrow(new IllegalArgumentException("test"));

    try (MockedStatic<RulesSeverityDetector> detector = mockStatic(RulesSeverityDetector.class)) {
      detector.when(() -> RulesSeverityDetector.detectRulesSeverities(run, TEST_DRIVER)).thenReturn(Map.of(RULE_ID, WARNING));

      List<NewExternalIssue> newExternalIssues = runMapper.mapRun(run);

      assertThat(newExternalIssues)
        .containsExactly(externalIssue2);
      assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly("Failed to import an issue raised by tool Test driver, error: test");
    }
  }

  @Test
  public void mapRun_failsIfToolNotSet() {
    when(run.getTool()).thenReturn(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> runMapper.mapRun(run))
      .withMessage("The run does not have a tool driver name defined.");
  }

  @Test
  public void mapRun_failsIfDriverNotSet() {
    when(run.getTool().getDriver()).thenReturn(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> runMapper.mapRun(run))
      .withMessage("The run does not have a tool driver name defined.");
  }

  @Test
  public void mapRun_failsIfDriverNameIsNotSet() {
    when(run.getTool().getDriver().getName()).thenReturn(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> runMapper.mapRun(run))
      .withMessage("The run does not have a tool driver name defined.");
  }

  private NewExternalIssue mockMappedResult(Result result) {
    NewExternalIssue externalIssue = mock(NewExternalIssue.class);
    when(result.getRuleId()).thenReturn(RULE_ID);
    when(resultMapper.mapResult(TEST_DRIVER, WARNING, result)).thenReturn(externalIssue);
    return externalIssue;
  }

}
