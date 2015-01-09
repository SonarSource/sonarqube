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

package org.sonar.server.component;

import org.sonar.api.ServerComponent;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.resource.ResourceIndexerDao;
import org.sonar.core.resource.ResourceKeyUpdaterDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ComponentService implements ServerComponent {

  private final DbClient dbClient;

  private final ResourceKeyUpdaterDao resourceKeyUpdaterDao;
  private final PreviewCache previewCache;
  private final I18n i18n;
  private final ResourceIndexerDao resourceIndexerDao;
  private final InternalPermissionService permissionService;

  public ComponentService(DbClient dbClient, ResourceKeyUpdaterDao resourceKeyUpdaterDao, PreviewCache previewCache, I18n i18n, ResourceIndexerDao resourceIndexerDao,
                          InternalPermissionService permissionService) {
    this.dbClient = dbClient;
    this.resourceKeyUpdaterDao = resourceKeyUpdaterDao;
    this.previewCache = previewCache;
    this.i18n = i18n;
    this.resourceIndexerDao = resourceIndexerDao;
    this.permissionService = permissionService;
  }

  public ComponentDto getByKey(String key) {
    DbSession session = dbClient.openSession(false);
    try {
      return getByKey(session, key);
    } finally {
      session.close();
    }
  }

  @CheckForNull
  public ComponentDto getNullableByKey(String key) {
    DbSession session = dbClient.openSession(false);
    try {
      return getNullableByKey(session, key);
    } finally {
      session.close();
    }
  }

  public ComponentDto getByUuid(String uuid) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getByUuid(session, uuid);
    } finally {
      session.close();
    }
  }

  @CheckForNull
  public ComponentDto getNullableByUuid(String uuid) {
    DbSession session = dbClient.openSession(false);
    try {
      return dbClient.componentDao().getNullableByUuid(session, uuid);
    } finally {
      session.close();
    }
  }

  public void updateKey(String projectOrModuleKey, String newKey) {
    UserSession.get().checkComponentPermission(UserRole.ADMIN, projectOrModuleKey);

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto projectOrModule = getByKey(session, projectOrModuleKey);
      resourceKeyUpdaterDao.updateKey(projectOrModule.getId(), newKey);
      session.commit();

      previewCache.reportResourceModification(newKey);

      session.commit();
    } finally {
      session.close();
    }
  }

  public Map<String, String> checkModuleKeysBeforeRenaming(String projectKey, String stringToReplace, String replacementString) {
    UserSession.get().checkProjectPermission(UserRole.ADMIN, projectKey);
    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto project = getByKey(projectKey);
      return resourceKeyUpdaterDao.checkModuleKeysBeforeRenaming(project.getId(), stringToReplace, replacementString);
    } finally {
      session.close();
    }
  }

  public void bulkUpdateKey(String projectKey, String stringToReplace, String replacementString) {
    UserSession.get().checkProjectPermission(UserRole.ADMIN, projectKey);

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto project = getByKey(session, projectKey);

      resourceKeyUpdaterDao.bulkUpdateKey(project.getId(), stringToReplace, replacementString);
      session.commit();

      ComponentDto newProject = dbClient.componentDao().getById(project.getId(), session);
      previewCache.reportResourceModification(newProject.key());

      session.commit();
    } finally {
      session.close();
    }
  }

  public String create(NewComponent newComponent) {
    UserSession.get().checkGlobalPermission(GlobalPermissions.PROVISIONING);

    DbSession session = dbClient.openSession(false);
    try {
      checkKeyFormat(newComponent.qualifier(), newComponent.key());
      checkBranchFormat(newComponent.qualifier(), newComponent.branch());
      String keyWithBranch = ComponentKeys.createKey(newComponent.key(), newComponent.branch());

      ComponentDto existingComponent = getNullableByKey(keyWithBranch);
      if (existingComponent != null) {
        throw new BadRequestException(formatMessage("Could not create %s, key already exists: %s", newComponent.qualifier(), keyWithBranch));
      }

      String uuid = Uuids.create();
      ComponentDto component = dbClient.componentDao().insert(session,
        new ComponentDto()
          .setUuid(uuid)
          .setProjectUuid(uuid)
          .setKey(keyWithBranch)
          .setDeprecatedKey(keyWithBranch)
          .setName(newComponent.name())
          .setLongName(newComponent.name())
          .setScope(Scopes.PROJECT)
          .setQualifier(newComponent.qualifier())
          .setCreatedAt(new Date()));
      resourceIndexerDao.indexResource(session, component.getId());
      session.commit();

      permissionService.applyDefaultPermissionTemplate(component.key());
      return component.key();
    } finally {
      session.close();
    }
  }

  private void checkKeyFormat(String qualifier, String kee) {
    if (!ComponentKeys.isValidModuleKey(kee)) {
      throw new BadRequestException(formatMessage("Malformed key for %s: %s. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.",
        qualifier, kee));
    }
  }

  private void checkBranchFormat(String qualifier, @Nullable String branch) {
    if (branch != null && !ComponentKeys.isValidBranch(branch)) {
      throw new BadRequestException(formatMessage("Malformed branch for %s: %s. Allowed characters are alphanumeric, '-', '_', '.' and '/', with at least one non-digit.",
        qualifier, branch));
    }
  }

  private String formatMessage(String message, String qualifier, String key) {
    return String.format(message, i18n.message(Locale.getDefault(), "qualifier." + qualifier, "Project"), key);
  }

  @CheckForNull
  private ComponentDto getNullableByKey(DbSession session, String key) {
    return dbClient.componentDao().getNullableByKey(session, key);
  }

  private ComponentDto getByKey(DbSession session, String key) {
    return dbClient.componentDao().getByKey(session, key);
  }

}
