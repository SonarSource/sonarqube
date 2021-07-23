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

import javax.annotation.Nullable;

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

  void addProperty(DbSession dbSession, NewValue newValue, boolean isUserProperty);

  void updateProperty(DbSession dbSession, NewValue newValue, boolean isUserProperty);

  void deleteProperty(DbSession dbSession, NewValue newValue, boolean isUserProperty);

  void addUserToken(DbSession dbSession, NewValue newValue);

  void updateUserToken(DbSession dbSession, NewValue newValue);

  void deleteUserToken(DbSession dbSession, NewValue newValue);

  void addGroupPermission(DbSession dbSession, NewValue newValue);

  void deleteGroupPermission(DbSession dbSession, NewValue newValue);

  void addUserPermission(DbSession dbSession, NewValue newValue);

  void deleteUserPermission(DbSession dbSession, NewValue newValue);

  void addPermissionTemplate(DbSession dbSession, NewValue newValue);

  void updatePermissionTemplate(DbSession dbSession, NewValue newValue);

  void deletePermissionTemplate(DbSession dbSession, NewValue newValue);

  void addUserToPermissionTemplate(DbSession dbSession, NewValue newValue);

  void deleteUserFromPermissionTemplate(DbSession dbSession, NewValue newValue);

  void addGroupToPermissionTemplate(DbSession dbSession, NewValue newValue);

  void deleteGroupFromPermissionTemplate(DbSession dbSession, NewValue newValue);

  void addCharacteristicToPermissionTemplate(DbSession dbSession, NewValue newValue);

  void updateCharacteristicInPermissionTemplate(DbSession dbSession, NewValue newValue);

  void addPlugin(DbSession dbSession, NewValue newValue);

  void updatePlugin(DbSession dbSession, NewValue newValue);

  void generateSecretKey(DbSession dbSession);

  void setLicense(DbSession dbSession, boolean isSet, NewValue newValue);

  void addWebhook(DbSession dbSession, NewValue newValue);

  void updateWebhook(DbSession dbSession, NewValue newValue);

  void deleteWebhook(DbSession dbSession, NewValue newValue);

  void addDevOpsPlatformSetting(DbSession dbSession, NewValue newValue);

  void updateDevOpsPlatformSetting(DbSession dbSession, NewValue newValue);

  void deleteDevOpsPlatformSetting(DbSession dbSession, NewValue newValue);

  void addPersonalAccessToken(DbSession dbSession, NewValue newValue);

  void updatePersonalAccessToken(DbSession dbSession, NewValue newValue);

  void deletePersonalAccessToken(DbSession dbSession, NewValue newValue);

  boolean isTrackedProperty(String propertyKey);

  void addComponent(DbSession dbSession, NewValue newValue, String qualifier);

  void deleteComponent(DbSession dbSession, NewValue newValue, @Nullable String qualifier);

  void updateComponent(DbSession dbSession, NewValue newValue, String qualifier);

  void setPrivateForComponentUuid(DbSession session, NewValue componentNewValue, @Nullable String qualifier);

  void updateComponentVisibility(DbSession session, NewValue projectNewValue, String qualifier);

  void componentKeyUpdate(DbSession session, NewValue componentKeyNewValue, String qualifier);

  void componentKeyBranchUpdate(DbSession session, NewValue componentKeyNewValue, String qualifier);

}
