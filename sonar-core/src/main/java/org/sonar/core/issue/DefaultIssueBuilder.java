/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.issue;

import com.google.common.base.Preconditions;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;

public class DefaultIssueBuilder implements Issuable.IssueBuilder {

  private final OnIssueCreation callback;
  private final String componentKey;
  private RuleKey ruleKey;
  private Integer line;
  private String message;
  private String title;
  private String severity;
  private Double cost;
  private boolean manual = false;

  public DefaultIssueBuilder(OnIssueCreation callback, String componentKey) {
    this.callback = callback;
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
  public Issuable.IssueBuilder message(String message) {
    this.message = message;
    return this;
  }

  @Override
  public Issuable.IssueBuilder title(String title) {
    this.title = title;
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
  public Issue done() {
    Preconditions.checkNotNull(componentKey, "Component key must be set");
    Preconditions.checkNotNull(ruleKey, "Rule key must be set");

    DefaultIssue issue = new DefaultIssue();
    issue.setComponentKey(componentKey);
    issue.setRuleKey(ruleKey);
    issue.setMessage(message);
    issue.setTitle(title);
    issue.setSeverity(severity);

    issue.setCost(cost);
    issue.setLine(line);
    issue.setManual(manual);

    callback.onIssueCreation(issue);
    return issue;
  }
}
