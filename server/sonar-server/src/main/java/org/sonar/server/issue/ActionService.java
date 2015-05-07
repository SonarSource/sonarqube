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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerSide;
import org.sonar.api.component.Component;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.issue.action.Function;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
@ServerSide
public class ActionService {

  private final DbClient dbClient;
  private final IssueService issueService;
  private final IssueStorage issueStorage;
  private final IssueUpdater updater;
  private final Settings settings;
  private final PropertiesDao propertiesDao;
  private final Actions actions;

  public ActionService(DbClient dbClient, IssueService issueService, IssueStorage issueStorage, IssueUpdater updater, Settings settings, PropertiesDao propertiesDao,
    Actions actions) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.issueStorage = issueStorage;
    this.updater = updater;
    this.settings = settings;
    this.propertiesDao = propertiesDao;
    this.actions = actions;
  }

  public List<Action> listAllActions() {
    return actions.list();
  }

  public List<Action> listAvailableActions(String issueKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return listAvailableActions(issueService.getByKeyForUpdate(session, issueKey).toDefaultIssue());
    } finally {
      session.close();
    }
  }

  public List<Action> listAvailableActions(final Issue issue) {
    return newArrayList(Iterables.filter(actions.list(), new Predicate<Action>() {
      @Override
      public boolean apply(Action action) {
        return action.supports(issue);
      }
    }));
  }

  public Issue execute(String issueKey, String actionKey, UserSession userSession) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(actionKey), "Missing action");

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = issueService.getByKeyForUpdate(session, issueKey).toDefaultIssue();
      Action action = getAction(actionKey);
      if (action == null) {
        throw new IllegalArgumentException("Action is not found : " + actionKey);
      }
      if (!action.supports(issue)) {
        throw new IllegalStateException("A condition is not respected");
      }

      IssueChangeContext changeContext = IssueChangeContext.createUser(new Date(), userSession.login());
      Component project = dbClient.componentDao().getByKey(session, issue.projectKey());
      FunctionContext functionContext = new FunctionContext(issue, updater, changeContext, getProjectSettings(project));
      for (Function function : action.functions()) {
        function.execute(functionContext);
      }
      issueStorage.save(issue);
      return issue;
    } finally {
      session.close();
    }
  }

  @CheckForNull
  private Action getAction(final String actionKey) {
    return Iterables.find(actions.list(), new Predicate<Action>() {
      @Override
      public boolean apply(Action action) {
        return action.key().equals(actionKey);
      }
    }, null);
  }

  // TODO org.sonar.server.properties.ProjectSettings should be used instead
  public Settings getProjectSettings(Component project) {
    Settings projectSettings = new Settings(settings);
    List<PropertyDto> properties = propertiesDao.selectProjectProperties(project.key());
    for (PropertyDto dto : properties) {
      projectSettings.setProperty(dto.getKey(), dto.getValue());
    }
    return projectSettings;
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
    public Function.Context addComment(String text) {
      updater.addComment(issue, text, changeContext);
      return this;
    }
  }

}
