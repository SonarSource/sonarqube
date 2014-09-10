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
package org.sonar.server.issue.index;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.elasticsearch.action.search.SearchResponse;
import org.sonar.api.component.Component;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.rules.Rule;
import org.sonar.api.user.User;
import org.sonar.api.utils.Paging;
import org.sonar.core.component.ComponentDto;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.Result;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class IssueResult extends Result<IssueDoc> implements IssueQueryResult {

  private final Map<String, Rule> rules;
  private final Map<String, Component> components;
  private final Map<String, Component> projects;
  private final Map<String, ActionPlan> actionPlans;
  private final Map<String, User> usersByLogin;

  Paging paging;

  public IssueResult(SearchResponse response) {
    this(null, response);
  }

  public IssueResult(@Nullable BaseIndex<IssueDoc, ?, ?> index, SearchResponse response) {
    super(index, response);
    rules = new HashMap<String, Rule>();
    components = new HashMap<String, Component>();
    projects = new HashMap<String, Component>();
    actionPlans = new HashMap<String, ActionPlan>();
    usersByLogin = new HashMap<String, User>();
  }

  @Override
  public List<Issue> issues() {
    return ImmutableList.<Issue>builder().addAll(this.getHits()).build();
  }

  @Override
  public Issue first() {
    return Iterables.getFirst(this.getHits(), null);
  }

  @Override
  public Rule rule(Issue issue) {
    return rules.get(issue.key());
  }

  @Override
  public Collection<Rule> rules() {
    return rules.values();
  }

  @Override
  public Component component(Issue issue) {
    return components.get(issue.componentKey());
  }

  @Override
  public Collection<Component> components() {
    return components.values();
  }

  @Override
  public Component project(Issue issue) {
    return projects.get(issue.projectKey());
  }

  @Override
  public Collection<Component> projects() {
    return projects.values();
  }

  @Override
  public ActionPlan actionPlan(Issue issue) {
    return actionPlans.get(issue.key());
  }

  @Override
  public Collection<ActionPlan> actionPlans() {
    return actionPlans.values();
  }

  @Override
  public Collection<User> users() {
    return usersByLogin.values();
  }

  @Override
  public User user(String login) {
    return usersByLogin.get(login);
  }

  @Override
  public Paging paging() {
    return paging;
  }

  public void setPaging(Paging paging) {
    this.paging = paging;
  }

  @Override
  public boolean maxResultsReached() {
    return false;
  }

  public void addProjects(Collection<ComponentDto> projects) {
    for (ComponentDto project : projects) {
      this.projects.put(project.key(), project);
    }
  }

  public void addComponents(Collection<ComponentDto> components) {
    for (ComponentDto component : components) {
      this.components.put(component.key(), component);
    }
  }

  public void addUsers(Collection<User> users) {
    for (User user : users) {
      this.usersByLogin.put(user.login(), user);
    }
  }

  public void addActionPlans(Collection<ActionPlan> plans) {
    for (ActionPlan plan : plans) {
      this.actionPlans.put(plan.key(), plan);
    }
  }

  public void addRules(Rule rule) {
    this.rules.put(rule.getKey(), rule);
  }
}
