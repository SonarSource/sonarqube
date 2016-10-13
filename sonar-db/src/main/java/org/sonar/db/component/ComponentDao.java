/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.WildcardPosition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static java.util.Collections.emptyList;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class ComponentDao implements Dao {

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
    if (query.getComponentIds() != null && query.getComponentIds().isEmpty()) {
      return emptyList();
    }
    return mapper(session).selectByQuery(query, new RowBounds(offset, limit));
  }

  public int countByQuery(DbSession session, ComponentQuery query) {
    if (query.getComponentIds() != null && query.getComponentIds().isEmpty()) {
      return 0;
    }
    return mapper(session).countByQuery(query);
  }

  public List<ComponentDto> selectSubProjectsByComponentUuids(DbSession session, Collection<String> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return mapper(session).selectSubProjectsByComponentUuids(keys);
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

  public List<ComponentDto> selectEnabledModulesFromProjectKey(DbSession session, String projectKey) {
    return mapper(session).selectComponentsFromProjectKeyAndScope(projectKey, Scopes.PROJECT, true);
  }

  public List<ComponentDto> selectByKeys(DbSession session, Collection<String> keys) {
    return executeLargeInputs(keys, mapper(session)::selectByKeys);
  }

  public List<ComponentDto> selectComponentsHavingSameKeyOrderedById(DbSession session, String key) {
    return mapper(session).selectComponentsHavingSameKeyOrderedById(key);
  }

  /**
   * Optional parent. It is absent if specified component is root.
   */
  public Optional<ComponentDto> selectParent(DbSession dbSession, ComponentDto component) {
    if (component.isRoot()) {
      return Optional.absent();
    }
    List<String> path = component.getUuidPathAsList();
    String parentUuid = path.get(path.size() - 1);
    return Optional.of(mapper(dbSession).selectByUuid(parentUuid));
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
   * Select the children of a base component, given by its UUID. The components that are not present in last
   * analysis are ignored.
   *
   * An empty list is returned if the base component does not exist or if the base component is a leaf.
   */
  public List<ComponentDto> selectChildren(DbSession dbSession, ComponentTreeQuery query) {
    Optional<ComponentDto> componentOpt = selectByUuid(dbSession, query.getBaseUuid());
    if (!componentOpt.isPresent()) {
      return emptyList();
    }
    ComponentDto component = componentOpt.get();
    RowBounds rowBounds = new RowBounds(offset(query.getPage(), query.getPageSize()), query.getPageSize());
    return mapper(dbSession).selectChildren(query, uuidPathForChildrenQuery(component), rowBounds);
  }

  /**
   * Count the children of a base component, given by its UUID. The components that are not present in last
   * analysis are ignored.
   *
   * Zero is returned if the base component does not exist or if the base component is a leaf.
   */
  public int countChildren(DbSession dbSession, ComponentTreeQuery query) {
    Optional<ComponentDto> componentOpt = selectByUuid(dbSession, query.getBaseUuid());
    if (!componentOpt.isPresent()) {
      return 0;
    }
    ComponentDto component = componentOpt.get();
    return mapper(dbSession).countChildren(query, uuidPathForChildrenQuery(component));
  }

  private static String uuidPathForChildrenQuery(ComponentDto component) {
    return component.getUuidPath() + component.uuid() + ".";
  }

  /**
   * Select the descendants of a base component, given by its UUID. The components that are not present in last
   * analysis are ignored.
   *
   * An empty list is returned if the base component does not exist or if the base component is a leaf.
   */
  public List<ComponentDto> selectDescendants(DbSession dbSession, ComponentTreeQuery query) {
    Optional<ComponentDto> componentOpt = selectByUuid(dbSession, query.getBaseUuid());
    if (!componentOpt.isPresent()) {
      return Collections.emptyList();
    }
    ComponentDto component = componentOpt.get();
    RowBounds rowBounds = new RowBounds(offset(query.getPage(), query.getPageSize()), query.getPageSize());
    return mapper(dbSession).selectDescendants(query, uuidPathForDescendantsQuery(component), rowBounds);
  }

  /**
   * Count the descendants of a base component, given by its UUID. The components that are not present in last
   * analysis are ignored.
   *
   * Zero is returned if the base component does not exist or if the base component is a leaf.
   */
  public int countDescendants(DbSession dbSession, ComponentTreeQuery query) {
    Optional<ComponentDto> componentOpt = selectByUuid(dbSession, query.getBaseUuid());
    if (!componentOpt.isPresent()) {
      return 0;
    }
    ComponentDto component = componentOpt.get();
    return mapper(dbSession).countDescendants(query, uuidPathForDescendantsQuery(component));
  }

  private static String uuidPathForDescendantsQuery(ComponentDto component) {
    return DatabaseUtils.buildLikeValue(component.getUuidPath() + component.uuid() + ".", WildcardPosition.AFTER);
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

  public List<UuidWithProjectUuidDto> selectAllViewsAndSubViews(DbSession session) {
    return mapper(session).selectUuidsForQualifiers(Qualifiers.VIEW, Qualifiers.SUBVIEW);
  }

  public List<String> selectProjectsFromView(DbSession session, String viewUuid, String projectViewUuid) {
    return mapper(session).selectProjectsFromView("%." + viewUuid + ".%", projectViewUuid);
  }

  /**
   * Returns all projects (Scope {@link org.sonar.api.resources.Scopes#PROJECT} and qualifier
   * {@link org.sonar.api.resources.Qualifiers#PROJECT}) which are enabled.
   *
   * Used by Views.
   */
  public List<ComponentDto> selectProjects(DbSession session) {
    return mapper(session).selectProjects();
  }

  public List<ComponentDto> selectProvisionedProjects(DbSession session, int offset, int limit, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addProjectQualifier(parameters);
    addPartialQueryParameterIfNotNull(parameters, query);

    return mapper(session).selectProvisionedProjects(parameters, new RowBounds(offset, limit));
  }

  public int countProvisionedProjects(DbSession session, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addProjectQualifier(parameters);
    addPartialQueryParameterIfNotNull(parameters, query);

    return mapper(session).countProvisionedProjects(parameters);
  }

  public List<ComponentDto> selectGhostProjects(DbSession session, int offset, int limit, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addProjectQualifier(parameters);
    addPartialQueryParameterIfNotNull(parameters, query);

    return mapper(session).selectGhostProjects(parameters, new RowBounds(offset, limit));
  }

  public long countGhostProjects(DbSession session, @Nullable String query) {
    Map<String, Object> parameters = newHashMapWithExpectedSize(2);
    addProjectQualifier(parameters);
    addPartialQueryParameterIfNotNull(parameters, query);

    return mapper(session).countGhostProjects(parameters);
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

  private static void addPartialQueryParameterIfNotNull(Map<String, Object> parameters, @Nullable String keyOrNameFilter) {
    // TODO rely on resource_index table and match exactly the key
    if (keyOrNameFilter != null) {
      parameters.put("query", "%" + keyOrNameFilter.toUpperCase(Locale.ENGLISH) + "%");
    }
  }

  private static void addProjectQualifier(Map<String, Object> parameters) {
    parameters.put("qualifier", Qualifiers.PROJECT);
  }

  public void insert(DbSession session, ComponentDto item) {
    mapper(session).insert(item);
  }

  public void insertBatch(DbSession session, ComponentDto item) {
    mapper(session).insertBatch(item);
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

  public void updateBEnabledToFalse(DbSession session, Collection<String> uuids) {
    executeLargeUpdates(uuids, mapper(session)::updateBEnabledToFalse);
  }

  public void applyBChangesForRootComponentUuid(DbSession session, String projectUuid) {
    mapper(session).applyBChangesForRootComponentUuid(projectUuid);
  }

  public void resetBChangedForRootComponentUuid(DbSession session, String projectUuid) {
    mapper(session).resetBChangedForRootComponentUuid(projectUuid);
  }

  public void delete(DbSession session, long componentId) {
    mapper(session).delete(componentId);
  }

  private static ComponentMapper mapper(DbSession session) {
    return session.getMapper(ComponentMapper.class);
  }

}
