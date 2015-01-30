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
package org.sonar.core.resource;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ResourceDao implements DaoComponent {
  private MyBatis mybatis;
  private System2 system2;

  public ResourceDao(MyBatis mybatis, System2 system2) {
    this.mybatis = mybatis;
    this.system2 = system2;
  }

  public List<ResourceDto> getResources(ResourceQuery query) {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ResourceMapper.class).selectResources(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ResourceDto> getResources(ResourceQuery query, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectResources(query);
  }

  /**
   * Return a single result or null. If the request returns multiple rows, then
   * the first row is returned.
   */
  @CheckForNull
  public ResourceDto getResource(ResourceQuery query) {
    DbSession session = mybatis.openSession(false);
    try {
      return getResource(query, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public ResourceDto getResource(ResourceQuery query, DbSession session) {
    List<ResourceDto> resources = getResources(query, session);
    if (!resources.isEmpty()) {
      return resources.get(0);
    }
    return null;
  }

  public List<Long> getResourceIds(ResourceQuery query) {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ResourceMapper.class).selectResourceIds(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ResourceDto getResource(long projectId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return getResource(projectId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public ResourceDto getResource(String componentUuid) {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ResourceMapper.class).selectResourceByUuid(componentUuid);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ResourceDto getResource(long projectId, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectResource(projectId);
  }

  @CheckForNull
  public SnapshotDto getLastSnapshot(String resourceKey, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectLastSnapshotByResourceKey(resourceKey);
  }

  @CheckForNull
  public SnapshotDto getLastSnapshotByResourceUuid(String componentUuid, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectLastSnapshotByResourceUuid(componentUuid);
  }

  public List<ResourceDto> getDescendantProjects(long projectId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return getDescendantProjects(projectId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ResourceDto> getDescendantProjects(long projectId, SqlSession session) {
    ResourceMapper mapper = session.getMapper(ResourceMapper.class);
    List<ResourceDto> resources = newArrayList();
    appendChildProjects(projectId, mapper, resources);
    return resources;
  }

  private void appendChildProjects(long projectId, ResourceMapper mapper, List<ResourceDto> resources) {
    List<ResourceDto> subProjects = mapper.selectDescendantProjects(projectId);
    for (ResourceDto subProject : subProjects) {
      resources.add(subProject);
      appendChildProjects(subProject.getId(), mapper, resources);
    }
  }

  /**
   * Used by the Views Plugin
   */
  public ResourceDao insertOrUpdate(ResourceDto... resources) {
    SqlSession session = mybatis.openSession(false);
    ResourceMapper mapper = session.getMapper(ResourceMapper.class);
    Date now = new Date(system2.now());
    try {
      for (ResourceDto resource : resources) {
        if (resource.getId() == null) {
          // Fix for Views
          if (resource.getUuid() == null && Scopes.PROJECT.equals(resource.getScope())) {
            String uuid = Uuids.create();
            resource.setUuid(uuid);
            resource.setProjectUuid(uuid);
            resource.setModuleUuidPath("");
          }
          resource.setCreatedAt(now);
          resource.setAuthorizationUpdatedAt(now.getTime());
          mapper.insert(resource);
        } else {
          mapper.update(resource);
        }
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
    return this;
  }

  /**
   * Should not be called from batch side (used to reindex permission in E/S)
   */
  public void updateAuthorizationDate(Long projectId, SqlSession session) {
    session.getMapper(ResourceMapper.class).updateAuthorizationDate(projectId, system2.now());
  }

  @CheckForNull
  public Component findByKey(String key) {
    ResourceDto resourceDto = getResource(ResourceQuery.create().setKey(key));
    return resourceDto != null ? toComponent(resourceDto) : null;
  }

  @CheckForNull
  public Component findById(Long id, SqlSession session) {
    ResourceDto resourceDto = getResource(id, session);
    return resourceDto != null ? toComponent(resourceDto) : null;
  }

  /**
   * Return the root project of a component.
   * Will return the component itself if it's already the root project
   * Can return null if the component does not exists.
   *
   * The implementation should rather use a new column already containing the root project, see https://jira.codehaus.org/browse/SONAR-5188.
   */
  @CheckForNull
  public ResourceDto getRootProjectByComponentKey(DbSession session, String componentKey) {
    ResourceDto component = getResource(ResourceQuery.create().setKey(componentKey), session);
    if (component != null) {
      Long rootId = component.getRootId();
      if (rootId != null) {
        return getParentModuleByComponentId(rootId, session);
      } else {
        return component;
      }
    }
    return null;
  }

  @CheckForNull
  public ResourceDto getRootProjectByComponentKey(String componentKey) {
    DbSession session = mybatis.openSession(false);
    try {
      return getRootProjectByComponentKey(session, componentKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  ResourceDto getParentModuleByComponentId(Long componentId, DbSession session) {
    ResourceDto component = getResource(componentId, session);
    if (component != null) {
      Long rootId = component.getRootId();
      if (rootId != null) {
        return getParentModuleByComponentId(rootId, session);
      } else {
        return component;
      }
    }
    return null;
  }

  /**
   * Return the root project of a component.
   * Will return the component itself if it's already the root project
   * Can return null if the component that does exists.
   *
   * The implementation should rather use a new column already containing the root project, see https://jira.codehaus.org/browse/SONAR-5188.
   */
  @CheckForNull
  public ResourceDto getRootProjectByComponentId(long componentId) {
    DbSession session = mybatis.openSession(false);
    try {
      ResourceDto component = getParentModuleByComponentId(componentId, session);
      Long rootId = component != null ? component.getRootId() : null;
      if (rootId != null) {
        return getParentModuleByComponentId(rootId, session);
      } else {
        return component;
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<Component> selectProjectsByQualifiers(Collection<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return Collections.emptyList();
    }
    SqlSession session = mybatis.openSession(false);
    try {
      return toComponents(session.getMapper(ResourceMapper.class).selectProjectsByQualifiers(qualifiers));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Return enabled projects including not completed ones, ie without snapshots or without snapshot having islast=true
   */
  public List<Component> selectProjectsIncludingNotCompletedOnesByQualifiers(Collection<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return Collections.emptyList();
    }
    SqlSession session = mybatis.openSession(false);
    try {
      return toComponents(session.getMapper(ResourceMapper.class).selectProjectsIncludingNotCompletedOnesByQualifiers(qualifiers));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Return ghosts projects :
   * - not enabled projects
   * - enabled projects without snapshot having islast=true
   * - enabled projects without snapshot
   */
  public List<Component> selectGhostsProjects(Collection<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return Collections.emptyList();
    }
    SqlSession session = mybatis.openSession(false);
    try {
      return toComponents(session.getMapper(ResourceMapper.class).selectGhostsProjects(qualifiers));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Return provisioned projects = enabled projects without snapshot
   */
  public List<ResourceDto> selectProvisionedProjects(Collection<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return Collections.emptyList();
    }
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ResourceMapper.class).selectProvisionedProjects(qualifiers);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Return provisioned project with given key
   */
  public ResourceDto selectProvisionedProject(DbSession session, String key) {
    return session.getMapper(ResourceMapper.class).selectProvisionedProject(key);
  }

  public ResourceDto selectProvisionedProject(String key) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectProvisionedProject(session, key);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public static ComponentDto toComponent(ResourceDto resourceDto) {
    return new ComponentDto()
      .setId(resourceDto.getId())
      .setKey(resourceDto.getKey())
      .setPath(resourceDto.getPath())
      .setLongName(resourceDto.getLongName())
      .setName(resourceDto.getName())
      .setQualifier(resourceDto.getQualifier());
  }

  public static List<Component> toComponents(List<ResourceDto> resourceDto) {
    return newArrayList(Iterables.transform(resourceDto, new Function<ResourceDto, Component>() {
      @Override
      public Component apply(@Nullable ResourceDto resourceDto) {
        return resourceDto == null ? null : toComponent(resourceDto);
      }
    }));
  }

  public void insertUsingExistingSession(ResourceDto resourceDto, SqlSession session) {
    ResourceMapper resourceMapper = session.getMapper(ResourceMapper.class);
    resourceMapper.insert(resourceDto);
  }
}
