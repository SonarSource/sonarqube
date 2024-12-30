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

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.github.GithubPermissionConverter;
import org.sonar.api.web.UserRole;
import org.sonar.auth.github.AppInstallationToken;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.auth.github.GsonRepositoryCollaborator;
import org.sonar.auth.github.GsonRepositoryPermissions;
import org.sonar.auth.github.GsonRepositoryTeam;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.provisioning.DevOpsPermissionsMappingDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContext;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.user.UserSession.IdentityProvider.GITHUB;

@ExtendWith(MockitoExtension.class)
class GithubProjectCreatorTest {

  private static final String ORGANIZATION_NAME = "orga2";
  private static final String REPOSITORY_NAME = "repo1";

  private static final String MAIN_BRANCH_NAME = "defaultBranch";
  private static final DevOpsProjectDescriptor DEVOPS_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, "http://api.com", ORGANIZATION_NAME + "/" + REPOSITORY_NAME,
    null);
  private static final String ALM_SETTING_KEY = "github_config_1";
  private static final String USER_LOGIN = "userLogin";
  private static final String USER_UUID = "userUuid";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private GithubApplicationClient githubApplicationClient;
  @Mock
  private GithubPermissionConverter githubPermissionConverter;
  @Mock
  private ProjectKeyGenerator projectKeyGenerator;
  @Mock
  private ComponentUpdater componentUpdater;
  @Mock
  private AppInstallationToken authAppInstallationToken;
  @Mock
  private UserSession userSession;
  @Mock
  private AlmSettingDto almSettingDto;
  private final PermissionService permissionService = new PermissionServiceImpl(mock());
  @Mock
  private PermissionUpdater<UserPermissionChange> permissionUpdater;
  @Mock
  private ManagedProjectService managedProjectService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ProjectDefaultVisibility projectDefaultVisibility;
  @Mock
  private DevOpsProjectCreationContext devOpsProjectCreationContext;

  private final GitHubSettings gitHubSettings = mock();

  private GithubProjectCreator githubProjectCreator;

  @BeforeEach
  void setup() {
    lenient().when(userSession.getLogin()).thenReturn(USER_LOGIN);
    lenient().when(userSession.getUuid()).thenReturn(USER_UUID);
    lenient().when(devOpsProjectCreationContext.userSession()).thenReturn(userSession);

    lenient().when(almSettingDto.getUrl()).thenReturn(DEVOPS_PROJECT_DESCRIPTOR.url());
    lenient().when(almSettingDto.getKey()).thenReturn(ALM_SETTING_KEY);
    lenient().when(devOpsProjectCreationContext.almSettingDto()).thenReturn(almSettingDto);

    lenient().when(devOpsProjectCreationContext.name()).thenReturn(REPOSITORY_NAME);
    lenient().when(devOpsProjectCreationContext.devOpsPlatformIdentifier()).thenReturn(ORGANIZATION_NAME + "/" + REPOSITORY_NAME);
    lenient().when(devOpsProjectCreationContext.fullName()).thenReturn(ORGANIZATION_NAME + "/" + REPOSITORY_NAME);
    lenient().when(devOpsProjectCreationContext.defaultBranchName()).thenReturn(MAIN_BRANCH_NAME);

    when(gitHubSettings.getDevOpsPlatform()).thenReturn(GITHUB.getKey());

    ProjectCreator projectCreator = new ProjectCreator(userSession, projectDefaultVisibility, componentUpdater);
    githubProjectCreator = new GithubProjectCreator(dbClient, devOpsProjectCreationContext, projectKeyGenerator, gitHubSettings, projectCreator, permissionService, permissionUpdater,
      managedProjectService, githubApplicationClient, githubPermissionConverter, authAppInstallationToken);

  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_whenNoAuthToken_throws() {
    githubProjectCreator = new GithubProjectCreator(dbClient, devOpsProjectCreationContext, projectKeyGenerator, gitHubSettings, null, permissionService, permissionUpdater,
      managedProjectService, githubApplicationClient, githubPermissionConverter, null);

    assertThatIllegalStateException().isThrownBy(() -> githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform())
      .withMessage("An auth app token is required in case repository permissions checking is necessary.");
  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_whenUserIsNotAGitHubUser_returnsFalse() {
    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isFalse();
  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_whenCollaboratorHasDirectAccessButNoScanPermissions_returnsFalse() {
    GsonRepositoryCollaborator collaborator1 = mockCollaborator("collaborator1", 1, "role1", "read", "admin");
    mockGithubCollaboratorsFromApi(collaborator1);
    bindSessionToCollaborator(collaborator1);

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isFalse();
  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_whenCollaboratorHasDirectAccess_returnsTrue() {
    GsonRepositoryCollaborator collaborator1 = mockCollaborator("collaborator1", 1, "role1", "read", "admin");
    GsonRepositoryCollaborator collaborator2 = mockCollaborator("collaborator2", 2, "role2", "read", "scan");
    mockGithubCollaboratorsFromApi(collaborator1, collaborator2);
    bindSessionToCollaborator(collaborator2);

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isTrue();
  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_whenAccessViaTeamButNoScanPermissions_returnsFalse() {
    GsonRepositoryTeam team2 = mockGithubTeam("team2", 2, "role2", "another_perm", UserRole.ADMIN);
    mockTeamsFromApi(team2);
    bindGroupsToUser(team2.name());

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isFalse();
  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_whenAccessViaTeam_returnsTrue() {
    GsonRepositoryTeam team1 = mockGithubTeam("team1", 1, "role1", "read", "another_perm");
    GsonRepositoryTeam team2 = mockGithubTeam("team2", 2, "role2", "another_perm", UserRole.SCAN);
    mockTeamsFromApi(team1, team2);
    bindGroupsToUser(team1.name(), team2.name());

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isTrue();
  }

  @Test
  void isScanAllowedUsingPermissionsFromDevopsPlatform_whenAccessViaTeamButUserNotInTeam_returnsFalse() {
    GsonRepositoryTeam team1 = mockGithubTeam("team1", 1, "role1", "read", "another_perm");
    GsonRepositoryTeam team2 = mockGithubTeam("team2", 2, "role2", "another_perm", UserRole.SCAN);
    mockTeamsFromApi(team1, team2);
    bindGroupsToUser(team1.name());

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isFalse();
  }

  private void bindSessionToCollaborator(GsonRepositoryCollaborator collaborator1) {
    UserSession.ExternalIdentity externalIdentity = new UserSession.ExternalIdentity(String.valueOf(collaborator1.id()), collaborator1.name());
    when(userSession.getExternalIdentity()).thenReturn(Optional.of(externalIdentity));
  }

  private GsonRepositoryCollaborator mockCollaborator(String collaboratorLogin, int id, String role1, String... sqPermissions) {
    GsonRepositoryCollaborator collaborator = new GsonRepositoryCollaborator(collaboratorLogin, id, role1,
      new GsonRepositoryPermissions(false, false, false, false, false));
    mockPermissionsConversion(collaborator, sqPermissions);
    return collaborator;
  }

  private void mockGithubCollaboratorsFromApi(GsonRepositoryCollaborator... repositoryCollaborators) {
    Set<GsonRepositoryCollaborator> collaborators = Arrays.stream(repositoryCollaborators).collect(toSet());
    when(githubApplicationClient.getRepositoryCollaborators(DEVOPS_PROJECT_DESCRIPTOR.url(), authAppInstallationToken, ORGANIZATION_NAME, REPOSITORY_NAME)).thenReturn(
      collaborators);
  }

  private GsonRepositoryTeam mockGithubTeam(String name, int id, String role, String... sqPermissions) {
    GsonRepositoryTeam gsonRepositoryTeam = new GsonRepositoryTeam(name, id, name + "slug", role, new GsonRepositoryPermissions(false, false, false, false, false));
    mockPermissionsConversion(gsonRepositoryTeam, sqPermissions);
    return gsonRepositoryTeam;
  }

  private void mockTeamsFromApi(GsonRepositoryTeam... repositoryTeams) {
    when(githubApplicationClient.getRepositoryTeams(DEVOPS_PROJECT_DESCRIPTOR.url(), authAppInstallationToken, ORGANIZATION_NAME, REPOSITORY_NAME))
      .thenReturn(Arrays.stream(repositoryTeams).collect(toSet()));
  }

  private void mockPermissionsConversion(GsonRepositoryCollaborator collaborator, String... sqPermissions) {
    Set<DevOpsPermissionsMappingDto> devOpsPermissionsMappingDtos = mockPermissionsMappingsDtos();
    lenient().when(githubPermissionConverter.toSonarqubeRolesWithFallbackOnRepositoryPermissions(devOpsPermissionsMappingDtos, collaborator.roleName(), collaborator.permissions()))
      .thenReturn(Arrays.stream(sqPermissions).collect(toSet()));
  }

  private void mockPermissionsConversion(GsonRepositoryTeam team, String... sqPermissions) {
    Set<DevOpsPermissionsMappingDto> devOpsPermissionsMappingDtos = mockPermissionsMappingsDtos();
    lenient().when(githubPermissionConverter.toSonarqubeRolesWithFallbackOnRepositoryPermissions(devOpsPermissionsMappingDtos, team.permission(), team.permissions()))
      .thenReturn(Arrays.stream(sqPermissions).collect(toSet()));
  }

  private Set<DevOpsPermissionsMappingDto> mockPermissionsMappingsDtos() {
    Set<DevOpsPermissionsMappingDto> devOpsPermissionsMappingDtos = Set.of(mock(DevOpsPermissionsMappingDto.class));
    when(dbClient.githubPermissionsMappingDao().findAll(any(), eq(GITHUB.getKey()))).thenReturn(devOpsPermissionsMappingDtos);
    return devOpsPermissionsMappingDtos;
  }

  private void bindGroupsToUser(String... groupNames) {
    Set<GroupDto> groupDtos = Arrays.stream(groupNames)
      .map(groupName -> new GroupDto().setName(ORGANIZATION_NAME + "/" + groupName).setUuid("uuid_" + groupName))
      .collect(toSet());
    when(userSession.getGroups()).thenReturn(groupDtos);
  }

}
