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
package org.sonar.batch.issue;

import org.apache.commons.lang.StringUtils;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.batch.protocol.input.BatchInput.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sonar.batch.repository.user.UserRepositoryLoader;
import org.sonar.batch.bootstrapper.IssueListener;
import org.sonar.core.issue.DefaultIssue;

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

    for (DefaultIssue issue : issues.all()) {
      collectInfo(issue);
    }

    getUsers();

    for (DefaultIssue issue : issues.all()) {
      IssueListener.Issue newIssue = new IssueListener.Issue();
      newIssue.setAssigneeLogin(issue.assignee());
      newIssue.setAssigneeName(getAssigneeName(issue.assignee()));
      newIssue.setComponentKey(issue.componentKey());
      newIssue.setKey(issue.key());
      newIssue.setLine(issue.getLine());
      newIssue.setMessage(issue.getMessage());
      newIssue.setNew(issue.isNew());
      newIssue.setResolution(issue.resolution());
      newIssue.setRuleKey(issue.getRuleKey().rule());
      newIssue.setRuleName(getRuleName(issue.getRuleKey()));
      newIssue.setSeverity(issue.severity());
      newIssue.setStatus(issue.status());

      listener.handle(newIssue);
    }
  }

  private void collectInfo(DefaultIssue issue) {
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
