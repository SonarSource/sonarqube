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
package org.sonar.server.almsettings.ws;

import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.alm.client.github.AppInstallationToken;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.api.GsonRepositoryCollaborator;
import org.sonar.alm.client.github.api.GsonRepositoryTeam;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.api.web.UserRole;
import org.sonar.auth.github.GithubPermissionConverter;
import org.sonar.auth.github.GsonRepositoryPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.provisioning.GithubPermissionsMappingDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserIdDto;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.component.ComponentCreationParameters;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.UserPermissionChange;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.utils.Preconditions.checkState;
import static org.sonar.server.component.NewComponent.newComponentBuilder;

public class GithubProjectCreator implements DevOpsProjectCreator {

  private final DbClient dbClient;
  private final GithubApplicationClient githubApplicationClient;
  private final GithubPermissionConverter githubPermissionConverter;
  private final ProjectKeyGenerator projectKeyGenerator;
  private final ComponentUpdater componentUpdater;
  private final PermissionUpdater<UserPermissionChange> permissionUpdater;
  private final PermissionService permissionService;
  private final GithubProjectCreationParameters githubProjectCreationParameters;
  private final DevOpsProjectDescriptor devOpsProjectDescriptor;
  private final UserSession userSession;
  private final AlmSettingDto almSettingDto;
  private final AccessToken devOpsAppInstallationToken;

  @CheckForNull
  private final AppInstallationToken authAppInstallationToken;

  public GithubProjectCreator(DbClient dbClient, GithubApplicationClient githubApplicationClient, GithubPermissionConverter githubPermissionConverter,
    ProjectKeyGenerator projectKeyGenerator, ComponentUpdater componentUpdater, PermissionUpdater<UserPermissionChange> permissionUpdater, PermissionService permissionService,
    GithubProjectCreationParameters githubProjectCreationParameters) {

    this.dbClient = dbClient;
    this.githubApplicationClient = githubApplicationClient;
    this.githubPermissionConverter = githubPermissionConverter;
    this.projectKeyGenerator = projectKeyGenerator;
    this.componentUpdater = componentUpdater;
    this.permissionUpdater = permissionUpdater;
    this.permissionService = permissionService;
    this.githubProjectCreationParameters = githubProjectCreationParameters;
    userSession = githubProjectCreationParameters.userSession();
    almSettingDto = githubProjectCreationParameters.almSettingDto();
    devOpsProjectDescriptor = githubProjectCreationParameters.devOpsProjectDescriptor();
    devOpsAppInstallationToken = githubProjectCreationParameters.devOpsAppInstallationToken();
    authAppInstallationToken = githubProjectCreationParameters.authAppInstallationToken();
  }

  @Override
  public boolean isScanAllowedUsingPermissionsFromDevopsPlatform() {
    checkState(githubProjectCreationParameters.authAppInstallationToken() != null, "An auth app token is required in case repository permissions checking is necessary.");

    String[] orgaAndRepoTokenified = devOpsProjectDescriptor.projectIdentifier().split("/");
    String organization = orgaAndRepoTokenified[0];
    String repository = orgaAndRepoTokenified[1];

    Set<GithubPermissionsMappingDto> permissionsMappingDtos = dbClient.githubPermissionsMappingDao().findAll(dbClient.openSession(false));

    boolean userHasDirectAccessToRepo = doesUserHaveScanPermission(organization, repository, permissionsMappingDtos);
    if (userHasDirectAccessToRepo) {
      return true;
    }
    return doesUserBelongToAGroupWithScanPermission(organization, repository, permissionsMappingDtos);
  }

  private boolean doesUserHaveScanPermission(String organization, String repository, Set<GithubPermissionsMappingDto> permissionsMappingDtos) {
    Set<GsonRepositoryCollaborator> repositoryCollaborators = githubApplicationClient.getRepositoryCollaborators(devOpsProjectDescriptor.url(), authAppInstallationToken,
      organization, repository);

    String externalLogin = userSession.getExternalIdentity().map(UserSession.ExternalIdentity::login).orElse(null);
    if (externalLogin == null) {
      return false;
    }
    return repositoryCollaborators.stream()
      .filter(gsonRepositoryCollaborator -> externalLogin.equals(gsonRepositoryCollaborator.name()))
      .findAny()
      .map(gsonRepositoryCollaborator -> hasScanPermission(permissionsMappingDtos, gsonRepositoryCollaborator.roleName(), gsonRepositoryCollaborator.permissions()))
      .orElse(false);
  }

