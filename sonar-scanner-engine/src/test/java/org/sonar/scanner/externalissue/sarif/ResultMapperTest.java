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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rules.RuleType;
import org.sonar.sarif.pojo.Location;
import org.sonar.sarif.pojo.Message;
import org.sonar.sarif.pojo.Result;
import org.sonar.sarif.pojo.Stack;
import org.sonar.sarif.pojo.StackFrame;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.sarif.pojo.Result.Level.WARNING;
import static org.sonar.scanner.externalissue.sarif.ResultMapper.DEFAULT_IMPACT_SEVERITY;
import static org.sonar.scanner.externalissue.sarif.ResultMapper.DEFAULT_SEVERITY;
import static org.sonar.scanner.externalissue.sarif.ResultMapper.DEFAULT_SOFTWARE_QUALITY;

@RunWith(DataProviderRunner.class)
public class ResultMapperTest {

  private static final String RULE_ID = "test_rules_id";
  private static final String DRIVER_NAME = "driverName";

  @Mock
  private LocationMapper locationMapper;

  @Mock
  private SensorContext sensorContext;

  @Mock
  private NewExternalIssue mockExternalIssue;

  @Mock
  private NewIssueLocation newExternalIssueLocation;

  private Result result;

  @InjectMocks
  ResultMapper resultMapper;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    result = new Result().withMessage(new Message().withText("Result message"));
    result.withRuleId(RULE_ID);
    when(sensorContext.newExternalIssue()).thenReturn(mockExternalIssue);
    when(locationMapper.fillIssueInFileLocation(any(), any())).thenReturn(true);
    when(mockExternalIssue.newLocation()).thenReturn(newExternalIssueLocation);
  }

  @Test
  public void mapResult_mapsSimpleFieldsCorrectly() {
    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(newExternalIssue).type(RuleType.VULNERABILITY);
    verify(newExternalIssue).engineId(DRIVER_NAME);
    verify(newExternalIssue).severity(DEFAULT_SEVERITY);
    verify(newExternalIssue).ruleId(RULE_ID);
  }

  @Test
  public void mapResult_ifRuleIdMissing_fails() {
    result.withRuleId(null);
    assertThatNullPointerException()
      .isThrownBy(() -> resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result))
      .withMessage("No ruleId found for issue thrown by driver driverName");
  }

  @Test
  public void mapResult_whenLocationExists_createsFileLocation() {
    Location location = new Location();
    result.withLocations(List.of(location));

    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInFileLocation(newExternalIssueLocation, location);
    verifyNoMoreInteractions(locationMapper);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue, never()).addLocation(any());
    verify(newExternalIssue, never()).addFlow(any());
  }

  @Test
  public void mapResult_useResultMessageForIssue() {
    Location location = new Location();
    result.withLocations(List.of(location));

    resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(newExternalIssueLocation).message("Result message");
  }

  @Test
  public void mapResult_concatResultMessageAndLocationMessageForIssue() {
    Location location = new Location().withMessage(new Message().withText("Location message"));
    result.withLocations(List.of(location));

    resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(newExternalIssueLocation).message("Result message - Location message");
  }

  @Test
  public void mapResult_whenRelatedLocationExists_createsSecondaryFileLocation() {
    Location relatedLocation = new Location().withMessage(new Message().withText("Related location message"));
    result.withRelatedLocations(Set.of(relatedLocation));
    var newIssueLocationCall2 = mock(NewIssueLocation.class);
    when(mockExternalIssue.newLocation()).thenReturn(newExternalIssueLocation, newIssueLocationCall2);

    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInProjectLocation(newExternalIssueLocation);
    verify(locationMapper).fillIssueInFileLocation(newIssueLocationCall2, relatedLocation);
    verifyNoMoreInteractions(locationMapper);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue).addLocation(newIssueLocationCall2);
    verify(newExternalIssue, never()).addFlow(any());
    verify(newIssueLocationCall2).message("Related location message");
  }

  @Test
  public void mapResult_whenRelatedLocationExists_createsSecondaryFileLocation_no_messages() {
    Location relatedLocationWithoutMessage = new Location();
    result.withRelatedLocations(Set.of(relatedLocationWithoutMessage));
    var newIssueLocationCall2 = mock(NewIssueLocation.class);
    when(mockExternalIssue.newLocation()).thenReturn(newExternalIssueLocation, newIssueLocationCall2);

    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(newExternalIssue).addLocation(newIssueLocationCall2);
    verify(newIssueLocationCall2, never()).message(anyString());
  }

  @Test
  public void mapResult_whenStacksLocationExists_createsCodeFlowFileLocation() {
    Location stackFrameLocation = new Location().withMessage(new Message().withText("Stack frame location message"));
    var stackWithFrame = new Stack().withFrames(List.of(new StackFrame().withLocation(stackFrameLocation)));
    var stackWithFrameNoLocation = new Stack().withFrames(List.of(new StackFrame()));
    var stackWithoutFrame = new Stack().withFrames(List.of());
    var stackWithoutFrame2 = new Stack();
    result.withStacks(Set.of(stackWithFrame, stackWithFrameNoLocation, stackWithoutFrame, stackWithoutFrame2));
    var newIssueLocationCall2 = mock(NewIssueLocation.class);
    when(mockExternalIssue.newLocation()).thenReturn(newExternalIssueLocation, newIssueLocationCall2);

    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInProjectLocation(newExternalIssueLocation);
    verify(locationMapper).fillIssueInFileLocation(newIssueLocationCall2, stackFrameLocation);
    verifyNoMoreInteractions(locationMapper);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue).addFlow(List.of(newIssueLocationCall2));
    verify(newExternalIssue, never()).addLocation(any());
    verify(newIssueLocationCall2).message("Stack frame location message");
  }

  @Test
  public void mapResult_whenStacksLocationExists_createsCodeFlowFileLocation_no_text_messages() {
    Location stackFrameLocationWithoutMessage = new Location().withMessage(new Message().withId("1"));
    result.withStacks(Set.of(new Stack().withFrames(List.of(new StackFrame().withLocation(stackFrameLocationWithoutMessage)))));
    var newIssueLocationCall2 = mock(NewIssueLocation.class);
    when(mockExternalIssue.newLocation()).thenReturn(newExternalIssueLocation, newIssueLocationCall2);

    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(newExternalIssue).addFlow(List.of(newIssueLocationCall2));
    verify(newIssueLocationCall2, never()).message(anyString());
  }

  @Test
  public void mapResult_whenLocationExistsButLocationMapperReturnsFalse_createsProjectLocation() {
    Location location = new Location();
    result.withLocations(List.of(location));
    when(locationMapper.fillIssueInFileLocation(any(), any())).thenReturn(false);

    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInProjectLocation(newExternalIssueLocation);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue, never()).addLocation(any());
    verify(newExternalIssue, never()).addFlow(any());
  }

  @Test
  public void mapResult_whenLocationsIsEmpty_createsProjectLocation() {
    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);
    result.withLocations(List.of());

    verify(locationMapper).fillIssueInProjectLocation(newExternalIssueLocation);
    verifyNoMoreInteractions(locationMapper);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue, never()).addLocation(any());
    verify(newExternalIssue, never()).addFlow(any());
  }

  @Test
  public void mapResult_whenLocationsIsNull_createsProjectLocation() {
    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInProjectLocation(newExternalIssueLocation);
    verifyNoMoreInteractions(locationMapper);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue, never()).addLocation(any());
    verify(newExternalIssue, never()).addFlow(any());
  }

  @DataProvider
  public static Object[][] rule_severity_to_sonarqube_severity_mapping() {
    return new Object[][] {
      {Result.Level.ERROR, Severity.CRITICAL, org.sonar.api.issue.impact.Severity.HIGH},
      {WARNING, Severity.MAJOR, org.sonar.api.issue.impact.Severity.MEDIUM},
      {Result.Level.NOTE, Severity.MINOR, org.sonar.api.issue.impact.Severity.LOW},
      {Result.Level.NONE, Severity.INFO, org.sonar.api.issue.impact.Severity.LOW},
      {null, DEFAULT_SEVERITY, DEFAULT_IMPACT_SEVERITY},
    };
  }

  @Test
  @UseDataProvider("rule_severity_to_sonarqube_severity_mapping")
  public void mapResult_mapsCorrectlyLevelToSeverity(Result.Level ruleSeverity, Severity sonarQubeSeverity, org.sonar.api.issue.impact.Severity impactSeverity) {
    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, ruleSeverity, ruleSeverity, result);
    verify(newExternalIssue).severity(sonarQubeSeverity);
    verify(newExternalIssue).addImpact(DEFAULT_SOFTWARE_QUALITY, impactSeverity);
  }

  @Test
  public void mapResult_mapsCorrectlyCleanCodeAttribute() {
    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);
    verify(newExternalIssue).cleanCodeAttribute(ResultMapper.DEFAULT_CLEAN_CODE_ATTRIBUTE);
  }

}
