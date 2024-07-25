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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.sarif.pojo.Location;
import org.sonar.sarif.pojo.Result;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.rules.CleanCodeAttribute.CONVENTIONAL;

@ScannerSide
public class ResultMapper {

  private static final Map<Result.Level, Severity> SEVERITY_MAPPING = ImmutableMap.<Result.Level, Severity>builder()
    .put(Result.Level.ERROR, Severity.CRITICAL)
    .put(Result.Level.WARNING, Severity.MAJOR)
    .put(Result.Level.NOTE, Severity.MINOR)
    .put(Result.Level.NONE, Severity.INFO)
    .build();

  private static final Map<Result.Level, org.sonar.api.issue.impact.Severity> IMPACT_SEVERITY_MAPPING = ImmutableMap.<Result.Level, org.sonar.api.issue.impact.Severity>builder()
    .put(Result.Level.ERROR, HIGH)
    .put(Result.Level.WARNING, MEDIUM)
    .put(Result.Level.NOTE, LOW)
    .put(Result.Level.NONE, LOW)
    .build();

  public static final Severity DEFAULT_SEVERITY = Severity.MAJOR;
  public static final RuleType DEFAULT_TYPE = RuleType.VULNERABILITY;
  public static final CleanCodeAttribute DEFAULT_CLEAN_CODE_ATTRIBUTE = CONVENTIONAL;
  public static final SoftwareQuality DEFAULT_SOFTWARE_QUALITY = SECURITY;
  public static final org.sonar.api.issue.impact.Severity DEFAULT_IMPACT_SEVERITY = MEDIUM;

  private final SensorContext sensorContext;
  private final LocationMapper locationMapper;

  ResultMapper(SensorContext sensorContext, LocationMapper locationMapper) {
    this.sensorContext = sensorContext;
    this.locationMapper = locationMapper;
  }

  NewExternalIssue mapResult(String driverName, @Nullable Result.Level ruleSeverity, @Nullable Result.Level ruleSeverityForNewTaxonomy, Result result) {
    NewExternalIssue newExternalIssue = sensorContext.newExternalIssue();
    newExternalIssue.type(DEFAULT_TYPE);
    newExternalIssue.engineId(driverName);
    newExternalIssue.severity(toSonarQubeSeverity(ruleSeverity));
    newExternalIssue.ruleId(requireNonNull(result.getRuleId(), "No ruleId found for issue thrown by driver " + driverName));
    newExternalIssue.cleanCodeAttribute(DEFAULT_CLEAN_CODE_ATTRIBUTE);
    newExternalIssue.addImpact(DEFAULT_SOFTWARE_QUALITY, toSonarQubeImpactSeverity(ruleSeverityForNewTaxonomy));

    mapLocations(result, newExternalIssue);
    return newExternalIssue;
  }

  protected static org.sonar.api.issue.impact.Severity toSonarQubeImpactSeverity(@Nullable Result.Level ruleSeverity) {
    return IMPACT_SEVERITY_MAPPING.getOrDefault(ruleSeverity, DEFAULT_IMPACT_SEVERITY);
  }

  protected static Severity toSonarQubeSeverity(@Nullable Result.Level ruleSeverity) {
    return SEVERITY_MAPPING.getOrDefault(ruleSeverity, DEFAULT_SEVERITY);
  }

  private void mapLocations(Result result, NewExternalIssue newExternalIssue) {
    NewIssueLocation newIssueLocation = newExternalIssue.newLocation();
    List<Location> locations = result.getLocations();
    if (locations == null || locations.isEmpty()) {
      newExternalIssue.at(locationMapper.fillIssueInProjectLocation(result, newIssueLocation));
    } else {
      Location firstLocation = locations.iterator().next();
      NewIssueLocation primaryLocation = fillFileOrProjectLocation(result, newIssueLocation, firstLocation);
      newExternalIssue.at(primaryLocation);
    }

    Set<Location> relatedLocations = result.getRelatedLocations();
    if (relatedLocations != null && !relatedLocations.isEmpty()) {
      relatedLocations.forEach(relatedLocation -> {
        NewIssueLocation newRelatedLocation = newExternalIssue.newLocation();
        fillFileOrProjectLocation(result, newRelatedLocation, relatedLocation);
        newExternalIssue.addLocation(newRelatedLocation);
      });
    }
  }

  private NewIssueLocation fillFileOrProjectLocation(Result result, NewIssueLocation newIssueLocation, Location firstLocation) {
    return Optional.ofNullable(locationMapper.fillIssueInFileLocation(result, newIssueLocation, firstLocation))
      .orElseGet(() -> locationMapper.fillIssueInProjectLocation(result, newIssueLocation));
  }

}
