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
package org.sonar.server.common.project;

import java.util.Optional;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectCreatorFactory;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.db.project.CreationMethod.getCreationMethod;
import static org.sonar.db.project.CreationMethod.Category.ALM_IMPORT;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.checkNewCodeDefinitionParam;

@ServerSide
public class ImportProjectService {
  private final DbClient dbClient;
  private final DevOpsProjectCreatorFactory devOpsProjectCreatorFactory;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final NewCodeDefinitionResolver newCodeDefinitionResolver;

  public ImportProjectService(DbClient dbClient, DevOpsProjectCreatorFactory devOpsProjectCreatorFactory, UserSession userSession, ComponentUpdater componentUpdater,
    NewCodeDefinitionResolver newCodeDefinitionResolver) {
    this.dbClient = dbClient;
    this.devOpsProjectCreatorFactory = devOpsProjectCreatorFactory;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.newCodeDefinitionResolver = newCodeDefinitionResolver;
  }

  public ImportedProject importProject(ImportProjectRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkNewCodeDefinitionParam(request.newCodeDefinitionType(), request.newCodeDefinitionValue());
      AlmSettingDto almSetting = dbClient.almSettingDao().selectByUuid(dbSession, request.almSettingId()).orElseThrow(() -> new IllegalArgumentException("ALM setting not found"));
      DevOpsProjectDescriptor projectDescriptor = new DevOpsProjectDescriptor(almSetting.getAlm(), almSetting.getUrl(), request.repositoryIdentifier());

      DevOpsProjectCreator projectCreator = devOpsProjectCreatorFactory.getDevOpsProjectCreator(almSetting, projectDescriptor)
        .orElseThrow(() -> new IllegalArgumentException(format("Platform %s not supported", almSetting.getAlm().name())));

      CreationMethod creationMethod = getCreationMethod(ALM_IMPORT, userSession.isAuthenticatedBrowserSession());
      ComponentCreationData componentCreationData = projectCreator.createProjectAndBindToDevOpsPlatform(
        dbSession,
        creationMethod,
        request.monorepo(),
        request.projectKey(),
        request.projectName());

      ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();
      BranchDto mainBranchDto = Optional.ofNullable(componentCreationData.mainBranchDto()).orElseThrow();

      if (request.newCodeDefinitionType() != null) {
        newCodeDefinitionResolver.createNewCodeDefinition(dbSession, projectDto.getUuid(), mainBranchDto.getUuid(),
          mainBranchDto.getKey(), request.newCodeDefinitionType(), request.newCodeDefinitionValue());
      }
      componentUpdater.commitAndIndex(dbSession, componentCreationData);
      ProjectAlmSettingDto projectAlmSettingDto = dbClient.projectAlmSettingDao().selectByProject(dbSession, projectDto)
        .orElseThrow(() -> new IllegalStateException("Project ALM setting was not created"));
      dbSession.commit();
      return new ImportedProject(projectDto, projectAlmSettingDto);
    }
  }

}
