/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.bootstrapper.IssueListener;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.protocol.input.ScannerInput.User;
import org.sonar.scanner.repository.user.UserRepositoryLoader;

public class DefaultIssueCallback implements IssueCallback {
  private final IssueCache issues;
  private final IssueListener listener;
  private final UserRepositoryLoader userRepository;
  private final Rules rules;

  private Set<String> userLoginNames = new HashSet<>();
  private Map<String, String> userMap = new HashMap<>();
  private Set<RuleKey> ruleKeys = new HashSet<>();

  public DefaultIssueCallback(IssueCache issues, IssueListener listener, UserRepositoryLoader userRepository, Rules rules) {
    this.issues = issues;
    this.listener = listener;
    this.userRepository = userRepository;
    this.rules = rules;
  }

  /**
   * If no listener exists, this constructor will be used by pico.
   */
  public DefaultIssueCallback(IssueCache issues, UserRepositoryLoader userRepository, Rules rules) {
    this(issues, null, userRepository, rules);
  }

  @Override
  public void execute() {
    if (listener == null) {
      return;
    }

    for (TrackedIssue issue : issues.all()) {
      collectInfo(issue);
    }

    getUsers();

    for (TrackedIssue issue : issues.all()) {
      IssueListener.Issue newIssue = new IssueListener.Issue();
      newIssue.setAssigneeLogin(issue.assignee());
      newIssue.setAssigneeName(getAssigneeName(issue.assignee()));
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

  private void collectInfo(TrackedIssue issue) {
    if (!StringUtils.isEmpty(issue.assignee())) {
      userLoginNames.add(issue.assignee());
    }
    if (issue.getRuleKey() != null) {
      ruleKeys.add(issue.getRuleKey());
    }
  }

  private String getAssigneeName(String login) {
    return userMap.get(login);
  }

  private void getUsers() {
    for (String loginName : userLoginNames) {
      User user = userRepository.load(loginName);
      if (user != null) {
        userMap.put(user.getLogin(), user.getName());
      }
    }
  }

  private String getRuleName(RuleKey ruleKey) {
    Rule rule = rules.find(ruleKey);
    return rule != null ? rule.name() : null;
  }
}
