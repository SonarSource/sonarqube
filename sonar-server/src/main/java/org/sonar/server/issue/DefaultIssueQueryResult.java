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

package org.sonar.server.issue;

import com.google.common.collect.Maps;
import org.sonar.api.component.Component;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.user.User;
import org.sonar.api.utils.Paging;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DefaultIssueQueryResult implements IssueQueryResult {

  private List<Issue> issues;
  private final Map<RuleKey, Rule> rulesByKey = Maps.newHashMap();
  private final Map<String, Component> componentsByKey = Maps.newHashMap();
  private final Map<String, Component> projectsByKey = Maps.newHashMap();
  private final Map<String, ActionPlan> actionPlansByKey = Maps.newHashMap();
  private final Map<String, User> usersByLogin = Maps.newHashMap();
  private boolean maxResultsReached;
  private Paging paging;

  public DefaultIssueQueryResult(List<Issue> issues){
    this.issues = issues;
  }

  public DefaultIssueQueryResult addRules(Collection<Rule> rules){
    for (Rule rule : rules) {
      rulesByKey.put(rule.ruleKey(), rule);
    }
    return this;
  }

  public DefaultIssueQueryResult addComponents(Collection<Component> components){
    for (Component component : components) {
      componentsByKey.put(component.key(), component);
    }
    return this;
  }

  public DefaultIssueQueryResult addProjects(Collection<Component> projects){
    for (Component project : projects) {
      projectsByKey.put(project.key(), project);
    }
    return this;
  }

  public DefaultIssueQueryResult addActionPlans(Collection<ActionPlan> actionPlans){
    for (ActionPlan actionPlan : actionPlans) {
      actionPlansByKey.put(actionPlan.key(), actionPlan);
    }
    return this;
  }

  public DefaultIssueQueryResult addUsers(Collection<User> users){
    for (User user : users) {
      usersByLogin.put(user.login(), user);
    }
    return this;
  }

  public DefaultIssueQueryResult setMaxResultsReached(boolean maxResultsReached){
    this.maxResultsReached = maxResultsReached;
    return this;
  }

  public DefaultIssueQueryResult setPaging(Paging paging){
    this.paging = paging;
    return this;
  }

  @Override
  public List<Issue> issues() {
    return issues;
  }

  @Override
  public Rule rule(Issue issue) {
    return rulesByKey.get(issue.ruleKey());
  }

  @Override
  public Collection<Rule> rules() {
    return rulesByKey.values();
  }

  @Override
  public Component component(Issue issue) {
    return componentsByKey.get(issue.componentKey());
  }

  @Override
  public Collection<Component> components() {
    return componentsByKey.values();
  }

  @Override
  public Component project(Issue issue) {
    return projectsByKey.get(issue.projectKey());
  }

  @Override
  public Collection<Component> projects() {
    return projectsByKey.values();
  }

  @Override
  public ActionPlan actionPlan(Issue issue) {
    return actionPlansByKey.get(issue.actionPlanKey());
  }

  @Override
  public Collection<ActionPlan> actionPlans() {
    return actionPlansByKey.values();
  }

  @Override
  public Collection<User> users() {
    return usersByLogin.values();
  }

  @Override
  @CheckForNull
  public User user(String login) {
    return usersByLogin.get(login);
  }

  @Override
  public boolean maxResultsReached() {
    return maxResultsReached;
  }

  @Override
  public Paging paging() {
    return paging;
  }


}
