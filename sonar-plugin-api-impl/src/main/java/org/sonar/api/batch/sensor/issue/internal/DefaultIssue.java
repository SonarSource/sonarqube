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

import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.rule.RuleKey;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkState;

public class DefaultIssue extends AbstractDefaultIssue<DefaultIssue> implements Issue, NewIssue {
  private RuleKey ruleKey;
  private Double gap;
  private Severity overriddenSeverity;

  public DefaultIssue(DefaultInputProject project) {
    this(project, null);
  }

  public DefaultIssue(DefaultInputProject project, @Nullable SensorStorage storage) {
    super(project, storage);
  }

  public DefaultIssue forRule(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public RuleKey ruleKey() {
    return this.ruleKey;
  }

  @Override
  public DefaultIssue gap(@Nullable Double gap) {
    checkArgument(gap == null || gap >= 0, format("Gap must be greater than or equal 0 (got %s)", gap));
    this.gap = gap;
    return this;
  }

  @Override
  public DefaultIssue overrideSeverity(@Nullable Severity severity) {
    this.overriddenSeverity = severity;
    return this;
  }

  @Override
  public Severity overriddenSeverity() {
    return this.overriddenSeverity;
  }

  @Override
  public Double gap() {
    return this.gap;
  }

  @Override
  public IssueLocation primaryLocation() {
    return primaryLocation;
  }

  @Override
  public void doSave() {
    requireNonNull(this.ruleKey, "ruleKey is mandatory on issue");
    checkState(primaryLocation != null, "Primary location is mandatory on every issue");
    storage.store(this);
  }

}
