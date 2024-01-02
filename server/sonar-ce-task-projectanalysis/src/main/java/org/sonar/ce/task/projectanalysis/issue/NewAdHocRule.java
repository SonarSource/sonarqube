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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.internal.ImpactMapper;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.trimToNull;

@Immutable
public class NewAdHocRule {
  private final RuleKey key;
  private final String engineId;
  private final String ruleId;
  private final String name;
  private final String description;
  private final String severity;
  private final RuleType ruleType;
  private final boolean hasDetails;
  private CleanCodeAttribute cleanCodeAttribute = null;

  private final Map<SoftwareQuality, Severity> defaultImpacts = new EnumMap<>(SoftwareQuality.class);

  public NewAdHocRule(ScannerReport.AdHocRule ruleFromScannerReport) {
    Preconditions.checkArgument(isNotBlank(ruleFromScannerReport.getEngineId()), "'engine id' not expected to be null for an ad hoc rule");
    Preconditions.checkArgument(isNotBlank(ruleFromScannerReport.getRuleId()), "'rule id' not expected to be null for an ad hoc rule");
    Preconditions.checkArgument(isNotBlank(ruleFromScannerReport.getName()), "'name' not expected to be null for an ad hoc rule");
    Preconditions.checkArgument(!ruleFromScannerReport.getDefaultImpactsList().isEmpty() || ruleFromScannerReport.getSeverity() != Constants.Severity.UNSET_SEVERITY,
      "'severity' not expected to be null for an ad hoc rule, or impacts should be provided instead");
    Preconditions.checkArgument(!ruleFromScannerReport.getDefaultImpactsList().isEmpty() || ruleFromScannerReport.getType() != ScannerReport.IssueType.UNSET,
      "'issue type' not expected to be null for an ad hoc rule, or impacts should be provided instead");
    this.key = RuleKey.of(RuleKey.EXTERNAL_RULE_REPO_PREFIX + ruleFromScannerReport.getEngineId(), ruleFromScannerReport.getRuleId());
    this.engineId = ruleFromScannerReport.getEngineId();
    this.ruleId = ruleFromScannerReport.getRuleId();
    this.name = ruleFromScannerReport.getName();
    this.description = trimToNull(ruleFromScannerReport.getDescription());
    this.hasDetails = true;
    this.ruleType = determineType(ruleFromScannerReport);
    this.severity = determineSeverity(ruleFromScannerReport);
    if (!ScannerReport.IssueType.SECURITY_HOTSPOT.equals(ruleFromScannerReport.getType())) {
      this.cleanCodeAttribute = mapCleanCodeAttribute(trimToNull(ruleFromScannerReport.getCleanCodeAttribute()));
      this.defaultImpacts.putAll(determineImpacts(ruleFromScannerReport));
    }
  }

  public NewAdHocRule(ScannerReport.ExternalIssue fromIssue) {
    Preconditions.checkArgument(isNotBlank(fromIssue.getEngineId()), "'engine id' not expected to be null for an ad hoc rule");
    Preconditions.checkArgument(isNotBlank(fromIssue.getRuleId()), "'rule id' not expected to be null for an ad hoc rule");
    this.key = RuleKey.of(RuleKey.EXTERNAL_RULE_REPO_PREFIX + fromIssue.getEngineId(), fromIssue.getRuleId());
    this.engineId = fromIssue.getEngineId();
    this.ruleId = fromIssue.getRuleId();
    this.name = null;
    this.description = null;
    this.severity = null;
    this.ruleType = null;
    this.hasDetails = false;
    if (!ScannerReport.IssueType.SECURITY_HOTSPOT.equals(fromIssue.getType())) {
      this.cleanCodeAttribute = CleanCodeAttribute.defaultCleanCodeAttribute();
      this.defaultImpacts.put(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM);
    }
  }

  private Map<SoftwareQuality, Severity> determineImpacts(ScannerReport.AdHocRule ruleFromScannerReport) {
    if (ruleFromScannerReport.getType().equals(ScannerReport.IssueType.SECURITY_HOTSPOT)) {
      return Collections.emptyMap();
    }
    Map<SoftwareQuality, Severity> impacts = mapImpacts(ruleFromScannerReport.getDefaultImpactsList());
    if (impacts.isEmpty()) {
      return Map.of(ImpactMapper.convertToSoftwareQuality(this.ruleType),
        ImpactMapper.convertToImpactSeverity(this.severity));
    } else {
      return impacts;
    }
  }

  private static RuleType determineType(ScannerReport.AdHocRule ruleFromScannerReport) {
    if (ruleFromScannerReport.getType() != ScannerReport.IssueType.UNSET) {
      return RuleType.valueOf(ruleFromScannerReport.getType().name());
    }
    Map<SoftwareQuality, Severity> impacts = mapImpacts(ruleFromScannerReport.getDefaultImpactsList());
    Map.Entry<SoftwareQuality, Severity> bestImpactForBackMapping = ImpactMapper.getBestImpactForBackmapping(impacts);
    return ImpactMapper.convertToRuleType(bestImpactForBackMapping.getKey());
  }

  private static String determineSeverity(ScannerReport.AdHocRule ruleFromScannerReport) {
    if (ruleFromScannerReport.getSeverity() != Constants.Severity.UNSET_SEVERITY) {
      return ruleFromScannerReport.getSeverity().name();
    }
    Map<SoftwareQuality, Severity> impacts = mapImpacts(ruleFromScannerReport.getDefaultImpactsList());
    Map.Entry<SoftwareQuality, Severity> bestImpactForBackMapping = ImpactMapper.getBestImpactForBackmapping(impacts);
    return ImpactMapper.convertToDeprecatedSeverity(bestImpactForBackMapping.getValue());
  }

  private static CleanCodeAttribute mapCleanCodeAttribute(@Nullable String cleanCodeAttribute) {
    if (cleanCodeAttribute == null) {
      return CleanCodeAttribute.defaultCleanCodeAttribute();
    }
    return CleanCodeAttribute.valueOf(cleanCodeAttribute);
  }

  private static Map<SoftwareQuality, Severity> mapImpacts(List<ScannerReport.Impact> impacts) {
    if (!impacts.isEmpty()) {
      return impacts.stream()
        .collect(Collectors.toMap(i -> mapSoftwareQuality(i.getSoftwareQuality()), i -> mapImpactSeverity(i.getSeverity())));
    }
    return Collections.emptyMap();
  }

  private static Severity mapImpactSeverity(String severity) {
    return Severity.valueOf(severity);
  }

  private static SoftwareQuality mapSoftwareQuality(String softwareQuality) {
    return SoftwareQuality.valueOf(softwareQuality);
  }

  public RuleKey getKey() {
    return key;
  }

  public String getEngineId() {
    return engineId;
  }

  public String getRuleId() {
    return ruleId;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  @CheckForNull
  public RuleType getRuleType() {
    return ruleType;
  }

  public boolean hasDetails() {
    return hasDetails;
  }

  @CheckForNull
  public CleanCodeAttribute getCleanCodeAttribute() {
    return cleanCodeAttribute;
  }

  public Map<SoftwareQuality, Severity> getDefaultImpacts() {
    return defaultImpacts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NewAdHocRule that = (NewAdHocRule) o;
    return Objects.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }


}
