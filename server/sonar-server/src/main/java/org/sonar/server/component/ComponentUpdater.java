/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.component;

import com.google.common.collect.ImmutableSet;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.ProjectIndexer.Cause;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionTemplateService;

import static java.util.Collections.singletonList;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.core.component.ComponentKeys.isValidProjectKey;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class ComponentUpdater {

  private static final Set<String> MAIN_BRANCH_QUALIFIERS = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.APP);

  private final DbClient dbClient;
  private final I18n i18n;
  private final System2 system2;
  private final PermissionTemplateService permissionTemplateService;
  private final FavoriteUpdater favoriteUpdater;
  private final ProjectIndexers projectIndexers;

  public ComponentUpdater(DbClient dbClient, I18n i18n, System2 system2,
    PermissionTemplateService permissionTemplateService, FavoriteUpdater favoriteUpdater,
    ProjectIndexers projectIndexers) {
    this.dbClient = dbClient;
    this.i18n = i18n;
    this.system2 = system2;
    this.permissionTemplateService = permissionTemplateService;
    this.favoriteUpdater = favoriteUpdater;
    this.projectIndexers = projectIndexers;
  }

  /**
   * - Create component
   * - Apply default permission template
   * - Add component to favorite if the component has the 'Project Creators' permission
   * - Index component in es indexes
   */
  public ComponentDto create(DbSession dbSession, NewComponent newComponent, @Nullable Integer userId) {
    ComponentDto componentDto = createWithoutCommit(dbSession, newComponent, userId);
    commitAndIndex(dbSession, componentDto);
    return componentDto;
  }

  /**
   * Create component without committing.
   * Don't forget to call commitAndIndex(...) when ready to commit.
   */
  public ComponentDto createWithoutCommit(DbSession dbSession, NewComponent newComponent, @Nullable Integer userId) {
    checkKeyFormat(newComponent.qualifier(), newComponent.key());
    ComponentDto componentDto = createRootComponent(dbSession, newComponent);
    if (isRootProject(componentDto)) {
      createMainBranch(dbSession, componentDto.uuid());
    }
    removeDuplicatedProjects(dbSession, componentDto.getDbKey());
    handlePermissionTemplate(dbSession, componentDto, userId);
    return componentDto;
  }

  public void commitAndIndex(DbSession dbSession, ComponentDto componentDto) {
    projectIndexers.commitAndIndex(dbSession, singletonList(componentDto), Cause.PROJECT_CREATION);
  }

  private ComponentDto createRootComponent(DbSession session, NewComponent newComponent) {
    checkLegacyBranchFormat(newComponent.qualifier(), newComponent.deprecatedBranch());
    String keyWithBranch = ComponentKeys.createKey(newComponent.key(), newComponent.deprecatedBranch());
    checkRequest(!dbClient.componentDao().selectByKey(session, keyWithBranch).isPresent(),
      "Could not create %s, key already exists: %s", getQualifierToDisplay(newComponent.qualifier()), keyWithBranch);

    String uuid = Uuids.create();
    ComponentDto component = new ComponentDto()
      .setOrganizationUuid(newComponent.getOrganizationUuid())
      .setUuid(uuid)
      .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
      .setRootUuid(uuid)
      .setModuleUuid(null)
      .setModuleUuidPath(ComponentDto.UUID_PATH_SEPARATOR + uuid + ComponentDto.UUID_PATH_SEPARATOR)
      .setProjectUuid(uuid)
      .setDbKey(keyWithBranch)
      .setName(newComponent.name())
      .setLongName(newComponent.name())
      .setScope(Scopes.PROJECT)
      .setQualifier(newComponent.qualifier())
      .setPrivate(newComponent.isPrivate())
      .setCreatedAt(new Date(system2.now()));
    dbClient.componentDao().insert(session, component);

    return component;
  }

  private static boolean isRootProject(ComponentDto componentDto) {
    return Scopes.PROJECT.equals(componentDto.scope())
      && MAIN_BRANCH_QUALIFIERS.contains(componentDto.qualifier());
  }

  private void createMainBranch(DbSession session, String componentUuid) {
    BranchDto branch = new BranchDto()
      .setBranchType(BranchType.LONG)
      .setUuid(componentUuid)
      .setKey(BranchDto.DEFAULT_MAIN_BRANCH_NAME)
      .setMergeBranchUuid(null)
      .setProjectUuid(componentUuid);
    dbClient.branchDao().upsert(session, branch);
  }

  /**
   * On MySQL, as PROJECTS.KEE is not unique, if the same project is provisioned multiple times, then it will be duplicated in the database.
   * So, after creating a project, we commit, and we search in the db if their are some duplications and we remove them.
   *
   * SONAR-6332
   */
  private void removeDuplicatedProjects(DbSession session, String projectKey) {
    List<ComponentDto> duplicated = dbClient.componentDao().selectComponentsHavingSameKeyOrderedById(session, projectKey);
    for (int i = 1; i < duplicated.size(); i++) {
      dbClient.componentDao().delete(session, duplicated.get(i).getId());
    }
  }

  private void handlePermissionTemplate(DbSession dbSession, ComponentDto componentDto, @Nullable Integer userId) {
    permissionTemplateService.applyDefault(dbSession, componentDto, userId);
    if (componentDto.qualifier().equals(PROJECT)
      && permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(dbSession, componentDto)) {
      favoriteUpdater.add(dbSession, componentDto, userId, false);
    }
  }

  private void checkKeyFormat(String qualifier, String key) {
    checkRequest(isValidProjectKey(key),
      "Malformed key for %s: %s. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.", getQualifierToDisplay(qualifier), key);
  }

  private void checkLegacyBranchFormat(String qualifier, @Nullable String branch) {
    checkRequest(branch == null || ComponentKeys.isValidLegacyBranch(branch),
      "Malformed branch for %s: %s. Allowed characters are alphanumeric, '-', '_', '.' and '/', with at least one non-digit.", getQualifierToDisplay(qualifier), branch);
  }

  private String getQualifierToDisplay(String qualifier) {
    return i18n.message(Locale.getDefault(), "qualifier." + qualifier, "Project");
  }

}
