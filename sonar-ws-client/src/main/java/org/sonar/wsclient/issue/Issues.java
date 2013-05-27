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
package org.sonar.wsclient.issue;

import org.sonar.wsclient.component.Component;
import org.sonar.wsclient.rule.Rule;
import org.sonar.wsclient.user.User;

import javax.annotation.CheckForNull;

import java.util.*;

/**
 * @since 3.6
 */
public class Issues {

  private final List<Issue> list = new ArrayList<Issue>();
  private final Map<String, Rule> rulesByKey = new HashMap<String, Rule>();
  private final Map<String, User> usersByKey = new HashMap<String, User>();
  private final Map<String, Component> componentsByKey = new HashMap<String, Component>();
  private final Map<String, Component> projectsByKey = new HashMap<String, Component>();
  private Paging paging;
  private Boolean maxResultsReached;

  public List<Issue> list() {
    return list;
  }

  public int size() {
    return list.size();
  }

  public Collection<Rule> rules() {
    return rulesByKey.values();
  }

  public Rule rule(Issue issue) {
    return rulesByKey.get(issue.ruleKey());
  }

  public Collection<User> users() {
    return usersByKey.values();
  }

  @CheckForNull
  public User user(String login) {
    return usersByKey.get(login);
  }

  public Collection<Component> components() {
    return componentsByKey.values();
  }

  @CheckForNull
  public Component component(Issue issue) {
    return componentsByKey.get(issue.componentKey());
  }

  public Collection<Component> projects() {
    return projectsByKey.values();
  }

  @CheckForNull
  public Component project(Issue issue) {
    return projectsByKey.get(issue.projectKey());
  }

  public Paging paging() {
    return paging;
  }

  public Boolean maxResultsReached() {
    return maxResultsReached;
  }

  Issues add(Issue issue) {
    list.add(issue);
    return this;
  }

  Issues add(Rule rule) {
    rulesByKey.put(rule.key(), rule);
    return this;
  }

  Issues add(User user) {
    usersByKey.put(user.login(), user);
    return this;
  }

  Issues addComponent(Component c) {
    componentsByKey.put(c.key(), c);
    return this;
  }

  Issues addProject(Component c) {
    projectsByKey.put(c.key(), c);
    return this;
  }

  Issues setPaging(Paging paging) {
    this.paging = paging;
    return this;
  }

  Issues setMaxResultsReached(Boolean maxResultsReached) {
    this.maxResultsReached = maxResultsReached;
    return this;
  }
}
