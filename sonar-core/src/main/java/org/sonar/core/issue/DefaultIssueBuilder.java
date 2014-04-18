/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.issue;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.UUID;

public class DefaultIssueBuilder implements Issuable.IssueBuilder {

  private String componentKey;
  private RuleKey ruleKey;
  private Integer line;
  private String message;
  private String severity;
  private Double effortToFix;
  private String reporter;
  private Map<String, String> attributes;

  public DefaultIssueBuilder() {

  }

  public DefaultIssueBuilder componentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  @Override
  public Issuable.IssueBuilder ruleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @Override
  public Issuable.IssueBuilder line(@Nullable Integer line) {
    this.line = line;
    return this;
  }

  @Override
  public Issuable.IssueBuilder message(@Nullable String s) {
    this.message = s;
    return this;
  }

  @Override
  public Issuable.IssueBuilder severity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  @Override
  public Issuable.IssueBuilder effortToFix(@Nullable Double d) {
    this.effortToFix = d;
    return this;
  }

  @Override
  public Issuable.IssueBuilder reporter(@Nullable String s) {
    this.reporter = s;
    return this;
  }

  @Override
  public Issuable.IssueBuilder attribute(String key, @Nullable String value) {
    if (attributes == null) {
      attributes = Maps.newLinkedHashMap();
    }
    attributes.put(key, value);
    return this;
  }

  @Override
  public DefaultIssue build() {
    Preconditions.checkNotNull(componentKey, "Component key must be set");
    Preconditions.checkNotNull(ruleKey, "Rule key must be set");

    DefaultIssue issue = new DefaultIssue();
    String key = UUID.randomUUID().toString();
    Preconditions.checkState(!Strings.isNullOrEmpty(key), "Fail to generate issue key");
    issue.setKey(key);
    issue.setComponentKey(componentKey);
    issue.setRuleKey(ruleKey);
    issue.setMessage(message);
    issue.setSeverity(severity);
    issue.setManualSeverity(false);
    issue.setEffortToFix(effortToFix);
    issue.setLine(line);
    issue.setReporter(reporter);
    issue.setAttributes(attributes);
    issue.setResolution(null);
    issue.setStatus(Issue.STATUS_OPEN);
    issue.setCloseDate(null);
    issue.setNew(true);
    issue.setEndOfLife(false);
    issue.setOnDisabledRule(false);
    return issue;
  }
}
