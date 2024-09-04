/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.Facets;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.issue.IssuesWsParameters;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Stream.concat;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.server.issue.AssignAction.ASSIGN_KEY;
import static org.sonar.server.issue.CommentAction.COMMENT_KEY;
import static org.sonar.server.issue.SetSeverityAction.SET_SEVERITY_KEY;
import static org.sonar.server.issue.SetTypeAction.SET_TYPE_KEY;
import static org.sonar.server.issue.ws.SearchAdditionalField.ACTIONS;
import static org.sonar.server.issue.ws.SearchAdditionalField.COMMENTS;
import static org.sonar.server.issue.ws.SearchAdditionalField.TRANSITIONS;

/**
 * Loads all the information required for the response of api/issues/search.
 */
public class SearchResponseLoader {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final TransitionService transitionService;

  public SearchResponseLoader(UserSession userSession, DbClient dbClient, TransitionService transitionService) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.transitionService = transitionService;
  }

  /**
   * The issue keys are given by the multi-criteria search in Elasticsearch index.
   * <p>
   * It will only retrieve from DB data which is not already provided by the specified preloaded {@link SearchResponseData}.<br/>
   * The returned {@link SearchResponseData} is <strong>not</strong> the one specified as argument.
   * </p>
   */
  public SearchResponseData load(SearchResponseData preloadedResponseData, Collector collector, Set<SearchAdditionalField> fields, @Nullable Facets facets) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SearchResponseData result = new SearchResponseData(loadIssues(preloadedResponseData, collector, dbSession));
      collector.collect(result.getIssues());

      loadRules(preloadedResponseData, collector, dbSession, result);
      // order is important - loading of comments complete the list of users: loadComments() is before loadUsers()
      loadComments(collector, dbSession, fields, result);
      loadUsers(preloadedResponseData, collector, dbSession, result);
      loadComponents(preloadedResponseData, collector, dbSession, result);
      // for all loaded components in result we "join" branches to know to which branch components belong
      loadBranches(dbSession, result);
      loadProjects(collector, dbSession, result);

      loadActionsAndTransitions(result, fields);
      completeTotalEffortFromFacet(facets, result);
      return result;
    }
  }

  private List<IssueDto> loadIssues(SearchResponseData preloadedResponseData, Collector collector, DbSession dbSession) {
    List<IssueDto> preloadedIssues = preloadedResponseData.getIssues();
    Set<String> preloadedIssueKeys = preloadedIssues.stream().map(IssueDto::getKey).collect(Collectors.toSet());

    ImmutableSet<String> issueKeys = ImmutableSet.copyOf(collector.getIssueKeys());
    Set<String> issueKeysToLoad = copyOf(difference(issueKeys, preloadedIssueKeys));

    if (issueKeysToLoad.isEmpty()) {
      return issueKeys.stream()
        .map(new KeyToIssueFunction(preloadedIssues)).filter(Objects::nonNull)
        .toList();
    }

    List<IssueDto> loadedIssues = dbClient.issueDao().selectByKeys(dbSession, issueKeysToLoad);
    List<IssueDto> unorderedIssues = concat(preloadedIssues.stream(), loadedIssues.stream())
      .toList();

    return issueKeys.stream()
      .map(new KeyToIssueFunction(unorderedIssues))
      .filter(Objects::nonNull)
      .toList();
  }

  private void loadUsers(SearchResponseData preloadedResponseData, Collector collector, DbSession dbSession, SearchResponseData result) {
    Set<String> usersUuidToLoad = collector.getUserUuids();
    List<UserDto> preloadedUsers = firstNonNull(preloadedResponseData.getUsers(), emptyList());
    result.addUsers(preloadedUsers);
    preloadedUsers.forEach(userDto -> usersUuidToLoad.remove(userDto.getUuid()));
    result.addUsers(dbClient.userDao().selectByUuids(dbSession, usersUuidToLoad));
  }

  private void loadComponents(SearchResponseData preloadedResponseData, Collector collector, DbSession dbSession, SearchResponseData result) {
    Collection<ComponentDto> preloadedComponents = preloadedResponseData.getComponents();
    Set<String> preloadedComponentUuids = preloadedComponents.stream().map(ComponentDto::uuid).collect(Collectors.toSet());
    Set<String> componentUuidsToLoad = copyOf(difference(collector.getComponentUuids(), preloadedComponentUuids));

    result.addComponents(preloadedComponents);
    if (!componentUuidsToLoad.isEmpty()) {
      result.addComponents(dbClient.componentDao().selectByUuids(dbSession, componentUuidsToLoad));
    }

    // always load components and branches, because some issue fields still relate to component ids/keys.
    // They should be dropped but are kept for backward-compatibility (see SearchResponseFormat)
    result.addComponents(dbClient.componentDao().selectSubProjectsByComponentUuids(dbSession, collector.getComponentUuids()));
    loadBranchComponents(collector, dbSession, result);
  }

  private void loadProjects(Collector collector, DbSession dbSession, SearchResponseData result) {
    List<ProjectDto> projects = dbClient.projectDao().selectByUuids(dbSession, collector.getProjectUuids());
    result.addProjects(projects);
  }

  private void loadBranches(DbSession dbSession, SearchResponseData result) {
    Set<String> branchUuids = result.getComponents().stream()
      .map(c -> c.getCopyComponentUuid() != null ? c.getCopyComponentUuid() : c.branchUuid())
      .collect(Collectors.toSet());
    List<BranchDto> branchDtos = dbClient.branchDao().selectByUuids(dbSession, branchUuids);
    result.addBranches(branchDtos);
  }

  private void loadBranchComponents(Collector collector, DbSession dbSession, SearchResponseData result) {
    Collection<ComponentDto> loadedComponents = result.getComponents();
    for (ComponentDto component : loadedComponents) {
      collector.addBranchUuid(component.branchUuid());
    }
    Set<String> loadedBranchUuids = loadedComponents.stream().filter(cpt -> cpt.uuid().equals(cpt.branchUuid())).map(ComponentDto::uuid).collect(Collectors.toSet());
    Set<String> branchUuidsToLoad = copyOf(difference(collector.getBranchUuids(), loadedBranchUuids));
    if (!branchUuidsToLoad.isEmpty()) {
      List<ComponentDto> branchComponents = dbClient.componentDao().selectByUuids(dbSession, collector.getBranchUuids());
      result.addComponents(branchComponents);
    }
  }

  private void loadRules(SearchResponseData preloadedResponseData, Collector collector, DbSession dbSession, SearchResponseData result) {
    List<RuleDto> preloadedRules = firstNonNull(preloadedResponseData.getRules(), emptyList());
    result.addRules(preloadedRules);
    Set<String> ruleUuidsToLoad = collector.getRuleUuids();
    preloadedRules.stream().map(RuleDto::getUuid).toList()
      .forEach(ruleUuidsToLoad::remove);

    List<RuleDto> rules = dbClient.ruleDao().selectByUuids(dbSession, ruleUuidsToLoad);
    result.addRules(rules);
    updateNamesOfAdHocRules(result.getRules());
  }

  private static void updateNamesOfAdHocRules(List<RuleDto> rules) {
    rules.stream()
      .filter(RuleDto::isAdHoc)
      .filter(r -> r.getAdHocName() != null)
      .forEach(r -> r.setName(r.getAdHocName()));
  }

  private void loadComments(Collector collector, DbSession dbSession, Set<SearchAdditionalField> fields, SearchResponseData result) {
    if (fields.contains(COMMENTS)) {
      List<IssueChangeDto> comments = dbClient.issueChangeDao().selectByTypeAndIssueKeys(dbSession, collector.getIssueKeys(), IssueChangeDto.TYPE_COMMENT);
      result.setComments(comments);
      comments.stream()
        .filter(c -> c.getUserUuid() != null)
        .forEach(comment -> loadComment(collector, result, comment));
    }
  }

  private void loadComment(Collector collector, SearchResponseData result, IssueChangeDto comment) {
    collector.addUserUuids(singletonList(comment.getUserUuid()));
    if (canEditOrDelete(comment)) {
      result.addUpdatableComment(comment.getKey());
    }
  }

  private boolean canEditOrDelete(IssueChangeDto dto) {
    return userSession.isLoggedIn() && requireNonNull(userSession.getUuid(), "User uuid should not be null").equals(dto.getUserUuid());
  }

  private void loadActionsAndTransitions(SearchResponseData result, Set<SearchAdditionalField> fields) {
    if (fields.contains(ACTIONS) || fields.contains(TRANSITIONS)) {
      Map<String, ComponentDto> componentsByProjectUuid = result.getComponents()
        .stream()
        .filter(ComponentDto::isRootProject)
        .collect(Collectors.toMap(ComponentDto::branchUuid, Function.identity()));
      for (IssueDto issueDto : result.getIssues()) {
        // so that IssueDto can be used.
        if (fields.contains(ACTIONS)) {
          ComponentDto branch = componentsByProjectUuid.get(issueDto.getProjectUuid());
          result.addActions(issueDto.getKey(), listAvailableActions(issueDto, branch));
        }
        if (fields.contains(TRANSITIONS)) {
          // TODO workflow and action engines must not depend on org.sonar.api.issue.Issue but on a generic interface
          DefaultIssue issue = issueDto.toDefaultIssue();
          result.addTransitions(issue.key(), transitionService.listTransitions(issue));
        }
      }
    }
  }

  private Set<String> listAvailableActions(IssueDto issue, ComponentDto branch) {
    Set<String> availableActions = newHashSet();
    String login = userSession.getLogin();
    if (login == null) {
      return Collections.emptySet();
    }
    RuleType ruleType = RuleType.valueOf(issue.getType());
    availableActions.add(COMMENT_KEY);
    availableActions.add("set_tags");
    if (issue.getResolution() != null) {
      return availableActions;
    }
    availableActions.add(ASSIGN_KEY);
    if (ruleType != RuleType.SECURITY_HOTSPOT && userSession.hasComponentPermission(ISSUE_ADMIN, branch)) {
      availableActions.add(SET_TYPE_KEY);
      availableActions.add(SET_SEVERITY_KEY);
    }
    return availableActions;
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
    private final Set<String> componentUuids = new HashSet<>();
    private final Set<String> branchUuids = new HashSet<>();
    private final Set<String> projectUuids = new HashSet<>();
    private final List<String> issueKeys;
    private final Set<String> ruleUuids = new HashSet<>();
    private final Set<String> userUuids = new HashSet<>();

    public Collector(List<String> issueKeys) {
      this.issueKeys = issueKeys;
    }

    void collect(List<IssueDto> issues) {
      for (IssueDto issue : issues) {
        componentUuids.add(issue.getComponentUuid());
        branchUuids.add(issue.getProjectUuid());
        Optional.ofNullable(issue.getRuleUuid()).ifPresent(ruleUuids::add);
        String issueAssigneeUuid = issue.getAssigneeUuid();
        if (issueAssigneeUuid != null) {
          userUuids.add(issueAssigneeUuid);
        }
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

    void addProjectUuids(@Nullable Collection<String> uuids) {
      if (uuids != null) {
        this.projectUuids.addAll(uuids);
      }
    }

    void addBranchUuid(String uuid) {
      this.branchUuids.add(uuid);
    }

    void addRuleIds(@Nullable Collection<String> ruleUuids) {
      if (ruleUuids != null) {
        this.ruleUuids.addAll(ruleUuids);
      }
    }

    void addUserUuids(@Nullable Collection<String> userUuids) {
      if (userUuids != null) {
        this.userUuids.addAll(userUuids);
      }
    }

    public Set<String> getProjectUuids() {
      return projectUuids;
    }

    public List<String> getIssueKeys() {
      return issueKeys;
    }

    public Set<String> getComponentUuids() {
      return componentUuids;
    }

    public Set<String> getBranchUuids() {
      return branchUuids;
    }

    public Set<String> getRuleUuids() {
      return ruleUuids;
    }

    Set<String> getUserUuids() {
      return userUuids;
    }
  }

  private static class KeyToIssueFunction implements Function<String, IssueDto> {
    private final Map<String, IssueDto> map = new HashMap<>();

    private KeyToIssueFunction(Collection<IssueDto> unordered) {
      for (IssueDto dto : unordered) {
        map.put(dto.getKey(), dto);
      }
    }

    @Nullable
    @Override
    public IssueDto apply(String issueKey) {
      return map.get(issueKey);
    }
  }

}
