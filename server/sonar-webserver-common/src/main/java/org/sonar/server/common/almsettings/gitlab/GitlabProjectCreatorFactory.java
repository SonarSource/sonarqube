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
package org.sonar.server.common.almsettings.gitlab;

import java.util.Map;
import java.util.Optional;
import org.sonar.auth.gitlab.GitLabSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DefaultDevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContext;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectCreatorFactory;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;

public class GitlabProjectCreatorFactory implements DevOpsProjectCreatorFactory {
  private final DbClient dbClient;
  private final ProjectKeyGenerator projectKeyGenerator;
  private final ProjectCreator projectCreator;
  private final GitLabSettings gitLabSettings;
  private final PermissionService permissionService;
  private final PermissionUpdater<UserPermissionChange> permissionUpdater;
  private final ManagedProjectService managedProjectService;
  private final GitlabDevOpsProjectCreationContextService gitlabDevOpsProjectService;

  public GitlabProjectCreatorFactory(DbClient dbClient, ProjectKeyGenerator projectKeyGenerator, ProjectCreator projectCreator,
    GitLabSettings gitLabSettings, PermissionService permissionService, PermissionUpdater<UserPermissionChange> permissionUpdater,
    ManagedProjectService managedProjectService, GitlabDevOpsProjectCreationContextService gitlabDevOpsProjectService) {
    this.dbClient = dbClient;
    this.projectKeyGenerator = projectKeyGenerator;
    this.projectCreator = projectCreator;
    this.gitLabSettings = gitLabSettings;
    this.permissionService = permissionService;
    this.permissionUpdater = permissionUpdater;
    this.managedProjectService = managedProjectService;
    this.gitlabDevOpsProjectService = gitlabDevOpsProjectService;
  }

  @Override
  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(DbSession dbSession, Map<String, String> characteristics) {
    return Optional.empty();
  }

  @Override
  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(AlmSettingDto almSettingDto, DevOpsProjectDescriptor devOpsProjectDescriptor) {
    if (almSettingDto.getAlm() != ALM.GITLAB) {
      return Optional.empty();
    }

    DevOpsProjectCreationContext devOpsProjectCreationContext = gitlabDevOpsProjectService.create(almSettingDto, devOpsProjectDescriptor);

    return Optional.of(
      new DefaultDevOpsProjectCreator(
        dbClient,
        devOpsProjectCreationContext,
        projectKeyGenerator,
        gitLabSettings,
        projectCreator,
        permissionService,
        permissionUpdater,
        managedProjectService
      )
    );
  }

}
