/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.audit;

import org.sonar.core.extension.PlatformLevel;
import org.sonar.db.DbSession;
import org.sonar.db.audit.model.NewValue;

@PlatformLevel(1)
public interface AuditPersister {

  void addUserGroup(DbSession dbSession, NewValue newValue);

  void updateUserGroup(DbSession dbSession, NewValue newValue);

  void deleteUserGroup(DbSession dbSession, NewValue newValue);

  void addUser(DbSession dbSession, NewValue newValue);

  void updateUser(DbSession dbSession, NewValue newValue);

  void deactivateUser(DbSession dbSession, NewValue newValue);

  void addUserToGroup(DbSession dbSession, NewValue newValue);

  void deleteUserFromGroup(DbSession dbSession, NewValue newValue);

  void addUserProperty(DbSession dbSession, NewValue newValue);

  void updateUserProperty(DbSession dbSession, NewValue newValue);

  void deleteUserProperty(DbSession dbSession, NewValue newValue);

  void addUserToken(DbSession dbSession, NewValue newValue);

  void updateUserToken(DbSession dbSession, NewValue newValue);

  void deleteUserToken(DbSession dbSession, NewValue newValue);

  boolean isTrackedProperty(String propertyKey);
}
