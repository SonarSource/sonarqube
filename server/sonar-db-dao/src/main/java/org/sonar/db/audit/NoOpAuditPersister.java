/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import jakarta.annotation.Priority;
import org.sonar.db.DbSession;
import org.sonar.db.audit.model.ComponentKeyNewValue;
import org.sonar.db.audit.model.ComponentNewValue;
import org.sonar.db.audit.model.DevOpsPlatformSettingNewValue;
import org.sonar.db.audit.model.AbstractEditorNewValue;
import org.sonar.db.audit.model.DevOpsPermissionsMappingNewValue;
import org.sonar.db.audit.model.GroupPermissionNewValue;
import org.sonar.db.audit.model.JiraOrganizationBindingNewValue;
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

@Priority(2)
public class NoOpAuditPersister implements AuditPersister {
  @Override
  public void addUserGroup(DbSession dbSession, UserGroupNewValue newValue) {
    // no op
  }

  @Override
  public void updateUserGroup(DbSession dbSession, UserGroupNewValue newValue) {
    // no op
  }

  @Override
  public void deleteUserGroup(DbSession dbSession, UserGroupNewValue newValue) {
    // no op
  }

  @Override
  public void addUser(DbSession dbSession, UserNewValue newValue) {
    // no op
  }

  @Override
  public void updateUser(DbSession dbSession, UserNewValue newValue) {
    // no op
  }

  @Override
  public void updateUserPassword(DbSession dbSession, SecretNewValue newValue) {
    // no op
  }

  @Override
  public void updateWebhookSecret(DbSession dbSession, SecretNewValue newValue) {
    // no op
  }

  @Override
  public void updateDevOpsPlatformSecret(DbSession dbSession, SecretNewValue newValue) {
    // no op
  }

  @Override
  public void deactivateUser(DbSession dbSession, UserNewValue newValue) {
    // no op
  }

  @Override
  public void addUserToGroup(DbSession dbSession, UserGroupNewValue newValue) {
    // no op
  }

  @Override
  public void deleteUserFromGroup(DbSession dbSession, UserGroupNewValue newValue) {
    // no op
  }

  @Override
  public void addProperty(DbSession dbSession, PropertyNewValue newValue, boolean isUserProperty) {
    // no op
  }

  @Override
  public void updateProperty(DbSession dbSession, PropertyNewValue newValue, boolean isUserProperty) {
    // no op
  }

  @Override
  public void deleteProperty(DbSession dbSession, PropertyNewValue newValue, boolean isUserProperty) {
    // no op
  }

  @Override
  public void addUserToken(DbSession dbSession, UserTokenNewValue newValue) {
    // no op
  }

  @Override
  public void addProjectBadgeToken(DbSession dbSession, ProjectBadgeTokenNewValue newValue) {
    // no op
  }

  @Override
  public void updateProjectBadgeToken(DbSession session, ProjectBadgeTokenNewValue projectBadgeTokenNewValue) {
    // no op
  }

  @Override
  public void updateUserToken(DbSession dbSession, UserTokenNewValue newValue) {
    // no op
  }

  @Override
  public void deleteUserToken(DbSession dbSession, UserTokenNewValue newValue) {
    // no op
  }

  @Override
  public void addGroupPermission(DbSession dbSession, GroupPermissionNewValue newValue) {
    // no op
  }

  @Override
  public void deleteGroupPermission(DbSession dbSession, GroupPermissionNewValue newValue) {
    // no op
  }

  @Override
  public void addUserPermission(DbSession dbSession, UserPermissionNewValue newValue) {
    // no op
  }

  @Override
  public void deleteUserPermission(DbSession dbSession, UserPermissionNewValue newValue) {
    // no op
  }

