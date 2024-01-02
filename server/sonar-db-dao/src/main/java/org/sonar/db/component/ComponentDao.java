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
package org.sonar.db.component;

import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentNewValue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.checkThatNotTooManyConditions;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsIntoSet;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class ComponentDao implements Dao {
  private final AuditPersister auditPersister;

  public ComponentDao(AuditPersister auditPersister) {
    this.auditPersister = auditPersister;
  }

  /*
   * SELECT BY UUID
   */
  public Optional<ComponentDto> selectByUuid(DbSession session, String uuid) {
    return Optional.ofNullable(mapper(session).selectByUuid(uuid));
  }

  public ComponentDto selectOrFailByUuid(DbSession session, String uuid) {
    return selectByUuid(session, uuid).orElseThrow(() -> new RowNotFoundException(String.format("Component with uuid '%s' not found", uuid)));
  }

  public List<ComponentDto> selectByUuids(DbSession session, Collection<String> uuids) {
    return executeLargeInputs(uuids, mapper(session)::selectByUuids);
  }

  public List<String> selectExistingUuids(DbSession session, Collection<String> uuids) {
    return executeLargeInputs(uuids, mapper(session)::selectExistingUuids);
  }

  public List<ComponentDto> selectSubProjectsByComponentUuids(DbSession session, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }
    return mapper(session).selectSubProjectsByComponentUuids(uuids);
  }

  public List<ComponentDto> selectDescendantModules(DbSession session, String rootComponentUuid) {
    return mapper(session).selectDescendantModules(rootComponentUuid, Scopes.PROJECT, false);
  }

  public List<ComponentDto> selectEnabledDescendantModules(DbSession session, String rootComponentUuid) {
    return mapper(session).selectDescendantModules(rootComponentUuid, Scopes.PROJECT, true);
  }

  public List<FilePathWithHashDto> selectEnabledDescendantFiles(DbSession session, String rootComponentUuid) {
    return mapper(session).selectDescendantFiles(rootComponentUuid, Scopes.FILE, true);
  }

  public List<FilePathWithHashDto> selectEnabledFilesFromProject(DbSession session, String rootComponentUuid) {
    return mapper(session).selectEnabledFilesFromProject(rootComponentUuid);
  }

  /**
   * Retrieves all components with a specific branch UUID, no other filtering is done by this method.
   */
  public List<ComponentDto> selectByBranchUuid(String branchUuid, DbSession dbSession) {
    return mapper(dbSession).selectByBranchUuid(branchUuid);
  }

  public int countEnabledModulesByBranchUuid(DbSession session, String branchUuid) {
    return mapper(session).countEnabledModulesByBranchUuid(branchUuid);
  }

  /*
    SELECT BY QUERY
   */

  /**
   * @throws IllegalArgumentException if parameter query#getComponentIds() has more than {@link org.sonar.db.DatabaseUtils#PARTITION_SIZE_FOR_ORACLE} values
   * @throws IllegalArgumentException if parameter query#getComponentKeys() has more than {@link org.sonar.db.DatabaseUtils#PARTITION_SIZE_FOR_ORACLE} values
   * @throws IllegalArgumentException if parameter query#getMainComponentUuids() has more than {@link org.sonar.db.DatabaseUtils#PARTITION_SIZE_FOR_ORACLE} values
   */
  public List<ComponentDto> selectByQuery(DbSession dbSession, ComponentQuery query, int offset, int limit) {
    return selectByQueryImpl(dbSession, query, offset, limit);
  }

  /**
   * @throws IllegalArgumentException if parameter query#getComponentIds() has more than {@link org.sonar.db.DatabaseUtils#PARTITION_SIZE_FOR_ORACLE} values
   * @throws IllegalArgumentException if parameter query#getComponentKeys() has more than {@link org.sonar.db.DatabaseUtils#PARTITION_SIZE_FOR_ORACLE} values
   * @throws IllegalArgumentException if parameter query#getMainComponentUuids() has more than {@link org.sonar.db.DatabaseUtils#PARTITION_SIZE_FOR_ORACLE} values
   */
  public int countByQuery(DbSession session, ComponentQuery query) {
    return countByQueryImpl(session, query);
  }

  private static List<ComponentDto> selectByQueryImpl(DbSession session, ComponentQuery query, int offset, int limit) {
    if (query.hasEmptySetOfComponents()) {
      return emptyList();
    }
    checkThatNotTooManyComponents(query);
    return mapper(session).selectByQuery(query, new RowBounds(offset, limit));
  }

  private static int countByQueryImpl(DbSession session, ComponentQuery query) {
    if (query.hasEmptySetOfComponents()) {
      return 0;
    }
    checkThatNotTooManyComponents(query);
    return mapper(session).countByQuery(query);
  }

  /*
     SELECT BY KEY
   */

  /**
   * Return all components of a project (including disable ones)
   */
  public List<KeyWithUuidDto> selectUuidsByKeyFromProjectKey(DbSession session, String projectKey) {
    return mapper(session).selectUuidsByKeyFromProjectKeyAndBranchOrPr(projectKey, null, null);
  }

  public List<KeyWithUuidDto> selectUuidsByKeyFromProjectKeyAndBranch(DbSession session, String projectKey, String branch) {
    return mapper(session).selectUuidsByKeyFromProjectKeyAndBranchOrPr(projectKey, branch, null);
  }

  public List<KeyWithUuidDto> selectUuidsByKeyFromProjectKeyAndPullRequest(DbSession session, String projectKey, String pullrequest) {
    return mapper(session).selectUuidsByKeyFromProjectKeyAndBranchOrPr(projectKey, null, pullrequest);
  }

  /**
   * If no branch or pull request is provided, returns components in the main branch
   */
  public List<ComponentDto> selectProjectAndModulesFromProjectKey(DbSession session, String projectKey, boolean excludeDisabled,
    @Nullable String branch, @Nullable String pullRequest) {
    checkState(branch == null || pullRequest == null, "Can't set both branch and pull request");
    return mapper(session).selectComponentsFromProjectKeyAndScope(projectKey, Scopes.PROJECT, excludeDisabled, branch, pullRequest);
  }

  /**
   * If no branch or pull request is provided, returns components in the main branch
   */
  public List<ComponentDto> selectEnabledModulesFromProjectKey(DbSession session, String projectKey, @Nullable String branch, @Nullable String pullRequest) {
    return selectProjectAndModulesFromProjectKey(session, projectKey, true, branch, pullRequest);
  }

  public List<ComponentDto> selectByKeys(DbSession session, Collection<String> keys) {
    return selectByKeys(session, keys, null, null);
  }

  /**
   * If no branch or pull request is provided, returns components in the main branch
   */
  public List<ComponentDto> selectByKeys(DbSession session, Collection<String> keys, @Nullable String branch, @Nullable String pullRequest) {
    checkState(branch == null || pullRequest == null, "Can't set both branch and pull request");
    return executeLargeInputs(keys, subKeys -> mapper(session).selectByKeysAndBranchOrPr(subKeys, branch, pullRequest));
  }

  /**
   * Returns components in the main branch
   */
  public Optional<ComponentDto> selectByKey(DbSession session, String key) {
    return Optional.ofNullable(mapper(session).selectByKeyAndBranchOrPr(key, null, null));
  }

  public Optional<ComponentDto> selectByKeyAndBranch(DbSession session, String key, String branch) {
    return Optional.ofNullable(mapper(session).selectByKeyAndBranchOrPr(key, branch, null));
  }

  public Optional<ComponentDto> selectByKeyAndPullRequest(DbSession session, String key, String pullRequestId) {
    return Optional.ofNullable(mapper(session).selectByKeyAndBranchOrPr(key, null, pullRequestId));
  }

  public Optional<ComponentDto> selectByKeyCaseInsensitive(DbSession session, String key) {
    return Optional.ofNullable(mapper(session).selectByKeyCaseInsensitive(key));
  }

  /**
   * List of ancestors, ordered from root to parent. The list is empty
   * if the component is a tree root. Disabled components are excluded by design
   * as tree represents the more recent analysis.
   */
  public List<ComponentDto> selectAncestors(DbSession dbSession, ComponentDto component) {
    if (component.isRoot()) {
      return Collections.emptyList();
    }
    List<String> ancestorUuids = component.getUuidPathAsList();
    List<ComponentDto> ancestors = selectByUuids(dbSession, ancestorUuids);
    return Ordering.explicit(ancestorUuids).onResultOf(ComponentDto::uuid).immutableSortedCopy(ancestors);
  }

  /**
   * Select the children or the leaves of a base component, given by its UUID. The components that are not present in last
   * analysis are ignored.
   * <p>
   * An empty list is returned if the base component does not exist or if the base component is a leaf.
   */
  public List<ComponentDto> selectDescendants(DbSession dbSession, ComponentTreeQuery query) {
    Optional<ComponentDto> componentOpt = selectByUuid(dbSession, query.getBaseUuid());
    if (!componentOpt.isPresent()) {
      return emptyList();
    }
    ComponentDto component = componentOpt.get();
    return mapper(dbSession).selectDescendants(query, componentOpt.get().uuid(), query.getUuidPath(component));
  }

  public List<ComponentDto> selectChildren(DbSession dbSession, String branchUuid, Collection<ComponentDto> components) {
    Set<String> uuidPaths = components.stream().map(c -> c.getUuidPath() + c.uuid() + ".").collect(Collectors.toSet());
    return mapper(dbSession).selectChildren(branchUuid, uuidPaths);
  }

  /*
    SELECT ALL
   */
  public List<UuidWithBranchUuidDto> selectAllViewsAndSubViews(DbSession session) {
    return mapper(session).selectUuidsForQualifiers(Qualifiers.APP, Qualifiers.VIEW, Qualifiers.SUBVIEW);
  }

  /**
   * Returns all projects (Scope {@link Scopes#PROJECT} and qualifier
   * {@link Qualifiers#PROJECT}) which are enabled.
   * <p>
   * Branches are not returned.
   * <p>
   * Used by Views.
   */
  public List<ComponentDto> selectProjects(DbSession session) {
    return mapper(session).selectProjects();
  }

  /**
   * Used by Governance
   */
  public Set<String> selectViewKeysWithEnabledCopyOfProject(DbSession session, Set<String> projectUuids) {
    return executeLargeInputsIntoSet(projectUuids,
      partition -> mapper(session).selectViewKeysWithEnabledCopyOfProject(partition),
      i -> i);
  }

  public List<String> selectProjectsFromView(DbSession session, String viewUuid, String projectViewUuid) {
    var escapedViewUuid = viewUuid.replace("_", "\\_").replace("%", "\\%");
    return mapper(session).selectProjectsFromView("%." + escapedViewUuid + ".%", projectViewUuid);
  }

  /**
   * Selects all components that are relevant for indexing. The result is not returned (since it is usually too big), but handed over to the <code>handler</code>
   *
   * @param session     the database session
   * @param projectUuid the project uuid, which is selected with all of its children
   * @param handler     the action to be applied to every result
   */
  public void scrollForIndexing(DbSession session, @Nullable String projectUuid, ResultHandler<ComponentDto> handler) {
    mapper(session).scrollForIndexing(projectUuid, handler);
  }

  /**
   * Retrieve enabled components keys with given qualifiers
   * <p>
   * Used by Views plugin
   */
  public Set<ComponentDto> selectComponentsByQualifiers(DbSession dbSession, Set<String> qualifiers) {
    checkArgument(!qualifiers.isEmpty(), "Qualifiers cannot be empty");
    return new HashSet<>(mapper(dbSession).selectComponentsByQualifiers(qualifiers));
  }

  public List<ComponentWithModuleUuidDto> selectEnabledComponentsWithModuleUuidFromProjectKey(DbSession dbSession, String projectKey) {
    return mapper(dbSession).selectEnabledComponentsWithModuleUuidFromProjectKey(projectKey);
  }

  /**
   * Returns components with open issues from P/Rs that use a certain branch as reference (reference branch).
   * Excludes components from the current branch.
   */
  public List<KeyWithUuidDto> selectComponentsFromPullRequestsTargetingCurrentBranchThatHaveOpenIssues(DbSession dbSession, String referenceBranchUuid, String currentBranchUuid) {
    return mapper(dbSession).selectComponentsFromPullRequestsTargetingCurrentBranchThatHaveOpenIssues(referenceBranchUuid, currentBranchUuid);
  }

  /**
   * Returns components with open issues from the given branches
   */
  public List<KeyWithUuidDto> selectComponentsFromBranchesThatHaveOpenIssues(DbSession dbSession, Set<String> branchUuids) {
    if (branchUuids.isEmpty()) {
      return emptyList();
    }

    return executeLargeInputs(branchUuids, input -> mapper(dbSession).selectComponentsFromBranchesThatHaveOpenIssues(input));
  }

  /**
   * Scroll all <strong>enabled</strong> files of the specified project (same project_uuid) in no specific order with
   * 'SOURCE' source and a non null path.
   */
  public void scrollAllFilesForFileMove(DbSession session, String branchUuid, ResultHandler<FileMoveRowDto> handler) {
    mapper(session).scrollAllFilesForFileMove(branchUuid, handler);
  }

  public List<ProjectNclocDistributionDto> selectPrivateProjectsWithNcloc(DbSession dbSession) {
    return mapper(dbSession).selectPrivateProjectsWithNcloc();
  }

  public boolean existAnyOfComponentsWithQualifiers(DbSession session, Collection<String> componentKeys, Set<String> qualifiers) {
    if (!componentKeys.isEmpty()) {
      List<Boolean> result = new LinkedList<>();
      return executeLargeInputs(componentKeys, input -> {
        boolean groupNeedIssueSync = mapper(session).checkIfAnyOfComponentsWithQualifiers(input, qualifiers) > 0;
        result.add(groupNeedIssueSync);
        return result;
      }).stream().anyMatch(b -> b);
    }
    return false;
  }

  /*
    INSERT / UPDATE
   */
  public void insert(DbSession session, ComponentDto item) {
    mapper(session).insert(item);
    if (!isBranchOrPullRequest(item)) {
      auditPersister.addComponent(session, new ComponentNewValue(item));
    }
  }

  public void insert(DbSession session, Collection<ComponentDto> items) {
    insert(session, items.stream());
  }

  private void insert(DbSession session, Stream<ComponentDto> items) {
    items.forEach(item -> insert(session, item));
  }

  public void insert(DbSession session, ComponentDto item, ComponentDto... others) {
    insert(session, Stream.concat(Stream.of(item), Arrays.stream(others)));
  }

  public void update(DbSession session, ComponentUpdateDto component, String qualifier) {
    auditPersister.updateComponent(session, new ComponentNewValue(component.getUuid(), component.getBName(),
      component.getBKey(), component.isBEnabled(), component.getBPath(), qualifier));
    mapper(session).update(component);
  }

  public void updateBEnabledToFalse(DbSession session, Collection<String> uuids) {
    executeLargeUpdates(uuids, mapper(session)::updateBEnabledToFalse);
  }

  public void applyBChangesForRootComponentUuid(DbSession session, String branchUuid) {
    mapper(session).applyBChangesForRootComponentUuid(branchUuid);
  }

  public void resetBChangedForRootComponentUuid(DbSession session, String branchUuid) {
    mapper(session).resetBChangedForRootComponentUuid(branchUuid);
  }

  public void setPrivateForRootComponentUuidWithoutAudit(DbSession session, String branchUuid, boolean isPrivate) {
    mapper(session).setPrivateForRootComponentUuid(branchUuid, isPrivate);
  }

  public void setPrivateForRootComponentUuid(DbSession session, String branchUuid, boolean isPrivate, String qualifier, String componentKey, String componentName) {
    ComponentNewValue componentNewValue = new ComponentNewValue(branchUuid, componentName, componentKey, isPrivate, qualifier);
    auditPersister.updateComponentVisibility(session, componentNewValue);
    mapper(session).setPrivateForRootComponentUuid(branchUuid, isPrivate);
  }

  /*
     UTIL
   */
  private static ComponentMapper mapper(DbSession session) {
    return session.getMapper(ComponentMapper.class);
  }

  private static void checkThatNotTooManyComponents(ComponentQuery query) {
    checkThatNotTooManyConditions(query.getComponentKeys(), "Too many component keys in query");
    checkThatNotTooManyConditions(query.getComponentUuids(), "Too many component UUIDs in query");
  }

  private static boolean isBranchOrPullRequest(ComponentDto item) {
    return item.getMainBranchProjectUuid() != null;
  }

}
