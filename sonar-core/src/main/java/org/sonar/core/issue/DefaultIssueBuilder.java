/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import com.google.common.collect.Maps;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;

import java.util.Map;

public class DefaultIssueBuilder implements Issuable.IssueBuilder {

  private final String componentKey;
  private RuleKey ruleKey;
  private Integer line;
  private String description;
  private String severity;
  private Double cost;
  private boolean manual = false;
  private Map<String, String> attributes;

  public DefaultIssueBuilder(String componentKey) {
    this.componentKey = componentKey;
  }

  @Override
  public Issuable.IssueBuilder ruleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @Override
  public Issuable.IssueBuilder line(Integer line) {
    this.line = line;
    return this;
  }

  @Override
  public Issuable.IssueBuilder description(String s) {
    this.description = s;
    return this;
  }

  @Override
  public Issuable.IssueBuilder severity(String severity) {
    this.severity = severity;
    return this;
  }

  @Override
  public Issuable.IssueBuilder cost(Double cost) {
    this.cost = cost;
    return this;
  }

  @Override
  public Issuable.IssueBuilder manual(boolean b) {
    this.manual = b;
    return this;
  }

  @Override
  public Issuable.IssueBuilder attribute(String key, String value) {
    if (attributes==null) {
      attributes = Maps.newLinkedHashMap();
    }
    attributes.put(key, value);
    return this;
  }

  @Override
  public Issue build() {
    Preconditions.checkNotNull(componentKey, "Component key must be set");
    Preconditions.checkNotNull(ruleKey, "Rule key must be set");

    DefaultIssue issue = new DefaultIssue();
    issue.setComponentKey(componentKey);
    issue.setRuleKey(ruleKey);
    issue.setDescription(description);
    issue.setSeverity(severity);
    issue.setCost(cost);
    issue.setLine(line);
    issue.setManual(manual);
    issue.setAttributes(attributes);
    issue.setNew(true);
    issue.setResolution(Issue.RESOLUTION_OPEN);
    issue.setStatus(Issue.STATUS_OPEN);
    return issue;
  }
}
