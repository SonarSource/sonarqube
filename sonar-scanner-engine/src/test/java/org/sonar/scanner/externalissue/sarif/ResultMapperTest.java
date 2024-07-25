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
import org.sonar.sarif.pojo.Result;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
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

  @Mock
  private Result result;

  @InjectMocks
  ResultMapper resultMapper;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(result.getRuleId()).thenReturn(RULE_ID);
    when(sensorContext.newExternalIssue()).thenReturn(mockExternalIssue);
    when(locationMapper.fillIssueInFileLocation(any(), any(), any())).thenReturn(newExternalIssueLocation);
    when(locationMapper.fillIssueInProjectLocation(any(), any())).thenReturn(newExternalIssueLocation);
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
    when(result.getRuleId()).thenReturn(null);
    assertThatNullPointerException()
      .isThrownBy(() -> resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result))
      .withMessage("No ruleId found for issue thrown by driver driverName");
  }

  @Test
  public void mapResult_whenLocationExists_createsFileLocation() {
    Location location = mock(Location.class);
    when(result.getLocations()).thenReturn(List.of(location));

    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInFileLocation(result, newExternalIssueLocation, location);
    verifyNoMoreInteractions(locationMapper);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue, never()).addLocation(any());
    verify(newExternalIssue, never()).addFlow(any());
  }

  @Test
  public void mapResult_whenRelatedLocationExists_createsSecondaryFileLocation() {
    Location location = mock(Location.class);
    when(result.getRelatedLocations()).thenReturn(Set.of(location));
    var newIssueLocationCall2 = mock(NewIssueLocation.class);
    when(mockExternalIssue.newLocation()).thenReturn(newExternalIssueLocation, newIssueLocationCall2);

    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInProjectLocation(result, newExternalIssueLocation);
    verify(locationMapper).fillIssueInFileLocation(result, newIssueLocationCall2, location);
    verifyNoMoreInteractions(locationMapper);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue).addLocation(newIssueLocationCall2);
    verify(newExternalIssue, never()).addFlow(any());
  }

  @Test
  public void mapResult_whenLocationExistsButLocationMapperReturnsNull_createsProjectLocation() {
    Location location = mock(Location.class);
    when(result.getLocations()).thenReturn(List.of(location));
    when(locationMapper.fillIssueInFileLocation(any(), any(), any())).thenReturn(null);

    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInProjectLocation(result, newExternalIssueLocation);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue, never()).addLocation(any());
    verify(newExternalIssue, never()).addFlow(any());
  }

  @Test
  public void mapResult_whenLocationsIsEmpty_createsProjectLocation() {
    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInProjectLocation(result, newExternalIssueLocation);
    verifyNoMoreInteractions(locationMapper);
    verify(newExternalIssue).at(newExternalIssueLocation);
    verify(newExternalIssue, never()).addLocation(any());
    verify(newExternalIssue, never()).addFlow(any());
  }

  @Test
  public void mapResult_whenLocationsIsNull_createsProjectLocation() {
    when(result.getLocations()).thenReturn(null);
    NewExternalIssue newExternalIssue = resultMapper.mapResult(DRIVER_NAME, WARNING, WARNING, result);

    verify(locationMapper).fillIssueInProjectLocation(result, newExternalIssueLocation);
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