  @Override
  public void addPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue) {
    // no op
  }

  @Override
  public void updatePermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue) {
    // no op
  }

  @Override
  public void deletePermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue) {
    // no op
  }

  @Override
  public void addUserToPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue) {
    // no op
  }

  @Override
  public void deleteUserFromPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue) {
    // no op
  }

  @Override
  public void addGroupToPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue) {
    // no op
  }

  @Override
  public void deleteGroupFromPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue) {
    // no op
  }

  @Override
  public void addDevOpsPermissionsMapping(DbSession dbSession, DevOpsPermissionsMappingNewValue newValue) {
    // no op
  }

  @Override
  public void deleteDevOpsPermissionsMapping(DbSession dbSession, DevOpsPermissionsMappingNewValue deletedValue) {
    // no op
  }

  @Override
  public void addQualityGateEditor(DbSession dbSession, AbstractEditorNewValue newValue) {
    // no op
  }

  @Override
  public void deleteQualityGateEditor(DbSession dbSession, AbstractEditorNewValue newValue) {
    // no op
  }

  @Override
  public void addQualityProfileEditor(DbSession dbSession, AbstractEditorNewValue newValue) {
    // no op
  }

  @Override
  public void deleteQualityProfileEditor(DbSession dbSession, AbstractEditorNewValue newValue) {
    // no op
  }

  @Override
  public void addCharacteristicToPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue) {
    // no op
  }

  @Override
  public void updateCharacteristicInPermissionTemplate(DbSession dbSession, PermissionTemplateNewValue newValue) {
    // no op
  }

  @Override
  public void addPlugin(DbSession dbSession, PluginNewValue newValue) {
    // no op
  }

  @Override
  public void updatePlugin(DbSession dbSession, PluginNewValue newValue) {
    // no op
  }

  @Override
  public void generateSecretKey(DbSession dbSession) {
    // no op
  }

  @Override
  public void setLicense(DbSession dbSession, boolean isSet, LicenseNewValue newValue) {
    // no op
  }

  @Override
  public void addWebhook(DbSession dbSession, WebhookNewValue newValue) {
    // no op
  }

  @Override
  public void updateWebhook(DbSession dbSession, WebhookNewValue newValue) {
    // no op
  }

  @Override
  public void deleteWebhook(DbSession dbSession, WebhookNewValue newValue) {
    // no op
  }

  @Override
  public void addDevOpsPlatformSetting(DbSession dbSession, DevOpsPlatformSettingNewValue newValue) {
    // no op
  }

  @Override
  public void updateDevOpsPlatformSetting(DbSession dbSession, DevOpsPlatformSettingNewValue newValue) {
    // no op
  }

  @Override
  public void deleteDevOpsPlatformSetting(DbSession dbSession, DevOpsPlatformSettingNewValue newValue) {
    // no op
  }

  @Override
  public void addPersonalAccessToken(DbSession dbSession, PersonalAccessTokenNewValue newValue) {
    // no op
  }

  @Override
  public void updatePersonalAccessToken(DbSession dbSession, PersonalAccessTokenNewValue newValue) {
    // no op
  }

  @Override
  public void deletePersonalAccessToken(DbSession dbSession, PersonalAccessTokenNewValue newValue) {
    // no op
  }

  @Override
  public boolean isTrackedProperty(String propertyKey) {
    return false;
  }

  @Override
  public void addComponent(DbSession dbSession, ComponentNewValue newValue) {
    // no op
  }

  @Override
  public void deleteComponent(DbSession dbSession, ComponentNewValue newValue) {
    // no op
  }

  @Override
  public void updateComponent(DbSession dbSession, ComponentNewValue newValue) {
    // no op
  }

  @Override
  public void updateComponentVisibility(DbSession session, ComponentNewValue componentNewValue) {
    // no op
  }

  @Override
  public void componentKeyUpdate(DbSession session, ComponentKeyNewValue componentKeyNewValue, String qualifier) {
    // no op
  }

  @Override
  public void componentKeyBranchUpdate(DbSession session, ComponentKeyNewValue componentKeyNewValue, String qualifier) {
    // no op
  }

  @Override
  public void validateDevOpsPlatformSettingSuccess(DbSession dbSession, DevOpsPlatformSettingNewValue newValue) {
    // no op
  }

  @Override
  public void validateDevOpsPlatformSettingFailure(DbSession dbSession, DevOpsPlatformSettingNewValue newValue) {
    // no op
  }

  @Override
  public void jiraOrganizationBindingAdd(DbSession dbSession, JiraOrganizationBindingNewValue newValue) {
    // no op
  }

  @Override
  public void jiraOrganizationBindingReauthorize(DbSession dbSession, JiraOrganizationBindingNewValue newValue) {
    // no op
  }

  @Override
  public void jiraOrganizationBindingUpdate(DbSession dbSession, JiraOrganizationBindingNewValue newValue) {
    // no op
  }

  @Override
  public void jiraOrganizationBindingDelete(DbSession dbSession, JiraOrganizationBindingNewValue newValue) {
    // no op
  }
}
