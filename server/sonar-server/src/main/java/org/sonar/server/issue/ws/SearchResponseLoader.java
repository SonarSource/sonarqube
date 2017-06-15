/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.ws;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.Facets;
import org.sonar.server.issue.ActionFinder;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.issue.IssuesWsParameters;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.difference;
import static java.util.Collections.emptyList;
import static java.util.stream.Stream.concat;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.server.issue.ws.SearchAdditionalField.ACTIONS;
import static org.sonar.server.issue.ws.SearchAdditionalField.COMMENTS;
import static org.sonar.server.issue.ws.SearchAdditionalField.RULES;
import static org.sonar.server.issue.ws.SearchAdditionalField.TRANSITIONS;
import static org.sonar.server.issue.ws.SearchAdditionalField.USERS;

/**
 * Loads all the information required for the response of api/issues/search.
 */
public class SearchResponseLoader {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ActionFinder actionService;
  private final TransitionService transitionService;

  public SearchResponseLoader(UserSession userSession, DbClient dbClient, ActionFinder actionService, TransitionService transitionService) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.actionService = actionService;
    this.transitionService = transitionService;
  }

  /**
   * The issue keys are given by the multi-criteria search in Elasticsearch index.
   */
  public SearchResponseData load(Collector collector, @Nullable Facets facets) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchResponseData result = new SearchResponseData(dbClient.issueDao().selectByOrderedKeys(dbSession, collector.getIssueKeys()));
      collector.collect(result.getIssues());

      loadRules(collector, dbSession, result);
      // order is important - loading of comments complete the list of users: loadComments() is
      // before loadUsers()
      loadComments(collector, dbSession, result);
      loadUsers(collector, dbSession, result);
      loadComponents(collector, dbSession, result);
      loadOrganizations(dbSession, result);
      loadActionsAndTransitions(collector, result);
      completeTotalEffortFromFacet(facets, result);
      return result;
    }
  }

  /**
   * The issue keys are given by the multi-criteria search in Elasticsearch index.
   * <p>
   * Same as {@link #load(Collector, Facets)} but will only retrieve from DB data which is not already provided by the
   * specified preloaded {@link SearchResponseData}.<br/>
   * The returned {@link SearchResponseData} is <strong>not</strong> the one specified as argument.
   * </p>
   */
  public SearchResponseData load(SearchResponseData preloadedResponseData, Collector collector, @Nullable Facets facets) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchResponseData result = new SearchResponseData(loadIssues(preloadedResponseData, collector, dbSession));
      collector.collect(result.getIssues());

      loadRules(preloadedResponseData, collector, dbSession, result);
      // order is important - loading of comments complete the list of users: loadComments() is
      // before loadUsers()
      loadComments(collector, dbSession, result);
      loadUsers(preloadedResponseData, collector, dbSession, result);
      loadComponents(preloadedResponseData, collector, dbSession, result);
      loadOrganizations(dbSession, result);
      loadActionsAndTransitions(collector, result);
      completeTotalEffortFromFacet(facets, result);
      return result;
    }
  }

  private List<IssueDto> loadIssues(SearchResponseData preloadedResponseData, Collector collector, DbSession dbSession) {
    List<IssueDto> preloadedIssues = preloadedResponseData.getIssues();
    Set<String> preloadedIssueKeys = preloadedIssues.stream().map(IssueDto::getKey).collect(MoreCollectors.toSet(preloadedIssues.size()));
    Set<String> issueKeysToLoad = copyOf(difference(ImmutableSet.copyOf(collector.getIssueKeys()), preloadedIssueKeys));
    if (issueKeysToLoad.isEmpty()) {
      return preloadedIssues;
    }

    List<IssueDto> loadedIssues = dbClient.issueDao().selectByKeys(dbSession, issueKeysToLoad);
    return concat(preloadedIssues.stream(), loadedIssues.stream())
      .collect(toList(preloadedIssues.size() + loadedIssues.size()));
  }

  private void loadUsers(SearchResponseData preloadedResponseData, Collector collector, DbSession dbSession, SearchResponseData result) {
    if (collector.contains(USERS)) {
      List<UserDto> preloadedUsers = firstNonNull(preloadedResponseData.getUsers(), emptyList());
      Set<String> preloadedLogins = preloadedUsers.stream().map(UserDto::getLogin).collect(MoreCollectors.toSet(preloadedUsers.size()));
      Set<String> requestedLogins = collector.get(USERS);
      Set<String> loginsToLoad = copyOf(difference(requestedLogins, preloadedLogins));

      if (loginsToLoad.isEmpty()) {
        result.setUsers(preloadedUsers);
      } else {
        List<UserDto> loadedUsers = dbClient.userDao().selectByLogins(dbSession, loginsToLoad);
        result.setUsers(concat(preloadedUsers.stream(), loadedUsers.stream()).collect(toList(preloadedUsers.size() + loadedUsers.size())));
      }
    }
  }

  private void loadComponents(SearchResponseData preloadedResponseData, Collector collector, DbSession dbSession, SearchResponseData result) {
    Collection<ComponentDto> preloadedComponents = preloadedResponseData.getComponents();
    Set<String> preloadedComponentUuids = preloadedComponents.stream().map(ComponentDto::uuid).collect(MoreCollectors.toSet(preloadedComponents.size()));
    Set<String> componentUuidsToLoad = copyOf(difference(collector.getComponentUuids(), preloadedComponentUuids));

    result.addComponents(preloadedComponents);
    if (!componentUuidsToLoad.isEmpty()) {
      result.addComponents(dbClient.componentDao().selectByUuids(dbSession, componentUuidsToLoad));
    }

    // always load components and projects, because some issue fields still relate to component ids/keys.
    // They should be dropped but are kept for backward-compatibility (see SearchResponseFormat)
    result.addComponents(dbClient.componentDao().selectSubProjectsByComponentUuids(dbSession, collector.getComponentUuids()));
    addProjectUuids(collector, dbSession, result);
  }

  private void addProjectUuids(Collector collector, DbSession dbSession, SearchResponseData result) {
    Collection<ComponentDto> loadedComponents = result.getComponents();
    for (ComponentDto component : loadedComponents) {
      collector.addProjectUuid(component.projectUuid());
    }
    Set<String> loadedProjectUuids = loadedComponents.stream().filter(cpt -> cpt.uuid().equals(cpt.projectUuid())).map(ComponentDto::uuid).collect(MoreCollectors.toSet());
    Set<String> projectUuidsToLoad = copyOf(difference(collector.getProjectUuids(), loadedProjectUuids));
    if (!projectUuidsToLoad.isEmpty()) {
      List<ComponentDto> projects = dbClient.componentDao().selectByUuids(dbSession, collector.getProjectUuids());
      result.addComponents(projects);
    }
  }

  private void loadRules(SearchResponseData preloadedResponseData, Collector collector, DbSession dbSession, SearchResponseData result) {
    if (collector.contains(RULES)) {
      List<RuleDefinitionDto> preloadedRules = firstNonNull(preloadedResponseData.getRules(), emptyList());
      Set<RuleKey> preloaedRuleKeys = preloadedRules.stream().map(RuleDefinitionDto::getKey).collect(MoreCollectors.toSet());
      Set<RuleKey> ruleKeysToLoad = copyOf(difference(collector.get(RULES), preloaedRuleKeys));
      if (ruleKeysToLoad.isEmpty()) {
        result.setRules(preloadedResponseData.getRules());
      } else {
        List<RuleDefinitionDto> loadedRules = dbClient.ruleDao().selectDefinitionByKeys(dbSession, ruleKeysToLoad);
        result.setRules(concat(preloadedRules.stream(), loadedRules.stream()).collect(toList(preloadedRules.size() + loadedRules.size())));
      }
    }
  }

  private void loadUsers(Collector collector, DbSession dbSession, SearchResponseData result) {
    if (collector.contains(USERS)) {
      result.setUsers(dbClient.userDao().selectByLogins(dbSession, collector.getList(USERS)));
    }
  }

  private void loadComments(Collector collector, DbSession dbSession, SearchResponseData result) {
    if (collector.contains(COMMENTS)) {
      List<IssueChangeDto> comments = dbClient.issueChangeDao().selectByTypeAndIssueKeys(dbSession, collector.getIssueKeys(), IssueChangeDto.TYPE_COMMENT);
      result.setComments(comments);
      for (IssueChangeDto comment : comments) {
        collector.add(USERS, comment.getUserLogin());
        if (canEditOrDelete(comment)) {
          result.addUpdatableComment(comment.getKey());
        }
      }
    }
  }

  private boolean canEditOrDelete(IssueChangeDto dto) {
    return userSession.isLoggedIn() && userSession.getLogin().equals(dto.getUserLogin());
  }

  private void loadRules(Collector collector, DbSession dbSession, SearchResponseData result) {
    if (collector.contains(RULES)) {
      result.setRules(dbClient.ruleDao().selectDefinitionByKeys(dbSession, collector.getList(RULES)));
    }
  }

  private void loadComponents(Collector collector, DbSession dbSession, SearchResponseData result) {
    // always load components and projects, because some issue fields still relate to component ids/keys.
    // They should be dropped but are kept for backward-compatibility (see SearchResponseFormat)
    result.addComponents(dbClient.componentDao().selectByUuids(dbSession, collector.getComponentUuids()));
    result.addComponents(dbClient.componentDao().selectSubProjectsByComponentUuids(dbSession, collector.getComponentUuids()));
    addProjectUuids(collector, dbSession, result);
  }

  private void loadOrganizations(DbSession dbSession, SearchResponseData result) {
    Collection<ComponentDto> components = result.getComponents();
    dbClient.organizationDao().selectByUuids(
      dbSession,
      components.stream().map(ComponentDto::getOrganizationUuid).collect(MoreCollectors.toSet()))
      .forEach(result::addOrganization);
  }

  private void loadActionsAndTransitions(Collector collector, SearchResponseData result) {
    if (collector.contains(ACTIONS) || collector.contains(TRANSITIONS)) {
      for (IssueDto dto : result.getIssues()) {
        // so that IssueDto can be used.
        if (collector.contains(ACTIONS)) {
          result.addActions(dto.getKey(), actionService.listAvailableActions(dto));
        }
        if (collector.contains(TRANSITIONS)) {
          // TODO workflow and action engines must not depend on org.sonar.api.issue.Issue but on a generic interface
          DefaultIssue issue = dto.toDefaultIssue();
          result.addTransitions(issue.key(), transitionService.listTransitions(issue));
        }
      }
    }
  }

  private static void completeTotalEffortFromFacet(@Nullable Facets facets, SearchResponseData result) {
    if (facets != null) {
      Map<String, Long> effortFacet = facets.get(IssuesWsParameters.FACET_MODE_EFFORT);
      if (effortFacet != null) {
        result.setEffortTotal(effortFacet.get(Facets.TOTAL));
      }
    }
  }

  /**
   * Collects the keys of all the data to be loaded (users, rules, ...)
   */
  public static class Collector {
    private final Set<SearchAdditionalField> fields;
    private final SetMultimap<SearchAdditionalField, Object> fieldValues = MultimapBuilder.enumKeys(SearchAdditionalField.class).hashSetValues().build();
    private final Set<String> componentUuids = new HashSet<>();
    private final Set<String> projectUuids = new HashSet<>();
    private final List<String> issueKeys;

    public Collector(Set<SearchAdditionalField> fields, List<String> issueKeys) {
      this.fields = fields;
      this.issueKeys = issueKeys;
    }

    void collect(List<IssueDto> issues) {
      for (IssueDto issue : issues) {
        componentUuids.add(issue.getComponentUuid());
        projectUuids.add(issue.getProjectUuid());
        add(RULES, issue.getRuleKey());
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

    <T> Set<T> get(SearchAdditionalField key) {
      return (Set<T>) fieldValues.get(key);
    }

    <T> List<T> getList(SearchAdditionalField key) {
      return newArrayList(get(key));
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
