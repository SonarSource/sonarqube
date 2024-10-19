/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.common.almsettings;

import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.web.UserRole;
import org.sonar.auth.DevOpsPlatformSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserIdDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;

public class DefaultDevOpsProjectCreator implements DevOpsProjectCreator {

  protected final DbClient dbClient;
  protected final ProjectKeyGenerator projectKeyGenerator;
  protected final DevOpsPlatformSettings devOpsPlatformSettings;
  protected final ProjectCreator projectCreator;
  protected final PermissionService permissionService;
  protected final PermissionUpdater<UserPermissionChange> permissionUpdater;
  protected final ManagedProjectService managedProjectService;
  protected final DevOpsProjectCreationContext devOpsProjectCreationContext;

  public DefaultDevOpsProjectCreator(DbClient dbClient, DevOpsProjectCreationContext devOpsProjectCreationContext, ProjectKeyGenerator projectKeyGenerator,
    DevOpsPlatformSettings devOpsPlatformSettings, ProjectCreator projectCreator, PermissionService permissionService, PermissionUpdater<UserPermissionChange> permissionUpdater,
    ManagedProjectService managedProjectService) {
    this.dbClient = dbClient;
    this.projectKeyGenerator = projectKeyGenerator;
    this.devOpsPlatformSettings = devOpsPlatformSettings;
    this.projectCreator = projectCreator;
    this.permissionService = permissionService;
    this.permissionUpdater = permissionUpdater;
    this.managedProjectService = managedProjectService;
    this.devOpsProjectCreationContext = devOpsProjectCreationContext;
  }

  @Override
  public boolean isScanAllowedUsingPermissionsFromDevopsPlatform() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  @Override
  public ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, OrganizationDto organization, CreationMethod creationMethod, Boolean monorepo, @Nullable String projectKey,
                                                                    @Nullable String projectName) {
    String key = Optional.ofNullable(projectKey).orElse(generateUniqueProjectKey());
    boolean isManaged = devOpsPlatformSettings.isProvisioningEnabled();
    Boolean shouldProjectBePrivate = shouldProjectBePrivate(devOpsProjectCreationContext.isPublic());

    ComponentCreationData componentCreationData = projectCreator.createProject(dbSession, organization, key, getProjectName(projectName),
      devOpsProjectCreationContext.defaultBranchName(), creationMethod, shouldProjectBePrivate, isManaged);
    ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();

    createProjectAlmSettingDto(dbSession, projectDto, devOpsProjectCreationContext.almSettingDto(), monorepo);
    addScanPermissionToCurrentUser(dbSession, projectDto);

    BranchDto mainBranchDto = Optional.ofNullable(componentCreationData.mainBranchDto()).orElseThrow();
    if (isManaged) {
      syncProjectPermissionsWithDevOpsPlatform(projectDto, mainBranchDto);
    }
    return componentCreationData;
  }

  @CheckForNull
  private Boolean shouldProjectBePrivate(boolean isPublic) {
    if (devOpsPlatformSettings.isProvisioningEnabled() && devOpsPlatformSettings.isProjectVisibilitySynchronizationActivated()) {
      return !isPublic;
    } else if (devOpsPlatformSettings.isProvisioningEnabled()) {
      return true;
    }
    return null;
  }

  private String getProjectName(@Nullable String projectName) {
    return Optional.ofNullable(projectName).orElse(devOpsProjectCreationContext.name());
  }

  private String generateUniqueProjectKey() {
    return projectKeyGenerator.generateUniqueProjectKey(devOpsProjectCreationContext.fullName());
  }

  private void createProjectAlmSettingDto(DbSession dbSession, ProjectDto projectDto, AlmSettingDto almSettingDto, Boolean monorepo) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(devOpsProjectCreationContext.devOpsPlatformIdentifier())
      .setProjectUuid(projectDto.getUuid())
      .setSummaryCommentEnabled(true)
      .setMonorepo(monorepo);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(), projectDto.getName(), projectDto.getKey());
  }

  private void addScanPermissionToCurrentUser(DbSession dbSession, ProjectDto projectDto) {
    UserSession userSession = devOpsProjectCreationContext.userSession();
    UserIdDto userId = new UserIdDto(requireNonNull(userSession.getUuid()), requireNonNull(userSession.getLogin()));
    UserPermissionChange scanPermission = new UserPermissionChange(Operation.ADD, projectDto.getOrganizationUuid(), UserRole.SCAN, projectDto, userId, permissionService);
    permissionUpdater.apply(dbSession, Set.of(scanPermission));
  }

  private void syncProjectPermissionsWithDevOpsPlatform(ProjectDto projectDto, BranchDto mainBranchDto) {
    String userUuid = requireNonNull(devOpsProjectCreationContext.userSession().getUuid());
    managedProjectService.queuePermissionSyncTask(userUuid, mainBranchDto.getUuid(), projectDto.getUuid());
  }
}
