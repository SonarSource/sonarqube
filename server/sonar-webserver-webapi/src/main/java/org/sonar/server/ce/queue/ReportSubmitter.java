/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.ce.queue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.ce.task.CeTask;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.server.almsettings.ws.DevOpsPlatformService;
import org.sonar.server.almsettings.ws.DevOpsProjectDescriptor;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.component.ComponentCreationParameters;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.sonar.db.project.CreationMethod.SCANNER_API;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

@ServerSide
public class ReportSubmitter {

  private final CeQueue queue;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final PermissionTemplateService permissionTemplateService;
  private final DbClient dbClient;
  private final BranchSupport branchSupport;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final DevOpsPlatformService devOpsPlatformService;
  private final ManagedInstanceService managedInstanceService;

  public ReportSubmitter(CeQueue queue, UserSession userSession, ComponentUpdater componentUpdater,
    PermissionTemplateService permissionTemplateService, DbClient dbClient, BranchSupport branchSupport, ProjectDefaultVisibility projectDefaultVisibility,
    DevOpsPlatformService devOpsPlatformService, ManagedInstanceService managedInstanceService) {
    this.queue = queue;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.permissionTemplateService = permissionTemplateService;
    this.dbClient = dbClient;
    this.branchSupport = branchSupport;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.devOpsPlatformService = devOpsPlatformService;
    this.managedInstanceService = managedInstanceService;
  }

  public CeTask submit(String projectKey, @Nullable String projectName, Map<String, String> characteristics, InputStream reportInput) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentCreationData componentCreationData = null;
      // Note: when the main branch is analyzed, the characteristics may or may not have the branch name, so componentKey#isMainBranch is not
      // reliable!
      BranchSupport.ComponentKey componentKey = branchSupport.createComponentKey(projectKey, characteristics);
      Optional<ComponentDto> mainBranchComponentOpt = dbClient.componentDao().selectByKey(dbSession, componentKey.getKey());
      ComponentDto mainBranchComponent;

      if (mainBranchComponentOpt.isPresent()) {
        mainBranchComponent = mainBranchComponentOpt.get();
        validateProject(dbSession, mainBranchComponent, projectKey);
      } else {
        componentCreationData = createProject(projectKey, projectName, characteristics, dbSession, componentKey);
        mainBranchComponent = componentCreationData.mainBranchComponent();
      }

      BranchDto mainBranch = dbClient.branchDao().selectByUuid(dbSession, mainBranchComponent.branchUuid())
        .orElseThrow(() -> new IllegalStateException("Couldn't find the main branch of the project"));
      ComponentDto branchComponent;
      if (isMainBranch(componentKey, mainBranch)) {
        branchComponent = mainBranchComponent;
      } else if (componentKey.getBranchName().isPresent()) {
        branchComponent = dbClient.componentDao().selectByKeyAndBranch(dbSession, componentKey.getKey(), componentKey.getBranchName().get())
          .orElseGet(() -> branchSupport.createBranchComponent(dbSession, componentKey, mainBranchComponent, mainBranch));
      } else {
        branchComponent = dbClient.componentDao().selectByKeyAndPullRequest(dbSession, componentKey.getKey(), componentKey.getPullRequestKey().get())
          .orElseGet(() -> branchSupport.createBranchComponent(dbSession, componentKey, mainBranchComponent, mainBranch));
      }

      if (componentCreationData != null) {
        componentUpdater.commitAndIndex(dbSession, componentCreationData);
      } else {
        dbSession.commit();
      }

