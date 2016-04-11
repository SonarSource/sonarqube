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
package org.sonar.api.security;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.resources.Resource;

/**
 * Grant access to newly created projects.
 * <br>
 * <p>This component is not supposed to be called by standard plugins.
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
