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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.server.notification.NotificationManager;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.notification.IssueChangeNotification;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class IssueBulkChangeService {

  private static final Logger LOG = Loggers.get(IssueBulkChangeService.class);

  private final DbClient dbClient;
  private final IssueService issueService;
  private final IssueStorage issueStorage;
  private final DefaultRuleFinder ruleFinder;
  private final NotificationManager notificationService;
  private final List<Action> actions;
  private final UserSession userSession;

  public IssueBulkChangeService(DbClient dbClient, IssueService issueService, IssueStorage issueStorage, DefaultRuleFinder ruleFinder,
    NotificationManager notificationService, List<Action> actions, UserSession userSession) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.issueStorage = issueStorage;
    this.ruleFinder = ruleFinder;
    this.notificationService = notificationService;
    this.actions = actions;
    this.userSession = userSession;
  }

  public IssueBulkChangeResult execute(IssueBulkChangeQuery issueBulkChangeQuery, UserSession userSession) {
    LOG.debug("BulkChangeQuery : {}", issueBulkChangeQuery);
    long start = System.currentTimeMillis();
    userSession.checkLoggedIn();

    IssueBulkChangeResult result = new IssueBulkChangeResult();

    Collection<Issue> issues = getByKeysForUpdate(issueBulkChangeQuery.issues());
    Repository repository = new Repository(issues);

    List<Action> bulkActions = getActionsToApply(issueBulkChangeQuery, issues, userSession);
    IssueChangeContext issueChangeContext = IssueChangeContext.createUser(new Date(), userSession.getLogin());
    Set<String> concernedProjects = new HashSet<>();
    for (Issue issue : issues) {
      ActionContext actionContext = new ActionContext(issue, issueChangeContext);
      for (Action action : bulkActions) {
        applyAction(action, actionContext, issueBulkChangeQuery, result);
      }
      if (result.issuesChanged().contains(issue)) {
        // Apply comment action only on changed issues
        if (issueBulkChangeQuery.hasComment()) {
          applyAction(getAction(CommentAction.KEY), actionContext, issueBulkChangeQuery, result);
        }
        issueStorage.save((DefaultIssue) issue);
        if (issueBulkChangeQuery.sendNotifications()) {
          String projectKey = issue.projectKey();
          if (projectKey != null) {
            Rule rule = repository.rule(issue.ruleKey());
            notificationService.scheduleForSending(new IssueChangeNotification()
              .setIssue((DefaultIssue) issue)
              .setChangeAuthorLogin(issueChangeContext.login())
              .setRuleName(rule != null ? rule.getName() : null)
              .setProject(projectKey, repository.project(projectKey).name())
              .setComponent(repository.component(issue.componentKey())));
          }
        }
        concernedProjects.add(issue.projectKey());
      }
    }
    LOG.debug("BulkChange execution time : {} ms", System.currentTimeMillis() - start);
    return result;
  }

  private Collection<Issue> getByKeysForUpdate(List<String> issueKeys) {
    // Load from index to check permission
    SearchOptions options = new SearchOptions().setLimit(SearchOptions.MAX_LIMIT);
    // TODO restrict fields to issue key, in order to not load all other fields;
    List<IssueDoc> authorizedIssues = issueService.search(IssueQuery.builder(userSession).issueKeys(issueKeys).build(), options).getDocs();
    Collection<String> authorizedKeys = Collections2.transform(authorizedIssues, new Function<IssueDoc, String>() {
      @Override
      public String apply(IssueDoc input) {
        return input.key();
      }
    });

    if (!authorizedKeys.isEmpty()) {
      DbSession session = dbClient.openSession(false);
      try {
        List<IssueDto> dtos = dbClient.issueDao().selectByKeys(session, Lists.newArrayList(authorizedKeys));
        return Collections2.transform(dtos, new Function<IssueDto, Issue>() {
          @Override
          public Issue apply(@Nullable IssueDto input) {
            return input != null ? input.toDefaultIssue() : null;
          }
        });
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
    return Collections.emptyList();
  }

  private List<Action> getActionsToApply(IssueBulkChangeQuery issueBulkChangeQuery, Collection<Issue> issues, UserSession userSession) {
    List<Action> bulkActions = newArrayList();
    for (String actionKey : issueBulkChangeQuery.actions()) {
      Action action = getAction(actionKey);
      if (action.verify(issueBulkChangeQuery.properties(actionKey), issues, userSession)) {
        bulkActions.add(action);
      }
    }
    return bulkActions;
  }

  private static void applyAction(Action action, ActionContext actionContext, IssueBulkChangeQuery issueBulkChangeQuery, IssueBulkChangeResult result) {
    Issue issue = actionContext.issue();
    try {
      if (action.supports(issue) && action.execute(issueBulkChangeQuery.properties(action.key()), actionContext)) {
        result.addIssueChanged(issue);
      } else {
        result.addIssueNotChanged(issue);
      }
    } catch (Exception e) {
      result.addIssueNotChanged(issue);
      LOG.info("An error occur when trying to apply the action : " + action.key() + " on issue : " + issue.key() + ". This issue has been ignored.", e);
    }
  }

  private Action getAction(final String actionKey) {
    Action action = Iterables.find(actions, new Predicate<Action>() {
      @Override
      public boolean apply(Action action) {
        return action.key().equals(actionKey);
      }
    }, null);
    if (action == null) {
      throw new BadRequestException("The action : '" + actionKey + "' is unknown");
    }
    return action;
  }

  static class ActionContext implements Action.Context {
    private final Issue issue;
    private final IssueChangeContext changeContext;

    ActionContext(Issue issue, IssueChangeContext changeContext) {
      this.issue = issue;
      this.changeContext = changeContext;
    }

    @Override
    public Issue issue() {
      return issue;
    }

    @Override
    public IssueChangeContext issueChangeContext() {
      return changeContext;
    }
  }

  private class Repository {

    private final Map<RuleKey, Rule> rules = newHashMap();
    private final Map<String, ComponentDto> components = newHashMap();
    private final Map<String, ComponentDto> projects = newHashMap();

    public Repository(Collection<Issue> issues) {
      Set<RuleKey> ruleKeys = newHashSet();
      Set<String> componentKeys = newHashSet();
      Set<String> projectKeys = newHashSet();

      for (Issue issue : issues) {
        ruleKeys.add(issue.ruleKey());
        componentKeys.add(issue.componentKey());
        String projectKey = issue.projectKey();
        if (projectKey != null) {
          projectKeys.add(projectKey);
        }
      }

      DbSession session = dbClient.openSession(false);
      try {
        for (Rule rule : ruleFinder.findByKeys(ruleKeys)) {
          rules.put(rule.ruleKey(), rule);
        }

        for (ComponentDto file : dbClient.componentDao().selectByKeys(session, componentKeys)) {
          components.put(file.getKey(), file);
        }

        for (ComponentDto project : dbClient.componentDao().selectByKeys(session, projectKeys)) {
          projects.put(project.getKey(), project);
        }
      } finally {
        session.close();
      }
    }

    public Rule rule(RuleKey ruleKey) {
      return rules.get(ruleKey);
    }

    @CheckForNull
    public ComponentDto component(String key) {
      return components.get(key);
    }

    public ComponentDto project(String key) {
      return projects.get(key);
    }
  }
}
