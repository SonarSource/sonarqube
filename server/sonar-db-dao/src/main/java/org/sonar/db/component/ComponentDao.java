/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.db.DaoDatabaseUtils.buildLikeValue;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;
import static org.sonar.db.WildcardPosition.BEFORE_AND_AFTER;
import static org.sonar.db.component.ComponentDto.generateBranchKey;

public class ComponentDao implements Dao {

  private static List<ComponentDto> selectByQueryImpl(DbSession session, @Nullable String organizationUuid, ComponentQuery query, int offset, int limit) {
    if (query.hasEmptySetOfComponents()) {
      return emptyList();
    }
    return mapper(session).selectByQuery(organizationUuid, query, new RowBounds(offset, limit));
  }

  private static int countByQueryImpl(DbSession session, @Nullable String organizationUuid, ComponentQuery query) {
    if (query.hasEmptySetOfComponents()) {
      return 0;
    }

    return mapper(session).countByQuery(organizationUuid, query);
  }

  @CheckForNull
  private static String buildUpperLikeSql(@Nullable String textQuery) {
    if (isBlank(textQuery)) {
      return null;
    }
    return buildLikeValue(textQuery.toUpperCase(Locale.ENGLISH), BEFORE_AND_AFTER);
  }

  private static ComponentMapper mapper(DbSession session) {
    return session.getMapper(ComponentMapper.class);
  }

  public ComponentDto selectOrFailById(DbSession session, long id) {
    Optional<ComponentDto> componentDto = selectById(session, id);
    if (!componentDto.isPresent()) {
      throw new RowNotFoundException(String.format("Component id does not exist: %d", id));
    }
    return componentDto.get();
  }

  public Optional<ComponentDto> selectById(DbSession session, long id) {
    return Optional.fromNullable(mapper(session).selectById(id));
  }

  public Optional<ComponentDto> selectByUuid(DbSession session, String uuid) {
    return Optional.fromNullable(mapper(session).selectByUuid(uuid));
  }

  public ComponentDto selectOrFailByUuid(DbSession session, String uuid) {
    Optional<ComponentDto> componentDto = selectByUuid(session, uuid);
    if (!componentDto.isPresent()) {
      throw new RowNotFoundException(String.format("Component with uuid '%s' not found", uuid));
    }
    return componentDto.get();
  }

  public List<ComponentDto> selectByQuery(DbSession session, ComponentQuery query, int offset, int limit) {
    return selectByQueryImpl(session, null, query, offset, limit);
  }

  public List<ComponentDto> selectByQuery(DbSession dbSession, String organizationUuid, ComponentQuery query, int offset, int limit) {
    requireNonNull(organizationUuid, "organizationUuid can't be null");
    return selectByQueryImpl(dbSession, organizationUuid, query, offset, limit);
  }

  public int countByQuery(DbSession session, ComponentQuery query) {
    return countByQueryImpl(session, null, query);
  }

