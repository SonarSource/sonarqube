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
package org.sonar.wsclient.issue.internal;

import org.sonar.wsclient.base.Paging;
import org.sonar.wsclient.component.Component;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.rule.Rule;
import org.sonar.wsclient.user.User;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 3.6
 */
public class DefaultIssues implements Issues {

  private final List<Issue> list = new ArrayList<Issue>();
  private final Map<String, Rule> rulesByKey = new HashMap<String, Rule>();
  private final Map<String, User> usersByKey = new HashMap<String, User>();
  private final Map<Long, Component> componentsById = new HashMap<Long, Component>();
  private final Map<String, Component> componentsByKey = new HashMap<String, Component>();
  private final Map<String, Component> projectsByKey = new HashMap<String, Component>();
  private final Map<String, ActionPlan> actionPlansByKey = new HashMap<String, ActionPlan>();
  private Paging paging;

  @Override
  public List<Issue> list() {
    return list;
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public Collection<Rule> rules() {
    return rulesByKey.values();
  }

  @Override
  public Rule rule(Issue issue) {
    return rulesByKey.get(issue.ruleKey());
  }

  @Override
  public Collection<User> users() {
    return usersByKey.values();
  }

  @Override
  @CheckForNull
  public User user(String login) {
    return usersByKey.get(login);
  }

  @Override
  public Collection<Component> components() {
    return componentsByKey.values();
  }

  @Override
  @CheckForNull
  public Component component(Issue issue) {
    return componentsByKey.get(issue.componentKey());
  }

  @Override
  @CheckForNull
  public Component componentById(long id) {
    return componentsById.get(id);
  }

  @Override
  @CheckForNull
  public Component componentByKey(String key) {
    return componentsByKey.get(key);
  }

  @Override
  public Collection<Component> projects() {
    return projectsByKey.values();
  }

  @Override
  @CheckForNull
  public Component project(Issue issue) {
    return projectsByKey.get(issue.projectKey());
  }

  @Override
  public Collection<ActionPlan> actionPlans() {
    return actionPlansByKey.values();
  }

  @Override
  @CheckForNull
  public ActionPlan actionPlans(Issue issue) {
    return actionPlansByKey.get(issue.actionPlan());
  }

  @Override
  public Paging paging() {
    return paging;
  }

  DefaultIssues add(Issue issue) {
    list.add(issue);
    return this;
  }

  DefaultIssues add(Rule rule) {
    rulesByKey.put(rule.key(), rule);
    return this;
  }

  DefaultIssues add(User user) {
    usersByKey.put(user.login(), user);
    return this;
  }

  DefaultIssues add(ActionPlan actionPlan) {
    actionPlansByKey.put(actionPlan.key(), actionPlan);
    return this;
  }

  DefaultIssues addComponent(Component c) {
    componentsById.put(c.id(), c);
    componentsByKey.put(c.key(), c);
    return this;
  }

  DefaultIssues addProject(Component c) {
    projectsByKey.put(c.key(), c);
    return this;
  }

  DefaultIssues setPaging(Paging paging) {
    this.paging = paging;
    return this;
  }
}
