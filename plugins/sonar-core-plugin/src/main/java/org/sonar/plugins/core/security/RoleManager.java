/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.security;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.security.GroupRole;
import org.sonar.api.security.UserRole;

import javax.persistence.Query;
import java.util.List;

/**
 * @since 1.12
 */
public class RoleManager {

  protected static final String DEFAULT_ROLE_PREFIX = "default-";
  private DatabaseSession session;

  public RoleManager(DatabaseSession session) {
    this.session = session;
  }

  public void affectDefaultRolesToResource(int resourceId) {
    for (UserRole defaultRole : getDefaultUserRoles()) {
      session.save(createResourceRoleFromDefault(defaultRole, resourceId));
    }
    for (GroupRole defaultRole : getDefaultGroupRoles()) {
      session.save(createResourceRoleFromDefault(defaultRole, resourceId));
    }
    session.commit();
  }

  public List<UserRole> getUserRoles(int resourceId) {
    return session.getResults(UserRole.class, "resourceId", resourceId);
  }

  public List<GroupRole> getGroupRoles(int resourceId) {
    return session.getResults(GroupRole.class, "resourceId", resourceId);
  }

  protected List<UserRole> getDefaultUserRoles() {
    final Query query = session.createQuery("from " + UserRole.class.getSimpleName() + " ur where ur.resourceId is null and ur.role like '" + DEFAULT_ROLE_PREFIX + "%'");
    return query.getResultList();
  }

  protected List<GroupRole> getDefaultGroupRoles() {
    final Query query = session.createQuery("from " + GroupRole.class.getSimpleName() + " gr where gr.resourceId is null and gr.role like '" + DEFAULT_ROLE_PREFIX + "%'");
    return query.getResultList();
  }

  protected UserRole createResourceRoleFromDefault(UserRole defaultUserRole, int resourceId) {
    final UserRole result = new UserRole();
    result.setRole(convertDefaultRoleName(defaultUserRole.getRole()));
    result.setResourceId(resourceId);
    result.setUserId(defaultUserRole.getUserId());
    return result;
  }

  protected GroupRole createResourceRoleFromDefault(GroupRole defaultUserRole, int resourceId) {
    final GroupRole result = new GroupRole();
    result.setRole(convertDefaultRoleName(defaultUserRole.getRole()));
    result.setResourceId(resourceId);
    result.setGroupId(defaultUserRole.getGroupId());
    return result;
  }

  protected static String convertDefaultRoleName(String defaultRoleName) {
    return StringUtils.substringAfter(defaultRoleName, DEFAULT_ROLE_PREFIX);
  }
}
