/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
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

  public NewAdHocRule(ScannerReport.AdHocRule ruleFromScannerReport) {
    Preconditions.checkArgument(isNotBlank(ruleFromScannerReport.getEngineId()), "'engine id' not expected to be null for an ad hoc rule");
    Preconditions.checkArgument(isNotBlank(ruleFromScannerReport.getRuleId()), "'rule id' not expected to be null for an ad hoc rule");
    Preconditions.checkArgument(isNotBlank(ruleFromScannerReport.getName()), "'name' not expected to be null for an ad hoc rule");
    Preconditions.checkArgument(ruleFromScannerReport.getSeverity() != Constants.Severity.UNSET_SEVERITY , "'severity' not expected to be null for an ad hoc rule");
    Preconditions.checkArgument(ruleFromScannerReport.getType() != ScannerReport.IssueType.UNSET, "'issue type' not expected to be null for an ad hoc rule");
    this.key = RuleKey.of(RuleKey.EXTERNAL_RULE_REPO_PREFIX + ruleFromScannerReport.getEngineId(), ruleFromScannerReport.getRuleId());
    this.engineId = ruleFromScannerReport.getEngineId();
    this.ruleId = ruleFromScannerReport.getRuleId();
    this.name = ruleFromScannerReport.getName();
    this.description = trimToNull(ruleFromScannerReport.getDescription());
    this.severity = ruleFromScannerReport.getSeverity().name();
    this.ruleType = RuleType.valueOf(ruleFromScannerReport.getType().name());
    this.hasDetails = true;
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
