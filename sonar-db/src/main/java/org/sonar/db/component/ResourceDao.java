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
package org.sonar.db.component;

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.utils.System2;
import org.sonar.db.AbstractDao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class ResourceDao extends AbstractDao {

  public ResourceDao(MyBatis myBatis, System2 system2) {
    super(myBatis, system2);
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
      return selectResource(componentUuid, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  private static ResourceDto selectResource(String componentUuid, SqlSession session) {
    return session.getMapper(ResourceMapper.class).selectResourceByUuid(componentUuid);
  }

  public void updateAuthorizationDate(Long projectId, SqlSession session) {
    session.getMapper(ResourceMapper.class).updateAuthorizationDate(projectId, now());
  }

  /**
   * Return the root project of a component.
   * Will return the component itself if it's already the root project
   * Can return null if the component does not exists.
   *
   * The implementation should rather use a new column already containing the root project, see https://jira.sonarsource.com/browse/SONAR-5188.
   */
  @CheckForNull
  private static ResourceDto getRootProjectByComponentKey(DbSession session, String componentKey) {
    ResourceDto component = selectResource(ResourceQuery.create().setKey(componentKey), session);
    if (component != null) {
      String rootUuid = component.getRootUuid();
      if (rootUuid.equals(component.getUuid())) {
        return component;
      } else {
        return getParentModuleByComponentUuid(rootUuid, session);
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
  private static ResourceDto getParentModuleByComponentUuid(String componentUUid, DbSession session) {
    ResourceDto component = selectResource(componentUUid, session);
    if (component != null) {
      String rootUuid = component.getRootUuid();
      if (rootUuid.equals(component.getUuid())) {
        return component;
      } else {
        return getParentModuleByComponentUuid(rootUuid, session);
      }
    }
    return null;
  }

  /**
   * Return provisioned project with given key
   */
  public ResourceDto selectProvisionedProject(DbSession session, String key) {
    return session.getMapper(ResourceMapper.class).selectProvisionedProject(key);
  }

}