  public int countByQuery(DbSession session, String organizationUuid, ComponentQuery query) {
    requireNonNull(organizationUuid, "organizationUuid can't be null");
    return countByQueryImpl(session, organizationUuid, query);
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

  public List<ComponentDto> selectByIds(DbSession session, Collection<Long> ids) {
    return executeLargeInputs(ids, mapper(session)::selectByIds);
  }

  public List<ComponentDto> selectByUuids(DbSession session, Collection<String> uuids) {
    return executeLargeInputs(uuids, mapper(session)::selectByUuids);
  }

  public List<String> selectExistingUuids(DbSession session, Collection<String> uuids) {
    return executeLargeInputs(uuids, mapper(session)::selectExistingUuids);
  }

  /**
   * Return all components of a project (including disable ones)
   */
  public List<ComponentDto> selectAllComponentsFromProjectKey(DbSession session, String projectKey) {
    return mapper(session).selectComponentsFromProjectKeyAndScope(projectKey, null, false);
  }

  public List<KeyWithUuidDto> selectUuidsByKeyFromProjectKey(DbSession session, String projectKey) {
    return mapper(session).selectUuidsByKeyFromProjectKey(projectKey);
  }

  public List<ComponentDto> selectEnabledModulesFromProjectKey(DbSession session, String projectKey) {
    return mapper(session).selectComponentsFromProjectKeyAndScope(projectKey, Scopes.PROJECT, true);
  }

  public List<ComponentDto> selectByKeys(DbSession session, Collection<String> keys) {
    return executeLargeInputs(keys, mapper(session)::selectByKeys);
  }

  public List<ComponentDto> selectByKeysAndBranch(DbSession session, Collection<String> keys, String branch) {
    List<String> dbKeys = keys.stream().map(k -> generateBranchKey(k, branch)).collect(toList());
    List<String> allKeys = Stream.of(keys, dbKeys).flatMap(Collection::stream).collect(toList());
    return executeLargeInputs(allKeys, subKeys -> mapper(session).selectByKeysAndBranch(subKeys, branch));
  }

  public List<ComponentDto> selectComponentsHavingSameKeyOrderedById(DbSession session, String key) {
    return mapper(session).selectComponentsHavingSameKeyOrderedById(key);
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
   *
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

  public ComponentDto selectOrFailByKey(DbSession session, String key) {
    Optional<ComponentDto> component = selectByKey(session, key);
    if (!component.isPresent()) {
      throw new RowNotFoundException(String.format("Component key '%s' not found", key));
    }
    return component.get();
  }

  public Optional<ComponentDto> selectByKey(DbSession session, String key) {
    return Optional.fromNullable(mapper(session).selectByKey(key));
  }

  public java.util.Optional<ComponentDto> selectByKeyAndBranch(DbSession session, String key, String branch) {
    return java.util.Optional.ofNullable(mapper(session).selectByKeyAndBranch(key, generateBranchKey(key, branch), branch));
  }

  public List<UuidWithProjectUuidDto> selectAllViewsAndSubViews(DbSession session) {
    return mapper(session).selectUuidsForQualifiers(Qualifiers.APP, Qualifiers.VIEW, Qualifiers.SUBVIEW);
  }

  public List<String> selectProjectsFromView(DbSession session, String viewUuid, String projectViewUuid) {
    return mapper(session).selectProjectsFromView("%." + viewUuid + ".%", projectViewUuid);
  }

  /**
   * Returns all projects (Scope {@link Scopes#PROJECT} and qualifier
   * {@link Qualifiers#PROJECT}) which are enabled.
   *
   * Branches are not returned.
   *
   * Used by Views.
   */
  public List<ComponentDto> selectProjects(DbSession session) {
    return mapper(session).selectProjects();
  }

  /**
   * Select all root components (projects and views), including disabled ones, for a given organization.
   *
   * Branches are not returned
   */
  public List<ComponentDto> selectAllRootsByOrganization(DbSession dbSession, String organizationUuid) {
    return mapper(dbSession).selectAllRootsByOrganization(organizationUuid);
  }

  public List<ComponentDto> selectGhostProjects(DbSession session, String organizationUuid, @Nullable String query, int offset, int limit) {
    return mapper(session).selectGhostProjects(organizationUuid, buildUpperLikeSql(query), new RowBounds(offset, limit));
  }

  public long countGhostProjects(DbSession session, String organizationUuid, @Nullable String query) {
    return mapper(session).countGhostProjects(organizationUuid, buildUpperLikeSql(query));
  }

  /**
   * Selects all components that are relevant for indexing. The result is not returned (since it is usually too big), but handed over to the <code>handler</code>
   * @param session the database session
   * @param projectUuid the project uuid, which is selected with all of its children
   * @param handler the action to be applied to every result
   */
  public void scrollForIndexing(DbSession session, @Nullable String projectUuid, ResultHandler<ComponentDto> handler) {
    mapper(session).scrollForIndexing(projectUuid, handler);
  }

  /**
   * Retrieves all components with a specific root project Uuid, no other filtering is done by this method.
   *
   * Used by Views plugin
   */
  public List<ComponentDto> selectByProjectUuid(String projectUuid, DbSession dbSession) {
    return mapper(dbSession).selectByProjectUuid(projectUuid);
  }

  /**
   * Retrieve enabled components keys with given qualifiers
   *
   * Used by Views plugin
   */
  public Set<ComponentDto> selectComponentsByQualifiers(DbSession dbSession, Set<String> qualifiers) {
    checkArgument(!qualifiers.isEmpty(), "Qualifiers cannot be empty");
    return new HashSet<>(mapper(dbSession).selectComponentsByQualifiers(qualifiers));
  }

  public List<ComponentDto> selectProjectsByNameQuery(DbSession dbSession, @Nullable String nameQuery, boolean includeModules) {
    String nameQueryForSql = nameQuery == null ? null : buildLikeValue(nameQuery, BEFORE_AND_AFTER).toUpperCase(Locale.ENGLISH);
    return mapper(dbSession).selectProjectsByNameQuery(nameQueryForSql, includeModules);
  }

  public void insert(DbSession session, ComponentDto item) {
    mapper(session).insert(item);
  }

  public void insert(DbSession session, Collection<ComponentDto> items) {
    for (ComponentDto item : items) {
      insert(session, item);
    }
  }

  public void insert(DbSession session, ComponentDto item, ComponentDto... others) {
    insert(session, Lists.asList(item, others));
  }

  public void update(DbSession session, ComponentUpdateDto component) {
    mapper(session).update(component);
  }

  public void updateTags(DbSession session, ComponentDto component) {
    mapper(session).updateTags(component);
  }

  public void updateBEnabledToFalse(DbSession session, Collection<String> uuids) {
    executeLargeUpdates(uuids, mapper(session)::updateBEnabledToFalse);
  }

  public void applyBChangesForRootComponentUuid(DbSession session, String projectUuid) {
    mapper(session).applyBChangesForRootComponentUuid(projectUuid);
  }

  public void resetBChangedForRootComponentUuid(DbSession session, String projectUuid) {
    mapper(session).resetBChangedForRootComponentUuid(projectUuid);
  }

  public void setPrivateForRootComponentUuid(DbSession session, String projectUuid, boolean isPrivate) {
    mapper(session).setPrivateForRootComponentUuid(projectUuid, isPrivate);
  }

  public void delete(DbSession session, long componentId) {
    mapper(session).delete(componentId);
  }

  public List<KeyWithUuidDto> selectComponentKeysHavingIssuesToMerge(DbSession dbSession, String mergeBranchUuid) {
    return mapper(dbSession).selectComponentKeysHavingIssuesToMerge(mergeBranchUuid);
  }
}
