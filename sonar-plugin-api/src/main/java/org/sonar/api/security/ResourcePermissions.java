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
package org.sonar.api.security;

import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.resources.Resource;

/**
 * Grant access to newly created projects.
 * <p/>
 * <p>This component is not supposed to be called by standard plugins.</p>
 *
 * @since 3.2
 */
@BatchSide
@ServerSide
public interface ResourcePermissions {

  /**
   * Limitation - the resource id is used instead of logical key.
   */
  boolean hasRoles(Resource resource);

  /**
   * Limitation - the resource id is used instead of logical key.
   * Important note : the existing roles are overridden by default ones, so it's recommended
   * to check that {@link ResourcePermissions#hasRoles(org.sonar.api.resources.Resource)} is
   * false before executing this method.
   */
  void grantDefaultRoles(Resource resource);

  /**
   * Limitation - the resource id is used instead of logical key.
   */
  void grantUserRole(Resource resource, String login, String role);

  /**
   * Limitation - the resource id is used instead of logical key.
   */
  void grantGroupRole(Resource resource, String groupName, String role);
}
