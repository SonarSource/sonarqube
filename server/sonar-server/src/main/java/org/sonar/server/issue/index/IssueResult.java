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
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.Result;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class IssueResult extends Result<IssueDoc> implements IssueQueryResult {

  Map<String, Rule> rules;
  Map<String, Component> components;
  Map<String, Component> projects;
  Map<String, ActionPlan> actionPlans;
  Map<String, User> usersByLogin;

  Paging paging;

  public IssueResult(SearchResponse response) {
    super(response);
  }

  public IssueResult(@Nullable BaseIndex<IssueDoc, ?, ?> index, SearchResponse response) {
    super(index, response);
  }

  @Override
  public List<Issue> issues() {
    return ImmutableList.<Issue>copyOf(this.getHits());
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
    return components.get(issue.key());
  }

  @Override
  public Collection<Component> components() {
    return components.values();
  }

  @Override
  public Component project(Issue issue) {
    return projects.get(issue.key());
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

  @Override
  public boolean maxResultsReached() {
    return false;
  }
}