  private boolean doesUserBelongToAGroupWithScanPermission(String organization, String repository,
    Set<GithubPermissionsMappingDto> permissionsMappingDtos) {
    Set<GsonRepositoryTeam> repositoryTeams = githubApplicationClient.getRepositoryTeams(devOpsProjectDescriptor.url(), authAppInstallationToken, organization, repository);

    Set<String> groupsOfUser = findUserMembershipOnSonarQube(organization);
    return repositoryTeams.stream()
      .filter(team -> hasScanPermission(permissionsMappingDtos, team.permission(), team.permissions()))
      .map(GsonRepositoryTeam::name)
      .anyMatch(groupsOfUser::contains);
  }

  private Set<String> findUserMembershipOnSonarQube(String organization) {
    return userSession.getGroups().stream()
      .map(GroupDto::getName)
      .filter(groupName -> groupName.contains("/"))
      .map(name -> name.replaceFirst(organization + "/", ""))
      .collect(toSet());
  }

  private boolean hasScanPermission(Set<GithubPermissionsMappingDto> permissionsMappingDtos, String role, GsonRepositoryPermissions permissions) {
    Set<String> sonarqubePermissions = githubPermissionConverter.toSonarqubeRolesWithFallbackOnRepositoryPermissions(permissionsMappingDtos,
      role, permissions);
    return sonarqubePermissions.contains(UserRole.SCAN);
  }

  @Override
  public ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, CreationMethod creationMethod, @Nullable String projectKey) {
    String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
    GithubApplicationClient.Repository repository = githubApplicationClient.getRepository(url, devOpsAppInstallationToken, devOpsProjectDescriptor.projectIdentifier())
      .orElseThrow(() -> new IllegalStateException(
        String.format("Impossible to find the repository '%s' on GitHub, using the devops config %s", devOpsProjectDescriptor.projectIdentifier(), almSettingDto.getKey())));

    return createProjectAndBindToDevOpsPlatform(dbSession, projectKey, almSettingDto, repository, creationMethod);
  }

  private ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, @Nullable String projectKey, AlmSettingDto almSettingDto,
    GithubApplicationClient.Repository repository, CreationMethod creationMethod) {
    ComponentCreationData componentCreationData = createProject(dbSession, projectKey, repository, creationMethod);
    ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();
    createProjectAlmSettingDto(dbSession, repository, projectDto, almSettingDto);
    addScanPermissionToCurrentUser(dbSession, projectDto);
    return componentCreationData;
  }

  private void addScanPermissionToCurrentUser(DbSession dbSession, ProjectDto projectDto) {
    UserIdDto userId = new UserIdDto(requireNonNull(userSession.getUuid()), requireNonNull(userSession.getLogin()));
    UserPermissionChange scanPermission = new UserPermissionChange(Operation.ADD, UserRole.SCAN, projectDto, userId, permissionService);
    permissionUpdater.apply(dbSession, Set.of(scanPermission));
  }

  private ComponentCreationData createProject(DbSession dbSession, @Nullable String projectKey, GithubApplicationClient.Repository repository, CreationMethod creationMethod) {
    NewComponent projectComponent = newComponentBuilder()
      .setKey(Optional.ofNullable(projectKey).orElse(getUniqueProjectKey(repository)))
      .setName(repository.getName())
      .setPrivate(githubProjectCreationParameters.projectsArePrivateByDefault())
      .setQualifier(PROJECT)
      .build();
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(projectComponent)
      .userLogin(userSession.getLogin())
      .userUuid(userSession.getUuid())
      .mainBranchName(repository.getDefaultBranch())
      .isManaged(githubProjectCreationParameters.isProvisioningEnabled())
      .creationMethod(creationMethod)
      .build();
    return componentUpdater.createWithoutCommit(dbSession, componentCreationParameters);
  }

  private String getUniqueProjectKey(GithubApplicationClient.Repository repository) {
    return projectKeyGenerator.generateUniqueProjectKey(repository.getFullName());
  }

  private void createProjectAlmSettingDto(DbSession dbSession, GithubApplicationClient.Repository repo, ProjectDto projectDto, AlmSettingDto almSettingDto) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(repo.getFullName())
      .setAlmSlug(null)
      .setProjectUuid(projectDto.getUuid())
      .setSummaryCommentEnabled(true)
      .setMonorepo(false);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(), projectDto.getName(), projectDto.getKey());
  }

}
