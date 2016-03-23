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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.component.Component;
import org.sonar.api.utils.System2;
import org.sonar.db.AbstractDao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

import static com.google.common.collect.Lists.newArrayList;

public class ResourceDao extends AbstractDao {

  public ResourceDao(MyBatis myBatis, System2 system2) {
    super(myBatis, system2);
  }

  /**
   * Return a single result or null. If the request returns multiple rows, then
   * the first row is returned.
   */
  @CheckForNull
  public ResourceDto selectResource(ResourceQuery query) {
    DbSession session = myBatis().openSession(false);
    try {
      return selectResource(query, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  private static ResourceDto selectResource(ResourceQuery query, DbSession session) {
    List<ResourceDto> resources = getResources(query, session);
    if (!resources.isEmpty()) {
      return resources.get(0);
    }
    return null;
  }

  private static List<ResourceDto> getResources(ResourceQuery query, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectResources(query);
  }

  @CheckForNull
  public ResourceDto selectResource(String componentUuid) {
    SqlSession session = myBatis().openSession(false);
    try {
      return session.getMapper(ResourceMapper.class).selectResourceByUuid(componentUuid);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ResourceDto selectResource(long projectId, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectResource(projectId);
  }

  @CheckForNull
  public SnapshotDto getLastSnapshot(String resourceKey, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectLastSnapshotByResourceKey(resourceKey);
  }

  public List<ResourceDto> selectWholeTreeForRootId(SqlSession session, long rootId, String scope) {
    return session.getMapper(ResourceMapper.class).selectWholeTreeForRootId(rootId, scope);
  }

  public void updateAuthorizationDate(Long projectId, SqlSession session) {
    session.getMapper(ResourceMapper.class).updateAuthorizationDate(projectId, now());
  }

  @CheckForNull
  public Component selectByKey(String key) {
    ResourceDto resourceDto = selectResource(ResourceQuery.create().setKey(key));
    return resourceDto != null ? toComponent(resourceDto) : null;
  }

  /**
   * Return the root project of a component.
   * Will return the component itself if it's already the root project
   * Can return null if the component does not exists.
   *
   * The implementation should rather use a new column already containing the root project, see https://jira.sonarsource.com/browse/SONAR-5188.
   */
  @CheckForNull
  private ResourceDto getRootProjectByComponentKey(DbSession session, String componentKey) {
    ResourceDto component = selectResource(ResourceQuery.create().setKey(componentKey), session);
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
    DbSession session = myBatis().openSession(false);
    try {
      return getRootProjectByComponentKey(session, componentKey);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  private ResourceDto getParentModuleByComponentId(Long componentId, DbSession session) {
    ResourceDto component = selectResource(componentId, session);
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

  public List<Component> selectProjectsByQualifiers(Collection<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return Collections.emptyList();
    }
    SqlSession session = myBatis().openSession(false);
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
    SqlSession session = myBatis().openSession(false);
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
    SqlSession session = myBatis().openSession(false);
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
    SqlSession session = myBatis().openSession(false);
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
    return newArrayList(Iterables.transform(resourceDto, ToComponent.INSTANCE));
  }

  public void insertUsingExistingSession(ResourceDto resourceDto, SqlSession session) {
    ResourceMapper resourceMapper = session.getMapper(ResourceMapper.class);
    resourceMapper.insert(resourceDto);
  }

  private enum ToComponent implements Function<ResourceDto, Component> {
    INSTANCE;

    @Override
    public Component apply(@Nonnull ResourceDto resourceDto) {
      return toComponent(resourceDto);
    }
  }
}
