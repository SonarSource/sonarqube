/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.db.audit.model.AbstractEditorNewValue;
import org.sonar.db.audit.model.ComponentKeyNewValue;
import org.sonar.db.audit.model.ComponentNewValue;
import org.sonar.db.audit.model.DevOpsPlatformSettingNewValue;
import org.sonar.db.audit.model.DevOpsPermissionsMappingNewValue;
import org.sonar.db.audit.model.GroupPermissionNewValue;
import org.sonar.db.audit.model.LicenseNewValue;
import org.sonar.db.audit.model.PermissionTemplateNewValue;
import org.sonar.db.audit.model.PersonalAccessTokenNewValue;
import org.sonar.db.audit.model.PluginNewValue;
import org.sonar.db.audit.model.ProjectBadgeTokenNewValue;
import org.sonar.db.audit.model.PropertyNewValue;
import org.sonar.db.audit.model.SecretNewValue;
import org.sonar.db.audit.model.UserGroupNewValue;
import org.sonar.db.audit.model.UserNewValue;
import org.sonar.db.audit.model.UserPermissionNewValue;
import org.sonar.db.audit.model.UserTokenNewValue;
import org.sonar.db.audit.model.WebhookNewValue;

@PlatformLevel(1)
public interface AuditPersister {

  void addUserGroup(DbSession dbSession, UserGroupNewValue newValue);

  void updateUserGroup(DbSession dbSession, UserGroupNewValue newValue);

  void deleteUserGroup(DbSession dbSession, UserGroupNewValue newValue);

  void addUser(DbSession dbSession, UserNewValue newValue);

  void updateUser(DbSession dbSession, UserNewValue newValue);

  void updateUserPassword(DbSession dbSession, SecretNewValue newValue);

  void updateWebhookSecret(DbSession dbSession, SecretNewValue newValue);

  void updateDevOpsPlatformSecret(DbSession dbSession, SecretNewValue newValue);

  void deactivateUser(DbSession dbSession, UserNewValue newValue);

  void addUserToGroup(DbSession dbSession, UserGroupNewValue newValue);

  void deleteUserFromGroup(DbSession dbSession, UserGroupNewValue newValue);

  void addProperty(DbSession dbSession, PropertyNewValue newValue, boolean isUserProperty);

  void updateProperty(DbSession dbSession, PropertyNewValue newValue, boolean isUserProperty);

  void deleteProperty(DbSession dbSession, PropertyNewValue newValue, boolean isUserProperty);

  void addUserToken(DbSession dbSession, UserTokenNewValue newValue);

  void addProjectBadgeToken(DbSession dbSession, ProjectBadgeTokenNewValue newValue);

  void updateProjectBadgeToken(DbSession session, ProjectBadgeTokenNewValue projectBadgeTokenNewValue);

  void updateUserToken(DbSession dbSession, UserTokenNewValue newValue);

  void deleteUserToken(DbSession dbSession, UserTokenNewValue newValue);

  void addGroupPermission(DbSession dbSession, GroupPermissionNewValue newValue);

  void deleteGroupPermission(DbSession dbSession, GroupPermissionNewValue newValue);

  void addUserPermission(DbSession dbSession, UserPermissionNewValue newValue);

  void deleteUserPermission(DbSession dbSession, UserPermissionNewValue newValue);

  void addPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue);

  void updatePermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue);

  void deletePermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue);

  void addUserToPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue);

  void deleteUserFromPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue);

  void addGroupToPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue);

  void deleteGroupFromPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue);

  void addDevOpsPermissionsMapping(DbSession dbSession, DevOpsPermissionsMappingNewValue newValue);

  void deleteDevOpsPermissionsMapping(DbSession dbSession, DevOpsPermissionsMappingNewValue deletedValue);

  void addQualityGateEditor(DbSession dbSession, AbstractEditorNewValue newValue);

  void deleteQualityGateEditor(DbSession dbSession, AbstractEditorNewValue newValue);

  void addQualityProfileEditor(DbSession dbSession, AbstractEditorNewValue newValue);

  void deleteQualityProfileEditor(DbSession dbSession, AbstractEditorNewValue newValue);

  void addCharacteristicToPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue);

  void updateCharacteristicInPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue);

  void addPlugin(DbSession dbSession, PluginNewValue newValue);

  void updatePlugin(DbSession dbSession, PluginNewValue newValue);

  void generateSecretKey(DbSession dbSession);

  void setLicense(DbSession dbSession, boolean isSet, LicenseNewValue newValue);

  void addWebhook(DbSession dbSession, WebhookNewValue newValue);

  void updateWebhook(DbSession dbSession, WebhookNewValue newValue);

  void deleteWebhook(DbSession dbSession, WebhookNewValue newValue);

  void addDevOpsPlatformSetting(DbSession dbSession, DevOpsPlatformSettingNewValue newValue);

  void updateDevOpsPlatformSetting(DbSession dbSession, DevOpsPlatformSettingNewValue newValue);

  void deleteDevOpsPlatformSetting(DbSession dbSession, DevOpsPlatformSettingNewValue newValue);

  void addPersonalAccessToken(DbSession dbSession, PersonalAccessTokenNewValue newValue);

  void updatePersonalAccessToken(DbSession dbSession, PersonalAccessTokenNewValue newValue);

  void deletePersonalAccessToken(DbSession dbSession, PersonalAccessTokenNewValue newValue);

  boolean isTrackedProperty(String propertyKey);

  void addComponent(DbSession dbSession, ComponentNewValue newValue);

  void deleteComponent(DbSession dbSession, ComponentNewValue newValue);

  void updateComponent(DbSession dbSession, ComponentNewValue newValue);

  void updateComponentVisibility(DbSession session, ComponentNewValue componentNewValue);

  void componentKeyUpdate(DbSession session, ComponentKeyNewValue componentKeyNewValue, String qualifier);

  void componentKeyBranchUpdate(DbSession session, ComponentKeyNewValue componentKeyNewValue, String qualifier);

  void validateDevOpsPlatformSettingSuccess(DbSession dbSession, DevOpsPlatformSettingNewValue newValue);

  void validateDevOpsPlatformSettingFailure(DbSession dbSession, DevOpsPlatformSettingNewValue newValue);

}
