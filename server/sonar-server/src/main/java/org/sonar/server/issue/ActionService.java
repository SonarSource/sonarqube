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

package org.sonar.server.issue;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.issue.action.Function;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.properties.ProjectSettingsFactory;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;

/**
 * @since 3.6
 */
@ServerSide
public class ActionService {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final Actions actions;
  private final IssueService issueService;
  private final IssueUpdater updater;
  private final ProjectSettingsFactory projectSettingsFactory;
  private final IssueStorage issueStorage;

  public ActionService(DbClient dbClient, UserSession userSession, ProjectSettingsFactory projectSettingsFactory, Actions actions, IssueService issueService, IssueUpdater updater,
    IssueStorage issueStorage) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.actions = actions;
    this.issueService = issueService;
    this.updater = updater;
    this.projectSettingsFactory = projectSettingsFactory;
    this.issueStorage = issueStorage;
  }

  public List<String> listAvailableActions(String issueKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return listAvailableActions(issueService.getByKeyForUpdate(session, issueKey).toDefaultIssue());
    } finally {
      dbClient.closeSession(session);
    }
  }

  public List<String> listAvailableActions(Issue issue) {
    List<String> availableActions = newArrayList();
    String login = userSession.getLogin();
    if (login != null) {
      availableActions.add("comment");
      if (issue.resolution() == null) {
        availableActions.add("assign");
        availableActions.add("set_tags");
        if (!login.equals(issue.assignee())) {
          availableActions.add("assign_to_me");
        }
        availableActions.add("plan");
        String projectUuid = issue.projectUuid();
        if (projectUuid != null && userSession.hasProjectPermissionByUuid(ISSUE_ADMIN, projectUuid)) {
          availableActions.add("set_severity");
        }
      }
      for (String action : loadPluginActions(issue)) {
        availableActions.add(action);
      }
    }
    return availableActions;
  }

  private List<String> loadPluginActions(final Issue issue) {
    return from(actions.list())
      .filter(new SupportIssue(issue))
      .transform(ActionToKey.INSTANCE)
      .toList();
  }

  public Issue execute(String issueKey, String actionKey) {
    checkArgument(!Strings.isNullOrEmpty(actionKey), "Missing action");

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = issueService.getByKeyForUpdate(session, issueKey).toDefaultIssue();
      Action action = getAction(actionKey, issue);

      IssueChangeContext changeContext = IssueChangeContext.createUser(new Date(), userSession.getLogin());
      FunctionContext functionContext = new FunctionContext(issue, updater, changeContext, projectSettingsFactory.newProjectSettings(issue.projectKey()));
      for (Function function : action.functions()) {
        function.execute(functionContext);
      }
      issueStorage.save(issue);
      return issue;
    } finally {
      dbClient.closeSession(session);
    }
  }

  private Action getAction(String actionKey, Issue issue) {
    Action action = find(actions.list(), new MatchActionKey(actionKey), null);
    checkArgument(action != null, String.format("Action is not found : %s", actionKey));
    checkState(action.supports(issue), "A condition is not respected");
    return action;
  }

  static class FunctionContext implements Function.Context {

    private final DefaultIssue issue;
    private final IssueUpdater updater;
    private final IssueChangeContext changeContext;
    private final Settings projectSettings;

    FunctionContext(DefaultIssue issue, IssueUpdater updater, IssueChangeContext changeContext, Settings projectSettings) {
      this.updater = updater;
      this.issue = issue;
      this.changeContext = changeContext;
      this.projectSettings = projectSettings;
    }

    @Override
    public Issue issue() {
      return issue;
    }

    @Override
    public Settings projectSettings() {
      return projectSettings;
    }

    @Override
    public Function.Context setAttribute(String key, @Nullable String value) {
      updater.setAttribute(issue, key, value, changeContext);
      return this;
    }

    @Override
    public Function.Context addComment(@Nullable String text) {
      if (text != null) {
        updater.addComment(issue, text, changeContext);
      }
      return this;
    }
  }

  private static class SupportIssue implements Predicate<Action> {
    private final Issue issue;

    public SupportIssue(Issue issue) {
      this.issue = issue;
    }

    @Override
    public boolean apply(@Nonnull Action action) {
      return action.supports(issue);
    }
  }

  private enum ActionToKey implements com.google.common.base.Function<Action, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull Action action) {
      return action.key();
    }
  }

  private static class MatchActionKey implements Predicate<Action> {
    private final String actionKey;

    private MatchActionKey(String actionKey) {
      this.actionKey = actionKey;
    }

    @Override
    public boolean apply(@Nonnull Action action) {
      return action.key().equals(actionKey);
    }
  }
}
