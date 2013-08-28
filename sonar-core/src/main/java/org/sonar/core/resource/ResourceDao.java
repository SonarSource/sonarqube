/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.component.Component;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ResourceDao {
  private MyBatis mybatis;

  public ResourceDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<ResourceDto> getResources(ResourceQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(ResourceMapper.class).selectResources(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Return a single result or null. If the request returns multiple rows, then
   * the first row is returned.
   */
  public ResourceDto getResource(ResourceQuery query) {
    List<ResourceDto> resources = getResources(query);
    if (!resources.isEmpty()) {
      return resources.get(0);
    }
    return null;
  }

  public List<Long> getResourceIds(ResourceQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(ResourceMapper.class).selectResourceIds(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ResourceDto getResource(long projectId) {
    SqlSession session = mybatis.openSession();
    try {
      return getResource(projectId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ResourceDto getResource(long projectId, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectResource(projectId);
  }

  public SnapshotDto getLastSnapshot(String resourceKey, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectLastSnapshotByResourceKey(resourceKey);
  }

  public SnapshotDto getLastSnapshotByResourceId(long resourceId, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectLastSnapshotByResourceId(resourceId);
  }

  public List<ResourceDto> getDescendantProjects(long projectId) {
    SqlSession session = mybatis.openSession();
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

  public ResourceDao insertOrUpdate(ResourceDto... resources) {
    SqlSession session = mybatis.openSession();
    ResourceMapper mapper = session.getMapper(ResourceMapper.class);
    try {
      for (ResourceDto resource : resources) {
        if (resource.getId() == null) {
          resource.setCreatedAt(new Date());
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

  public Collection<Component> findByIds(Collection<Long> ids) {
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }
    SqlSession session = mybatis.openSession();
    try {

      List<ResourceDto> resources = newArrayList();
      List <List<Long>> idsPartition = Lists.partition(newArrayList(ids), 1000);
      for (List<Long> partition : idsPartition) {
        List<ResourceDto> dtos = session.getMapper(ResourceMapper.class).selectResourcesById(partition);
        resources.addAll(dtos);
      }

      Collection<Component> components = newArrayList();
      for (ResourceDto resourceDto : resources) {
        components.add(toComponent(resourceDto));
      }
      return components;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public Component findByKey(String key) {
    ResourceDto resourceDto = getResource(ResourceQuery.create().setKey(key));
    return resourceDto != null ? toComponent(resourceDto) : null;
  }

  public List<Integer> findAuthorizedChildrenComponentIds(Collection<String> componentRootKeys, @Nullable Integer userId, String role){
    if (componentRootKeys.isEmpty()) {
      return Collections.emptyList();
    }
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(ResourceMapper.class).selectAuthorizedChildrenComponentIds(componentRootKeys, userId, role);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public ResourceDto getRootProjectByComponentKey(String componentKey) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(ResourceMapper.class).selectRootProjectByComponentKey(componentKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public ResourceDto getRootProjectByComponentId(Long componentId) {
    SqlSession session = mybatis.openSession();
    try {
      return session.getMapper(ResourceMapper.class).selectRootProjectByComponentId(componentId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<Component> selectProjectsByQualifiers(Collection<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return Collections.emptyList();
    }
    SqlSession session = mybatis.openSession();
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
    SqlSession session = mybatis.openSession();
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
    SqlSession session = mybatis.openSession();
    try {
      return toComponents(session.getMapper(ResourceMapper.class).selectGhostsProjects(qualifiers));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public static ComponentDto toComponent(ResourceDto resourceDto){
    return new ComponentDto()
      .setId(resourceDto.getId())
      .setKey(resourceDto.getKey())
      .setLongName(resourceDto.getLongName())
      .setName(resourceDto.getName())
      .setQualifier(resourceDto.getQualifier());
  }

  public static List<Component> toComponents(List<ResourceDto> resourceDto){
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