      checkScanPermission(branchComponent);
      return submitReport(dbSession, reportInput, branchComponent, mainBranch, characteristics);
    }
  }

  private static boolean isMainBranch(BranchSupport.ComponentKey componentKey, BranchDto mainBranch) {
    if (componentKey.isMainBranch()) {
      return true;
    }

    return componentKey.getBranchName().isPresent() && componentKey.getBranchName().get().equals(mainBranch.getKey());
  }

  private void checkScanPermission(ComponentDto project) {
    // this is a specific and inconsistent behavior. For legacy reasons, "technical users"
    // defined with global scan permission should be able to analyze a project even if
    // they don't have the direct permission on the project.
    // That means that dropping the permission on the project does not have any effects
    // if user has still the global permission
    if (!userSession.hasComponentPermission(UserRole.SCAN, project) && !userSession.hasPermission(GlobalPermission.SCAN)) {
      throw insufficientPrivilegesException();
    }
  }

  private void validateProject(DbSession dbSession, ComponentDto component, String rawProjectKey) {
    List<String> errors = new ArrayList<>();

    if (!Qualifiers.PROJECT.equals(component.qualifier()) || !Scopes.PROJECT.equals(component.scope())) {
      errors.add(format("Component '%s' is not a project", rawProjectKey));
    }
    if (!component.branchUuid().equals(component.uuid())) {
      // Project key is already used as a module of another project
      ComponentDto anotherBaseProject = dbClient.componentDao().selectOrFailByUuid(dbSession, component.branchUuid());
      errors.add(format("The project '%s' is already defined in SonarQube but as a module of project '%s'. "
                        + "If you really want to stop directly analysing project '%s', please first delete it from SonarQube and then relaunch the analysis of project '%s'.",
        rawProjectKey, anotherBaseProject.getKey(), anotherBaseProject.getKey(), rawProjectKey));
    }
    if (!errors.isEmpty()) {
      throw BadRequestException.create(errors);
    }
  }

  private ComponentCreationData createProject(String projectKey, @Nullable String projectName, Map<String, String> characteristics,
    DbSession dbSession, BranchSupport.ComponentKey componentKey) {
    userSession.checkPermission(GlobalPermission.PROVISION_PROJECTS);

    DevOpsProjectDescriptor devOpsProjectDescriptor = devOpsPlatformService.getDevOpsProjectDescriptor(characteristics).orElse(null);
    AlmSettingDto almSettingDto = getAlmSettingDto(dbSession, devOpsProjectDescriptor);

    throwIfNoValidDevOpsConfigurationFoundForDevOpsProject(devOpsProjectDescriptor, almSettingDto);
    throwIfCurrentUserWouldNotHaveScanPermission(projectKey, dbSession, devOpsProjectDescriptor, almSettingDto);

    if (almSettingDto != null) {
      return devOpsPlatformService.createProjectAndBindToDevOpsPlatform(dbSession, projectKey, almSettingDto, devOpsProjectDescriptor);
    }
    return createProject(dbSession, componentKey.getKey(), defaultIfBlank(projectName, projectKey));
  }

  @CheckForNull
  private AlmSettingDto getAlmSettingDto(DbSession dbSession, @Nullable DevOpsProjectDescriptor devOpsProjectDescriptor) {
    if (devOpsProjectDescriptor != null) {
      return devOpsPlatformService.getValidAlmSettingDto(dbSession, devOpsProjectDescriptor).orElse(null);
    }
    return null;
  }

  private static void throwIfNoValidDevOpsConfigurationFoundForDevOpsProject(@Nullable DevOpsProjectDescriptor devOpsProjectDescriptor, @Nullable AlmSettingDto almSettingDto) {
    if (devOpsProjectDescriptor != null && almSettingDto == null) {
      throw new IllegalArgumentException(format("The project %s could not be created. It was auto-detected as a %s project "
                                                + "and no valid DevOps platform configuration were found to access %s",
        devOpsProjectDescriptor.projectIdentifier(), devOpsProjectDescriptor.alm(), devOpsProjectDescriptor.url()));
    }
  }

  private void throwIfCurrentUserWouldNotHaveScanPermission(String projectKey, DbSession dbSession, @Nullable DevOpsProjectDescriptor devOpsProjectDescriptor,
    @Nullable AlmSettingDto almSettingDto) {
    if (!wouldCurrentUserHaveScanPermission(projectKey, dbSession, devOpsProjectDescriptor, almSettingDto)) {
      throw insufficientPrivilegesException();
    }
  }

  private boolean wouldCurrentUserHaveScanPermission(String projectKey, DbSession dbSession, @Nullable DevOpsProjectDescriptor devOpsProjectDescriptor,
    @Nullable AlmSettingDto almSettingDto) {
    if (managedInstanceService.isInstanceExternallyManaged() && almSettingDto != null && devOpsProjectDescriptor != null) {
      return devOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(almSettingDto, devOpsProjectDescriptor);
    }
    return permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(dbSession, userSession.getUuid(), projectKey);
  }

  private ComponentCreationData createProject(DbSession dbSession, String projectKey, String projectName) {
    NewComponent newProject = newComponentBuilder()
      .setKey(projectKey)
      .setName(defaultIfBlank(projectName, projectKey))
      .setQualifier(Qualifiers.PROJECT)
      .setPrivate(projectDefaultVisibility.get(dbSession).isPrivate())
      .build();
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(newProject)
      .userLogin(userSession.getLogin())
      .userUuid(userSession.getUuid())
      .creationMethod(SCANNER_API)
      .build();
    return componentUpdater.createWithoutCommit(dbSession, componentCreationParameters);
  }

  private CeTask submitReport(DbSession dbSession, InputStream reportInput, ComponentDto branch, BranchDto mainBranch, Map<String, String> characteristics) {
    CeTaskSubmit.Builder submit = queue.prepareSubmit();

    // the report file must be saved before submitting the task
    dbClient.ceTaskInputDao().insert(dbSession, submit.getUuid(), reportInput);
    dbSession.commit();
    submit.setType(CeTaskTypes.REPORT);
    submit.setComponent(CeTaskSubmit.Component.fromDto(branch.uuid(), mainBranch.getProjectUuid()));
    submit.setSubmitterUuid(userSession.getUuid());
    submit.setCharacteristics(characteristics);
    return queue.submit(submit.build());
  }

}
