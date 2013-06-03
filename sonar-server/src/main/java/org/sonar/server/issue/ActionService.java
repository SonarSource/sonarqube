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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ActionService implements ServerComponent {

  private final DefaultIssueFinder finder;
  private final IssueStorage issueStorage;
  private final IssueUpdater updater;
  private final List<Action> actions;

  public ActionService(DefaultIssueFinder finder, IssueStorage issueStorage, IssueUpdater updater, List<Action> actions) {
    this.finder = finder;
    this.issueStorage = issueStorage;
    this.updater = updater;
    this.actions = actions;
  }

  public ActionService(DefaultIssueFinder finder, IssueStorage issueStorage, IssueUpdater updater) {
    this(finder, issueStorage, updater, Collections.<Action>emptyList());
  }

  public List<Action> listAvailableActions(final Issue issue) {
    return newArrayList(Iterables.filter(actions, new Predicate<Action>() {
      @Override
      public boolean apply(Action action) {
        return action.supports(issue);
      }
    }));
  }

  public List<Action> listAvailableActions(String issueKey) {
    IssueQueryResult queryResult = loadIssue(issueKey);
    final DefaultIssue issue = (DefaultIssue) queryResult.first();
    if (issue == null) {
      throw new IllegalArgumentException("Issue is not found : " + issueKey);
    }

    return listAvailableActions(issue);
  }

  public Issue execute(String issueKey, String actionKey, UserSession userSession) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(actionKey), "Missing action");

    IssueQueryResult queryResult = loadIssue(issueKey);
    DefaultIssue issue = (DefaultIssue) queryResult.first();
    if (issue == null) {
      throw new IllegalArgumentException("Issue is not found : " + issueKey);
    }

    Action action = getAction(actionKey);
    if (action == null) {
      throw new IllegalArgumentException("Action is not found : " + actionKey);
    }
    if (!action.supports(issue)) {
      throw new IllegalStateException("A condition is not respected");
    }

    IssueChangeContext changeContext = IssueChangeContext.createUser(new Date(), userSession.login());
    FunctionContext functionContext = new FunctionContext(updater, issue, changeContext);
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

  @CheckForNull
  private Action getAction(final String actionKey) {
    return Iterables.find(actions, new Predicate<Action>() {
      @Override
      public boolean apply(Action action) {
        return action.key().equals(actionKey);
      }
    }, null);
  }

  static class FunctionContext implements Function.Context {

    private final DefaultIssue issue;
    private final IssueUpdater updater;
    private final IssueChangeContext changeContext;

    FunctionContext(IssueUpdater updater, DefaultIssue issue, IssueChangeContext changeContext) {
      this.updater = updater;
      this.issue = issue;
      this.changeContext = changeContext;
    }

    @Override
    public Issue issue() {
      return issue;
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
