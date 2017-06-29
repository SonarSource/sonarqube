/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.issue;

import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.bootstrapper.IssueListener;
import org.sonar.scanner.issue.tracking.TrackedIssue;

public class DefaultIssueCallback implements IssueCallback {
  private final IssueCache issues;
  private final IssueListener listener;
  private final Rules rules;

  public DefaultIssueCallback(IssueCache issues, IssueListener listener, Rules rules) {
    this.issues = issues;
    this.listener = listener;
    this.rules = rules;
  }

  /**
   * If no listener exists, this constructor will be used by pico.
   */
  public DefaultIssueCallback(IssueCache issues, Rules rules) {
    this(issues, null, rules);
  }

  @Override
  public void execute() {
    if (listener == null) {
      return;
    }

    for (TrackedIssue issue : issues.all()) {
      IssueListener.Issue newIssue = new IssueListener.Issue();
      newIssue.setAssigneeLogin(issue.assignee());
      newIssue.setAssigneeName(issue.assignee());
      newIssue.setComponentKey(issue.componentKey());
      newIssue.setKey(issue.key());
      newIssue.setMessage(issue.getMessage());
      newIssue.setNew(issue.isNew());
      newIssue.setResolution(issue.resolution());
      newIssue.setRuleKey(issue.getRuleKey().toString());
      newIssue.setRuleName(getRuleName(issue.getRuleKey()));
      newIssue.setSeverity(issue.severity());
      newIssue.setStatus(issue.status());
      newIssue.setStartLine(issue.startLine());
      newIssue.setStartLineOffset(issue.startLineOffset());
      newIssue.setEndLine(issue.endLine());
      newIssue.setEndLineOffset(issue.endLineOffset());

      listener.handle(newIssue);
    }
  }

  private String getRuleName(RuleKey ruleKey) {
    Rule rule = rules.find(ruleKey);
    return rule != null ? rule.name() : null;
  }
}
