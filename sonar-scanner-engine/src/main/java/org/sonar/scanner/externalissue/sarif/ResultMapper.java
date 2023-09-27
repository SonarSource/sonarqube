/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.core.sarif.Location;
import org.sonar.core.sarif.Result;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.rules.CleanCodeAttribute.CONVENTIONAL;

@ScannerSide
public class ResultMapper {

  private static final Map<String, Severity> SEVERITY_MAPPING = ImmutableMap.<String, Severity>builder()
    .put("error", Severity.CRITICAL)
    .put("warning", Severity.MAJOR)
    .put("note", Severity.MINOR)
    .put("none", Severity.INFO)
    .build();

  private static final Map<String, org.sonar.api.issue.impact.Severity> IMPACT_SEVERITY_MAPPING = ImmutableMap.<String, org.sonar.api.issue.impact.Severity>builder()
    .put("error", HIGH)
    .put("warning", MEDIUM)
    .put("note", LOW)
    .put("none", LOW)
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

  NewExternalIssue mapResult(String driverName, @Nullable String ruleSeverity, @Nullable String ruleSeverityForNewTaxonomy, Result result) {
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

  protected static org.sonar.api.issue.impact.Severity toSonarQubeImpactSeverity(@Nullable String ruleSeverity) {
    return IMPACT_SEVERITY_MAPPING.getOrDefault(ruleSeverity, DEFAULT_IMPACT_SEVERITY);
  }

  protected static Severity toSonarQubeSeverity(@Nullable String ruleSeverity) {
    return SEVERITY_MAPPING.getOrDefault(ruleSeverity, DEFAULT_SEVERITY);
  }

  private void mapLocations(Result result, NewExternalIssue newExternalIssue) {
    NewIssueLocation newIssueLocation = newExternalIssue.newLocation();
    Set<Location> locations = result.getLocations();
    if (locations == null || locations.isEmpty()) {
      newExternalIssue.at(locationMapper.fillIssueInProjectLocation(result, newIssueLocation));
    } else {
      Location firstLocation = locations.iterator().next();
      NewIssueLocation primaryLocation = fillFileOrProjectLocation(result, newIssueLocation, firstLocation);
      newExternalIssue.at(primaryLocation);
    }
  }

  private NewIssueLocation fillFileOrProjectLocation(Result result, NewIssueLocation newIssueLocation, Location firstLocation) {
    return Optional.ofNullable(locationMapper.fillIssueInFileLocation(result, newIssueLocation, firstLocation))
      .orElseGet(() -> locationMapper.fillIssueInProjectLocation(result, newIssueLocation));
  }

}
