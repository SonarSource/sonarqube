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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rules.RuleType;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.core.sarif.Location;
import org.sonar.core.sarif.Result;

import static java.util.Objects.requireNonNull;

@ScannerSide
public class ResultMapper {
  public static final Severity DEFAULT_SEVERITY = Severity.MAJOR;

  private static final Map<String, Severity> SEVERITY_MAPPING = ImmutableMap.<String, Severity>builder()
    .put("error", Severity.CRITICAL)
    .put("warning", Severity.MAJOR)
    .put("note", Severity.MINOR)
    .put("none", Severity.INFO)
    .build();

  private static final RuleType DEFAULT_TYPE = RuleType.VULNERABILITY;

  private final SensorContext sensorContext;
  private final LocationMapper locationMapper;

  ResultMapper(SensorContext sensorContext, LocationMapper locationMapper) {
    this.sensorContext = sensorContext;
    this.locationMapper = locationMapper;
  }

  NewExternalIssue mapResult(String driverName, @Nullable String ruleSeverity, Result result) {
    NewExternalIssue newExternalIssue = sensorContext.newExternalIssue();
    newExternalIssue.type(DEFAULT_TYPE);
    newExternalIssue.engineId(driverName);
    newExternalIssue.severity(toSonarQubeSeverity(ruleSeverity));
    newExternalIssue.ruleId(requireNonNull(result.getRuleId(), "No ruleId found for issue thrown by driver " + driverName));

    mapLocations(result, newExternalIssue);
    return newExternalIssue;
  }

  private static Severity toSonarQubeSeverity(@Nullable String ruleSeverity) {
    return SEVERITY_MAPPING.getOrDefault(ruleSeverity, DEFAULT_SEVERITY);
  }

  private void mapLocations(Result result, NewExternalIssue newExternalIssue) {
    NewIssueLocation newIssueLocation = newExternalIssue.newLocation();
    if (result.getLocations().isEmpty()) {
      newExternalIssue.at(locationMapper.fillIssueInProjectLocation(result, newIssueLocation));
    } else {
      Location firstLocation = result.getLocations().iterator().next();
      NewIssueLocation primaryLocation = fillFileOrProjectLocation(result, newIssueLocation, firstLocation);
      newExternalIssue.at(primaryLocation);
    }
  }

  private NewIssueLocation fillFileOrProjectLocation(Result result, NewIssueLocation newIssueLocation, Location firstLocation) {
    return Optional.ofNullable(locationMapper.fillIssueInFileLocation(result, newIssueLocation, firstLocation))
      .orElseGet(() -> locationMapper.fillIssueInProjectLocation(result, newIssueLocation));
  }

}
