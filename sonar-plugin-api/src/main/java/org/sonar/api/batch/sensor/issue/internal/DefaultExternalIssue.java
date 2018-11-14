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
package org.sonar.api.batch.sensor.issue.internal;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class DefaultExternalIssue extends AbstractDefaultIssue<DefaultExternalIssue> implements ExternalIssue, NewExternalIssue {
  private Long effort;
  private Severity severity;
  private RuleType type;
  private String engineId;
  private String ruleId;

  public DefaultExternalIssue(DefaultInputProject project) {
    this(project, null);
  }

  public DefaultExternalIssue(DefaultInputProject project, @Nullable SensorStorage storage) {
    super(project, storage);
  }

  @Override
  public DefaultExternalIssue remediationEffortMinutes(@Nullable Long effort) {
    Preconditions.checkArgument(effort == null || effort >= 0, format("effort must be greater than or equal 0 (got %s)", effort));
    this.effort = effort;
    return this;
  }

  @Override
  public DefaultExternalIssue severity(Severity severity) {
    this.severity = severity;
    return this;
  }

  @Override
  public String engineId() {
    return engineId;
  }

  @Override
  public String ruleId() {
    return ruleId;
  }

  @Override
  public Severity severity() {
    return this.severity;
  }

  @Override
  public Long remediationEffort() {
    return this.effort;
  }

  @Override
  public void doSave() {
    requireNonNull(this.engineId, "Engine id is mandatory on external issue");
    requireNonNull(this.ruleId, "Rule id is mandatory on external issue");
    checkState(primaryLocation != null, "Primary location is mandatory on every external issue");
    checkState(primaryLocation.inputComponent().isFile(), "External issues must be located in files");
    checkState(primaryLocation.message() != null, "External issues must have a message");
    checkState(severity != null, "Severity is mandatory on every external issue");
    checkState(type != null, "Type is mandatory on every external issue");
    storage.store(this);
  }

  @Override
  public RuleType type() {
    return type;
  }

  @Override
  public NewExternalIssue engineId(String engineId) {
    this.engineId = engineId;
    return this;
  }

  @Override
  public NewExternalIssue ruleId(String ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  @Override
  public DefaultExternalIssue forRule(RuleKey ruleKey) {
    this.engineId = ruleKey.repository();
    this.ruleId = ruleKey.rule();
    return this;
  }

  @Override
  public RuleKey ruleKey() {
    if (engineId != null && ruleId != null) {
      return RuleKey.of(RuleKey.EXTERNAL_RULE_REPO_PREFIX + engineId, ruleId);
    }
    return null;
  }

  @Override
  public DefaultExternalIssue type(RuleType type) {
    this.type = type;
    return this;
  }

}
