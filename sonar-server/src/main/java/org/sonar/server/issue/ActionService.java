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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.action.Function;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class ActionService implements ServerComponent {

  private final DefaultIssueFinder finder;
  private final IssueStorage issueStorage;
  private final IssueUpdater updater;
  private final DefaultActions actions;

  public ActionService(DefaultIssueFinder finder, IssueStorage issueStorage, IssueUpdater updater, DefaultActions actions) {
    this.finder = finder;
    this.issueStorage = issueStorage;
    this.updater = updater;
    this.actions = actions;
  }

  public List<Action> listAvailableActions(String issueKey) {
    IssueQueryResult queryResult = loadIssue(issueKey);
    final DefaultIssue issue = (DefaultIssue) queryResult.first();

    return newArrayList(Iterables.filter(actions.getActions(), new Predicate<Action>() {
      @Override
      public boolean apply(Action action) {
        return action.supports(issue);
      }
    }));
  }

  public Issue execute(String issueKey, String actionKey, UserSession userSession, Map<String, String> parameters) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(actionKey), "Missing action");

    IssueQueryResult queryResult = loadIssue(issueKey);
    DefaultIssue issue = (DefaultIssue) queryResult.first();
    if (issue == null) {
      throw new IllegalStateException("Issue is not found : " + issueKey);
    }

    Action action = actions.getAction(actionKey);
    if (action == null) {
      throw new IllegalStateException("Action is not found : " + actionKey);
    }
    if (!action.supports(issue)) {
      throw new IllegalStateException("A condition is not respected.");
    }

    IssueChangeContext changeContext = IssueChangeContext.createUser(new Date(), userSession.login());
    FunctionContext functionContext = new FunctionContext(updater, issue, parameters, changeContext);
    for (Function function : action.functions()) {
      function.execute(functionContext);
    }
    issueStorage.save(issue);
    return issue;
  }

  public IssueQueryResult loadIssue(String issueKey) {
    IssueQuery query = IssueQuery.builder().issueKeys(newArrayList(issueKey)).requiredRole(UserRole.USER).build();
    return finder.find(query);
  }

  static class FunctionContext implements Function.Context {

    private final DefaultIssue issue;
    private final Map<String, String> parameters;
    private final IssueUpdater updater;
    private final IssueChangeContext changeContext;

    FunctionContext(IssueUpdater updater, DefaultIssue issue, Map<String, String> parameters, IssueChangeContext changeContext) {
      this.updater = updater;
      this.issue = issue;
      this.parameters = parameters;
      this.changeContext = changeContext;
    }

    @Override
    public Issue issue() {
      return issue;
    }

    @Override
    public Map<String, String> parameters() {
      return parameters;
    }

    @Override
    public Function.Context setAttribute(String key, @Nullable String value) {
      updater.setAttribute(issue, key, value, changeContext);
      return this;
    }

    @Override
    public Function.Context addComment(String text) {
      updater.addComment(issue, text, changeContext);
      return this;
    }
  }

}
