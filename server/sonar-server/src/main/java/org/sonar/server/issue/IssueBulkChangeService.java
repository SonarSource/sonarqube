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
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.search.QueryContext;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

public class IssueBulkChangeService {

  private static final Logger LOG = LoggerFactory.getLogger(IssueBulkChangeService.class);

  private final DbClient dbClient;
  private final IssueService issueService;
  private final IssueStorage issueStorage;
  private final DefaultRuleFinder ruleFinder;
  private final IssueNotifications issueNotifications;
  private final PreviewCache dryRunCache;
  private final List<Action> actions;

  public IssueBulkChangeService(DbClient dbClient, IssueService issueService, IssueStorage issueStorage, DefaultRuleFinder ruleFinder,
    IssueNotifications issueNotifications, List<Action> actions, PreviewCache dryRunCache) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.issueStorage = issueStorage;
    this.ruleFinder = ruleFinder;
    this.issueNotifications = issueNotifications;
    this.actions = actions;
    this.dryRunCache = dryRunCache;
  }

  public IssueBulkChangeResult execute(IssueBulkChangeQuery issueBulkChangeQuery, UserSession userSession) {
    LOG.debug("BulkChangeQuery : {}", issueBulkChangeQuery);
    long start = System.currentTimeMillis();
    userSession.checkLoggedIn();

    IssueBulkChangeResult result = new IssueBulkChangeResult();

    List<Issue> issues = getByKeysForUpdate(issueBulkChangeQuery.issues());
    Repository repository = new Repository(issues);

    List<Action> bulkActions = getActionsToApply(issueBulkChangeQuery, issues, userSession);
    IssueChangeContext issueChangeContext = IssueChangeContext.createUser(new Date(), userSession.login());
    Set<String> concernedProjects = new HashSet<String>();
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
            issueNotifications.sendChanges((DefaultIssue) issue, issueChangeContext,
              repository.rule(issue.ruleKey()),
              repository.project(projectKey),
              repository.component(issue.componentKey()));
          }
        }
        concernedProjects.add(issue.projectKey());
      }
    }
    // Purge dryRun cache
    for (String projectKey : concernedProjects) {
      dryRunCache.reportResourceModification(projectKey);
    }
    LOG.debug("BulkChange execution time : {} ms", System.currentTimeMillis() - start);
    return result;
  }

  private List<Issue> getByKeysForUpdate(List<String> issueKeys) {
    // Load from index to check permission
    List<Issue> authorizedIndexIssues = issueService.search(IssueQuery.builder().issueKeys(issueKeys).build(), new QueryContext().setMaxLimit()).getHits();
    List<String> authorizedIssueKeys = newArrayList(Iterables.transform(authorizedIndexIssues, new Function<Issue, String>() {
      @Override
      public String apply(@Nullable Issue input) {
        return input != null ? input.key() : null;
      }
    }));

    DbSession session = dbClient.openSession(false);
    try {
      List<IssueDto> issueDtos = dbClient.issueDao().selectByKeys(session, authorizedIssueKeys);
      return newArrayList(Iterables.transform(issueDtos, new Function<IssueDto, Issue>() {
        @Override
        public Issue apply(@Nullable IssueDto input) {
          return input != null ? input.toDefaultIssue() : null;
        }
      }));
    } finally {
      session.close();
    }
  }

  private List<Action> getActionsToApply(IssueBulkChangeQuery issueBulkChangeQuery, List<Issue> issues, UserSession userSession) {
    List<Action> bulkActions = newArrayList();
    for (String actionKey : issueBulkChangeQuery.actions()) {
      Action action = getAction(actionKey);
      if (action.verify(issueBulkChangeQuery.properties(actionKey), issues, userSession)) {
        bulkActions.add(action);
      }
    }
    return bulkActions;
  }

  private void applyAction(Action action, ActionContext actionContext, IssueBulkChangeQuery issueBulkChangeQuery, IssueBulkChangeResult result) {
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

    public Repository(List<Issue> issues) {
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

        for (ComponentDto file : dbClient.componentDao().getByKeys(session, componentKeys)) {
          components.put(file.getKey(), file);
        }

        for (ComponentDto project : dbClient.componentDao().getByKeys(session, projectKeys)) {
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
