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
package org.sonar.server.issue.ws;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.es.Facets;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.IssueCommentService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.index.IssueIndex;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.server.issue.ws.SearchAdditionalField.ACTIONS;
import static org.sonar.server.issue.ws.SearchAdditionalField.ACTION_PLANS;
import static org.sonar.server.issue.ws.SearchAdditionalField.COMMENTS;
import static org.sonar.server.issue.ws.SearchAdditionalField.RULES;
import static org.sonar.server.issue.ws.SearchAdditionalField.TRANSITIONS;
import static org.sonar.server.issue.ws.SearchAdditionalField.USERS;

/**
 * Loads all the information required for the response of api/issues/search.
 */
public class SearchResponseLoader {

  private final DbClient dbClient;
  private final IssueService issueService;
  private final ActionService actionService;
  private final IssueCommentService commentService;

  public SearchResponseLoader(DbClient dbClient, IssueService issueService, ActionService actionService, IssueCommentService commentService) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.actionService = actionService;
    this.commentService = commentService;
  }

  /**
   * The issue keys are given by the multi-criteria search in Elasticsearch index.
   */
  public SearchResponseData load(Collector collector, @Nullable Facets facets) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      SearchResponseData result = new SearchResponseData(dbClient.issueDao().selectByOrderedKeys(dbSession, collector.getIssueKeys()));
      collector.collect(result.getIssues());

      loadRules(collector, dbSession, result);
      // order is important - loading of comments complete the list of users: loadComments() is
      // before loadUsers()
      loadComments(collector, dbSession, result);
      loadUsers(collector, dbSession, result);
      loadActionPlans(collector, dbSession, result);
      loadComponents(collector, dbSession, result);
      loadActionsAndTransitions(collector, result);
      completeTotalDebtFromFacet(facets, result);
      return result;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void loadUsers(Collector collector, DbSession dbSession, SearchResponseData result) {
    if (collector.contains(USERS)) {
      result.setUsers(dbClient.userDao().selectByLogins(dbSession, collector.<String>get(USERS)));
    }
  }

  private void loadComments(Collector collector, DbSession dbSession, SearchResponseData result) {
    if (collector.contains(COMMENTS)) {
      List<IssueChangeDto> comments = dbClient.issueChangeDao().selectByTypeAndIssueKeys(dbSession, collector.getIssueKeys(), IssueChangeDto.TYPE_COMMENT);
      result.setComments(comments);
      for (IssueChangeDto comment : comments) {
        collector.add(USERS, comment.getUserLogin());
        if (commentService.canEditOrDelete(comment)) {
          result.addUpdatableComment(comment.getKey());
        }
      }
    }
  }

  private void loadActionPlans(Collector collector, DbSession dbSession, SearchResponseData result) {
    if (collector.contains(ACTION_PLANS)) {
      result.setActionPlans(dbClient.actionPlanDao().selectByKeys(dbSession, collector.<String>get(ACTION_PLANS)));
    }
  }

  private void loadRules(Collector collector, DbSession dbSession, SearchResponseData result) {
    if (collector.contains(RULES)) {
      result.setRules(dbClient.ruleDao().selectByKeys(dbSession, collector.<RuleKey>get(RULES)));
    }
  }

  private void loadComponents(Collector collector, DbSession dbSession, SearchResponseData result) {
    // always load components and projects, because some issue fields still relate to component ids/keys.
    // They should be dropped but are kept for backward-compatibility (see SearchResponseFormat)
    result.addComponents(dbClient.componentDao().selectByUuids(dbSession, collector.getComponentUuids()));
    result.addComponents(dbClient.componentDao().selectSubProjectsByComponentUuids(dbSession, collector.getComponentUuids()));
    for (ComponentDto component : result.getComponents()) {
      collector.addProjectUuid(component.projectUuid());
    }
    List<ComponentDto> projects = dbClient.componentDao().selectByUuids(dbSession, collector.getProjectUuids());
    result.addComponents(projects);
  }

  private void loadActionsAndTransitions(Collector collector, SearchResponseData result) {
    if (collector.contains(ACTIONS) || collector.contains(TRANSITIONS)) {
      for (IssueDto dto : result.getIssues()) {
        // so that IssueDto can be used.
        if (collector.contains(ACTIONS)) {
          result.addActions(dto.getKey(), actionService.listAvailableActions(dto.toDefaultIssue()));
        }
        if (collector.contains(TRANSITIONS)) {
          // TODO workflow and action engines must not depend on org.sonar.api.issue.Issue but on a generic interface
          DefaultIssue issue = dto.toDefaultIssue();
          result.addTransitions(issue.key(), issueService.listTransitions(issue));
        }
      }
    }
  }

  private void completeTotalDebtFromFacet(@Nullable Facets facets, SearchResponseData result) {
    if (facets != null) {
      Map<String, Long> debtFacet = facets.get(IssueIndex.DEBT_AGGREGATION_NAME);
      if (debtFacet != null) {
        result.setDebtTotal(debtFacet.get(Facets.TOTAL));
      }
    }
  }

  /**
   * Collects the keys of all the data to be loaded (users, rules, ...)
   */
  public static class Collector {
    private final EnumSet<SearchAdditionalField> fields;
    private final SetMultimap<SearchAdditionalField, Object> fieldValues = MultimapBuilder.enumKeys(SearchAdditionalField.class).hashSetValues().build();
    private final Set<String> componentUuids = new HashSet<>();
    private final Set<String> projectUuids = new HashSet<>();
    private final List<String> issueKeys;

    public Collector(EnumSet<SearchAdditionalField> fields, List<String> issueKeys) {
      this.fields = fields;
      this.issueKeys = issueKeys;
    }

    void collect(List<IssueDto> issues) {
      for (IssueDto issue : issues) {
        componentUuids.add(issue.getComponentUuid());
        projectUuids.add(issue.getProjectUuid());
        add(ACTION_PLANS, issue.getActionPlanKey());
        add(RULES, issue.getRuleKey());
        add(USERS, issue.getReporter());
        add(USERS, issue.getAssignee());
        collectComponentsFromIssueLocations(issue);
      }
    }

    private void collectComponentsFromIssueLocations(IssueDto issue) {
      DbIssues.Locations locations = issue.parseLocations();
      if (locations != null) {
        for (DbIssues.Flow flow : locations.getFlowList()) {
          for (DbIssues.Location location : flow.getLocationList()) {
            if (location.hasComponentId()) {
              componentUuids.add(location.getComponentId());
            }
          }
        }
      }
    }

    public void add(SearchAdditionalField key, @Nullable Object value) {
      if (value != null) {
        fieldValues.put(key, value);
      }
    }

    public void addComponentUuids(@Nullable Collection<String> uuids) {
      if (uuids != null) {
        this.componentUuids.addAll(uuids);
      }
    }

    public void addProjectUuid(String uuid) {
      this.projectUuids.add(uuid);
    }

    public void addProjectUuids(@Nullable Collection<String> uuids) {
      if (uuids != null) {
        this.projectUuids.addAll(uuids);
      }
    }

    public void addAll(SearchAdditionalField key, @Nullable Iterable values) {
      if (values != null) {
        for (Object value : values) {
          add(key, value);
        }
      }
    }

    <T> List<T> get(SearchAdditionalField key) {
      return newArrayList((Set<T>) fieldValues.get(key));
    }

    boolean contains(SearchAdditionalField field) {
      return fields.contains(field);
    }

    public List<String> getIssueKeys() {
      return issueKeys;
    }

    public Set<String> getComponentUuids() {
      return componentUuids;
    }

    public Set<String> getProjectUuids() {
      return projectUuids;
    }
  }
}
