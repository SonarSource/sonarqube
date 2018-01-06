/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.issue;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.core.util.Uuids;

public class DefaultIssueBuilder implements Issuable.IssueBuilder {

  private String componentKey;
  private String projectKey;
  private RuleKey ruleKey;
  private Integer line;
  private String message;
  private String severity;
  private Double effortToFix;
  private String assignee;
  private RuleType type;
  private Map<String, String> attributes;

  public DefaultIssueBuilder componentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  public DefaultIssueBuilder projectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @Override
  public DefaultIssueBuilder ruleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @Override
  public DefaultIssueBuilder line(@Nullable Integer line) {
    this.line = line;
    return this;
  }

  @Override
  public DefaultIssueBuilder message(@Nullable String s) {
    this.message = s;
    return this;
  }

  @Override
  public NewIssueLocation newLocation() {
    throw unsupported();
  }

  @Override
  public IssueBuilder addLocation(NewIssueLocation secondaryLocation) {
    throw unsupported();
  }

  @Override
  public IssueBuilder addFlow(Iterable<NewIssueLocation> flow) {
    throw unsupported();
  }

  @Override
  public IssueBuilder at(NewIssueLocation location) {
    throw unsupported();
  }

  private static UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException("Not supported for manual issues");
  }

  @Override
  public DefaultIssueBuilder severity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  @Override
  public DefaultIssueBuilder effortToFix(@Nullable Double d) {
    this.effortToFix = d;
    return this;
  }

  /**
   * @deprecated since 5.5, manual issue feature has been dropped.
   */
  @Deprecated
  @Override
  public DefaultIssueBuilder reporter(@Nullable String s) {
    return this;
  }

  public DefaultIssueBuilder assignee(@Nullable String s) {
    this.assignee = s;
    return this;
  }

  public DefaultIssueBuilder type(@Nullable RuleType type) {
    this.type = type;
    return this;
  }

  @Override
  public DefaultIssueBuilder attribute(String key, @Nullable String value) {
    if (attributes == null) {
      attributes = Maps.newLinkedHashMap();
    }
    attributes.put(key, value);
    return this;
  }

  @Override
  public DefaultIssue build() {
    Preconditions.checkNotNull(projectKey, "Project key must be set");
    Preconditions.checkNotNull(componentKey, "Component key must be set");
    Preconditions.checkNotNull(ruleKey, "Rule key must be set");

    DefaultIssue issue = new DefaultIssue();
    String key = Uuids.create();
    issue.setKey(key);
    issue.setType(MoreObjects.firstNonNull(type, RuleType.CODE_SMELL));
    issue.setComponentKey(componentKey);
    issue.setProjectKey(projectKey);
    issue.setRuleKey(ruleKey);
    issue.setMessage(message);
    issue.setSeverity(severity);
    issue.setManualSeverity(false);
    issue.setGap(effortToFix);
    issue.setLine(line);
    issue.setAssignee(assignee);
    issue.setAttributes(attributes);
    issue.setResolution(null);
    issue.setStatus(Issue.STATUS_OPEN);
    issue.setCloseDate(null);
    issue.setNew(true);
    issue.setCopied(false);
    issue.setBeingClosed(false);
    issue.setOnDisabledRule(false);
    return issue;
  }
}
