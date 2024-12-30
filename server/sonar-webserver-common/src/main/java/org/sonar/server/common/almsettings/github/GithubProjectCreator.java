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
package org.sonar.server.common.almsettings.github;

import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.alm.client.github.GithubPermissionConverter;
import org.sonar.api.web.UserRole;
import org.sonar.auth.DevOpsPlatformSettings;
import org.sonar.auth.github.AppInstallationToken;
import org.sonar.auth.github.GsonRepositoryCollaborator;
import org.sonar.auth.github.GsonRepositoryPermissions;
import org.sonar.auth.github.GsonRepositoryTeam;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.provisioning.DevOpsPermissionsMappingDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DefaultDevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContext;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.utils.Preconditions.checkState;

public class GithubProjectCreator extends DefaultDevOpsProjectCreator {

  private final GithubApplicationClient githubApplicationClient;
  private final GithubPermissionConverter githubPermissionConverter;

  @CheckForNull
  private final AppInstallationToken authAppInstallationToken;

  public GithubProjectCreator(DbClient dbClient, DevOpsProjectCreationContext devOpsProjectCreationContext,
    ProjectKeyGenerator projectKeyGenerator,
    DevOpsPlatformSettings devOpsPlatformSettings, ProjectCreator projectCreator, PermissionService permissionService, PermissionUpdater<UserPermissionChange> permissionUpdater,
    ManagedProjectService managedProjectService, GithubApplicationClient githubApplicationClient, GithubPermissionConverter githubPermissionConverter,
    @CheckForNull AppInstallationToken authAppInstallationToken) {
    super(dbClient, devOpsProjectCreationContext, projectKeyGenerator, devOpsPlatformSettings, projectCreator, permissionService, permissionUpdater,
      managedProjectService);
    this.githubApplicationClient = githubApplicationClient;
    this.githubPermissionConverter = githubPermissionConverter;
    this.authAppInstallationToken = authAppInstallationToken;
  }

  @Override
  public boolean isScanAllowedUsingPermissionsFromDevopsPlatform() {
    checkState(authAppInstallationToken != null, "An auth app token is required in case repository permissions checking is necessary.");

    String[] orgaAndRepoTokenified = devOpsProjectCreationContext.fullName().split("/");
    String organization = orgaAndRepoTokenified[0];
    String repository = orgaAndRepoTokenified[1];

    Set<DevOpsPermissionsMappingDto> permissionsMappingDtos = dbClient.githubPermissionsMappingDao()
      .findAll(dbClient.openSession(false), devOpsPlatformSettings.getDevOpsPlatform());

    boolean userHasDirectAccessToRepo = doesUserHaveScanPermission(organization, repository, permissionsMappingDtos);
    if (userHasDirectAccessToRepo) {
      return true;
    }
    return doesUserBelongToAGroupWithScanPermission(organization, repository, permissionsMappingDtos);
  }

  private boolean doesUserHaveScanPermission(String organization, String repository, Set<DevOpsPermissionsMappingDto> permissionsMappingDtos) {
    String url = requireNonNull(devOpsProjectCreationContext.almSettingDto().getUrl(), "GitHub url not defined");
    Set<GsonRepositoryCollaborator> repositoryCollaborators = githubApplicationClient.getRepositoryCollaborators(url, authAppInstallationToken, organization, repository);

    UserSession userSession = devOpsProjectCreationContext.userSession();
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
    Set<DevOpsPermissionsMappingDto> permissionsMappingDtos) {
    String url = requireNonNull(devOpsProjectCreationContext.almSettingDto().getUrl(), "GitHub url not defined");
    Set<GsonRepositoryTeam> repositoryTeams = githubApplicationClient.getRepositoryTeams(url, authAppInstallationToken, organization, repository);

    Set<String> groupsOfUser = findUserMembershipOnSonarQube(organization);
    return repositoryTeams.stream()
      .filter(team -> hasScanPermission(permissionsMappingDtos, team.permission(), team.permissions()))
      .map(GsonRepositoryTeam::name)
      .anyMatch(groupsOfUser::contains);
  }

  private Set<String> findUserMembershipOnSonarQube(String organization) {
    return devOpsProjectCreationContext.userSession().getGroups().stream()
      .map(GroupDto::getName)
      .filter(groupName -> groupName.contains("/"))
      .map(name -> name.replaceFirst(organization + "/", ""))
      .collect(toSet());
  }

  private boolean hasScanPermission(Set<DevOpsPermissionsMappingDto> permissionsMappingDtos, String role, GsonRepositoryPermissions permissions) {
    Set<String> sonarqubePermissions = githubPermissionConverter.toSonarqubeRolesWithFallbackOnRepositoryPermissions(permissionsMappingDtos,
      role, permissions);
    return sonarqubePermissions.contains(UserRole.SCAN);
  }

}
