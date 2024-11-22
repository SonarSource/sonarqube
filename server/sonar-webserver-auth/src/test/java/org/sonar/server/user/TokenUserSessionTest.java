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
package org.sonar.server.user;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.SCAN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentDbTester.toProjectDto;
import static org.sonar.db.user.TokenType.GLOBAL_ANALYSIS_TOKEN;
import static org.sonar.db.user.TokenType.PROJECT_ANALYSIS_TOKEN;
import static org.sonar.db.user.TokenType.PROJECT_BADGE_TOKEN;
import static org.sonar.db.user.TokenType.USER_TOKEN;

@RunWith(DataProviderRunner.class)
public class TokenUserSessionTest {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  private final DbClient dbClient = db.getDbClient();

  @Test
  public void token_can_be_retrieved_from_the_session() {
    ComponentDto project1 = db.components().insertPrivateProject();

    UserDto user = db.users().insertUser();

    db.users().insertProjectPermissionOnUser(user, SCAN, project1);

    TokenUserSession userSession = mockTokenUserSession(user);

    assertThat(userSession.getUserToken()).isNotNull();
    assertThat(userSession.getUserToken().getName()).isEqualTo("User Token");
    assertThat(userSession.getUserToken().getUserUuid()).isEqualTo("userUid");
    assertThat(userSession.getUserToken().getType()).isEqualTo("USER_TOKEN");
  }

  @Test
  public void test_hasProjectsPermission_for_UserToken() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    UserDto user = db.users().insertUser();

    db.users().insertProjectPermissionOnUser(user, SCAN, project1);

    TokenUserSession userSession = mockTokenUserSession(user);

    assertThat(userSession.hasProjectUuidPermission(SCAN, project1.branchUuid())).isTrue();
    assertThat(userSession.hasProjectUuidPermission(SCAN, project2.branchUuid())).isFalse();
  }

  @Test
  public void test_hasProjectsPermission_for_ProjecAnalysisToken() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    UserDto user = db.users().insertUser();

    db.users().insertProjectPermissionOnUser(user, SCAN, project1);
    db.users().insertProjectPermissionOnUser(user, SCAN, project2);

    TokenUserSession userSession = mockProjectAnalysisTokenUserSession(user, project1);

    assertThat(userSession.hasProjectUuidPermission(SCAN, project1.branchUuid())).isTrue();
    assertThat(userSession.hasProjectUuidPermission(SCAN, project2.branchUuid())).isFalse();
  }

  @Test
  public void test_hasProjectsPermission_for_ProjectAnalysisToken_with_global_permission() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    UserDto user = db.users().insertUser();

    db.users().insertPermissionOnUser(user, GlobalPermission.SCAN);

    TokenUserSession userSession = mockProjectAnalysisTokenUserSession(user, project1);

    assertThat(userSession.hasProjectUuidPermission(SCAN, project1.branchUuid())).isTrue();
    assertThat(userSession.hasProjectUuidPermission(SCAN, project2.branchUuid())).isFalse();
  }

  @Test
  public void test_hasGlobalPermission_for_UserToken() {
    UserDto user = db.users().insertUser();
    db.users().insertPermissionOnUser(user, GlobalPermission.SCAN);

    TokenUserSession userSession = mockTokenUserSession(user);

    assertThat(userSession.hasPermission(GlobalPermission.SCAN)).isTrue();
  }

  @Test
  public void test_hasGlobalPermission_for_ProjecAnalysisToken() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();

    UserDto user = db.users().insertUser();

    db.users().insertProjectPermissionOnUser(user, SCAN, project1);
    db.users().insertProjectPermissionOnUser(user, SCAN, project2);

    db.users().insertPermissionOnUser(user, GlobalPermission.SCAN);

    TokenUserSession userSession = mockProjectAnalysisTokenUserSession(user, project1);

    assertThat(userSession.hasPermission(GlobalPermission.SCAN)).isFalse();
  }

  @Test
  public void test_hasGlobalPermission_for_GlobalAnalysisToken() {
    ComponentDto project1 = db.components().insertPrivateProject();

    UserDto user = db.users().insertUser();

    db.users().insertPermissionOnUser(user, GlobalPermission.SCAN);

    TokenUserSession userSession = mockGlobalAnalysisTokenUserSession(user);

    assertThat(userSession.hasProjectUuidPermission(SCAN, project1.branchUuid())).isFalse();
    assertThat(userSession.hasPermission(GlobalPermission.SCAN)).isTrue();
  }

  @Test
  public void test_hasProvisionProjectsGlobalPermission_for_GlobalAnalysisToken_returnsTrueIfUserIsGranted() {
    UserDto user = db.users().insertUser();

    db.users().insertPermissionOnUser(user, GlobalPermission.SCAN);
    db.users().insertPermissionOnUser(user, GlobalPermission.PROVISION_PROJECTS);

    TokenUserSession userSession = mockGlobalAnalysisTokenUserSession(user);

    assertThat(userSession.hasPermission(GlobalPermission.PROVISION_PROJECTS)).isTrue();
  }

  @Test
  public void test_hasProvisionProjectsGlobalPermission_for_GlobalAnalysisToken_returnsFalseIfUserIsNotGranted() {
    UserDto user = db.users().insertUser();

    db.users().insertPermissionOnUser(user, GlobalPermission.SCAN);

    TokenUserSession userSession = mockGlobalAnalysisTokenUserSession(user);

    assertThat(userSession.hasPermission(GlobalPermission.PROVISION_PROJECTS)).isFalse();
  }

  @Test
  public void test_hasAdministerGlobalPermission_for_GlobalAnalysisToken_returnsFalse() {
    UserDto user = db.users().insertUser();

    db.users().insertPermissionOnUser(user, GlobalPermission.ADMINISTER);

    TokenUserSession userSession = mockGlobalAnalysisTokenUserSession(user);

    assertThat(userSession.hasPermission(GlobalPermission.ADMINISTER)).isFalse();
  }

  @Test
  public void keepAuthorizedEntities_shouldFilterProjects_whenGlobalAnalysisToken() {
    UserDto user = db.users().insertUser();

    ComponentDto privateProjectComponent = db.components().insertPrivateProject();
    ProjectDto publicProject = db.components().insertPublicProjectDto();
    ProjectDto privateProject = toProjectDto(privateProjectComponent, 0L);
    ProjectDto privateProjectWithoutAccess = db.components().insertPrivateProjectDto();

    db.users().insertProjectPermissionOnUser(user, USER, privateProjectComponent);

    Set<ProjectDto> projectDto = Set.of(publicProject, privateProject);
    List<ProjectDto> projectDtos = mockGlobalAnalysisTokenUserSession(user).doKeepAuthorizedProjects(USER, projectDto);

    assertThat(projectDtos).containsExactlyInAnyOrder(privateProject, publicProject)
      .doesNotContain(privateProjectWithoutAccess);
  }

  @Test
  @UseDataProvider("validPermissions")
  public void keepAuthorizedEntities_shouldFilterPrivateProjects_whenProjectAnalysisToken(String permission) {
    UserDto user = db.users().insertUser();

    ComponentDto privateProjectComponent = db.components().insertPrivateProject();
    ProjectDto publicProject = db.components().insertPublicProjectDto();
    ProjectDto privateProject = toProjectDto(privateProjectComponent, 0L);
    ProjectDto privateProjectWithoutAccess = db.components().insertPrivateProjectDto();

    db.users().insertProjectPermissionOnUser(user, permission, privateProjectComponent);

    Set<ProjectDto> projectDto = Set.of(publicProject, privateProject);
    List<ProjectDto> projectDtos = mockProjectAnalysisTokenUserSession(user, privateProjectComponent).keepAuthorizedProjects(permission, projectDto);

    assertThat(projectDtos).containsExactly(privateProject)
      .doesNotContain(privateProjectWithoutAccess);
  }

  @Test
  public void keepAuthorizedEntities_shouldFilterPrivateProjects_whenUserToken() {
    UserDto user = db.users().insertUser();

    ComponentDto privateProjectComponent = db.components().insertPrivateProject();
    ProjectDto publicProject = db.components().insertPublicProjectDto();
    ProjectDto privateProject = toProjectDto(privateProjectComponent, 0L);
    ProjectDto privateProjectWithoutAccess = db.components().insertPrivateProjectDto();

    db.users().insertProjectPermissionOnUser(user, USER, privateProjectComponent);

    Set<ProjectDto> projectDto = Set.of(publicProject, privateProject);
    List<ProjectDto> projectDtos = mockTokenUserSession(user).keepAuthorizedProjects(USER, projectDto);

    assertThat(projectDtos).containsExactlyInAnyOrder(privateProject, publicProject)
      .doesNotContain(privateProjectWithoutAccess);
  }

  @Test
  public void keepAuthorizedEntities_shouldFilterPrivateProjects_returnEmptyListForPermissionOtherThanScanOrBrowse() {
    UserDto user = db.users().insertUser();

    ProjectDto publicProject = db.components().insertPublicProjectDto();
    ComponentDto privateProjectComponent = db.components().insertPrivateProject();
    ProjectDto privateProject = toProjectDto(privateProjectComponent, 0L);

    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, privateProjectComponent);

    Set<ProjectDto> projectDto = Set.of(publicProject, privateProject);
    List<ProjectDto> projectDtos = mockProjectAnalysisTokenUserSession(user, privateProjectComponent).keepAuthorizedProjects(CODEVIEWER, projectDto);

    assertThat(projectDtos).isEmpty();
  }

  @Test
  public void keepAuthorizedEntities_shouldFailForUnsupportedTokenSession() {
    UserDto user = db.users().insertUser();

    ProjectDto publicProject = db.components().insertPublicProjectDto();
    ComponentDto privateProjectComponent = db.components().insertPrivateProject();
    ProjectDto privateProject = toProjectDto(privateProjectComponent, 0L);

    db.users().insertProjectPermissionOnUser(user, USER, privateProjectComponent);

    Set<ProjectDto> projectDto = Set.of(publicProject, privateProject);

    TokenUserSession tokenUserSession = mockProjectBadgeTokenSession(user);
    assertThatThrownBy(() -> tokenUserSession.keepAuthorizedProjects(USER, projectDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported token type PROJECT_BADGE_TOKEN");
  }

  @Test
  public void keepAuthorizedComponents_shouldFilterProjects_whenGlobalAnalysisToken() {
    UserDto user = db.users().insertUser();

    ComponentDto publicProject = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto privateProjectWithoutAccess = db.components().insertPrivateProject();

    db.users().insertProjectPermissionOnUser(user, USER, privateProject);

    Set<ComponentDto> componentDtos = Set.of(publicProject, privateProject);
    List<ComponentDto> authorizedComponents = mockGlobalAnalysisTokenUserSession(user).keepAuthorizedComponents(USER, componentDtos);

    assertThat(authorizedComponents).containsExactlyInAnyOrder(privateProject, publicProject)
      .doesNotContain(privateProjectWithoutAccess);
  }

  @Test
  @UseDataProvider("validPermissions")
  public void keepAuthorizedComponents_shouldFilterPrivateProjects_whenProjectAnalysisToken(String permission) {
    UserDto user = db.users().insertUser();

    ComponentDto publicProject = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto privateProjectWithoutAccess = db.components().insertPrivateProject();

    db.users().insertProjectPermissionOnUser(user, permission, privateProject);

    Set<ComponentDto> componentDtos = Set.of(publicProject, privateProject);
    List<ComponentDto> authorizedComponents = mockProjectAnalysisTokenUserSession(user, privateProject)
      .keepAuthorizedComponents(permission, componentDtos);

    assertThat(authorizedComponents).containsExactly(privateProject)
      .doesNotContain(privateProjectWithoutAccess, publicProject);
  }

  @Test
  public void keepAuthorizedComponents_shouldFilterPrivateProjects_whenUserToken() {
    UserDto user = db.users().insertUser();

    ComponentDto publicProject = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto privateProjectWithoutAccess = db.components().insertPrivateProject();

    db.users().insertProjectPermissionOnUser(user, USER, privateProject);

    Set<ComponentDto> componentDtos = Set.of(publicProject, privateProject);
    List<ComponentDto> authorizedComponents = mockTokenUserSession(user).keepAuthorizedComponents(USER, componentDtos);

    assertThat(authorizedComponents).containsExactlyInAnyOrder(privateProject, publicProject)
      .doesNotContain(privateProjectWithoutAccess);
  }

  @Test
  public void keepAuthorizedComponents_returnEmptyListForPermissionOtherThanScanOrBrowse() {
    UserDto user = db.users().insertUser();

    ComponentDto publicProject = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();

    db.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, privateProject);

    Set<ComponentDto> componentDtos = Set.of(publicProject, privateProject);
    List<ComponentDto> authorizedComponents = mockProjectAnalysisTokenUserSession(user, privateProject)
      .keepAuthorizedComponents(UserRole.CODEVIEWER, componentDtos);

    assertThat(authorizedComponents).isEmpty();
  }

  @Test
  public void keepAuthorizedComponents_shouldFailForUnsupportedTokenSession() {
    UserDto user = db.users().insertUser();

    ComponentDto publicProject = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();

    db.users().insertProjectPermissionOnUser(user, USER, privateProject);

    Set<ComponentDto> componentDtos = Set.of(publicProject, privateProject);

    TokenUserSession tokenUserSession = mockProjectBadgeTokenSession(user);
    assertThatThrownBy(() -> tokenUserSession.keepAuthorizedComponents(USER, componentDtos))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported token type PROJECT_BADGE_TOKEN");
  }

  @DataProvider
  public static Object[][] validPermissions() {
    return new Object[][] {
      {USER},
      {SCAN},
    };
  }

  @Test
  public void getType_shouldReturnValidTypeOfToken() {
    UserDto user = db.users().insertUser();
    ComponentDto privateComponent = db.components().insertPrivateProject();

    TokenUserSession projectBadgeTokenSession = mockProjectBadgeTokenSession(user);
    assertThat(projectBadgeTokenSession.getTokenType()).isEqualTo(PROJECT_BADGE_TOKEN);

    TokenUserSession tokenUserSession = mockTokenUserSession(user);
    assertThat(tokenUserSession.getTokenType()).isEqualTo(USER_TOKEN);

    TokenUserSession projectAnalysisTokenUserSession = mockProjectAnalysisTokenUserSession(user, privateComponent);
    assertThat(projectAnalysisTokenUserSession.getTokenType()).isEqualTo(PROJECT_ANALYSIS_TOKEN);

    TokenUserSession globalAnalysisTokenUserSession = mockGlobalAnalysisTokenUserSession(user);
    assertThat(globalAnalysisTokenUserSession.getTokenType()).isEqualTo(GLOBAL_ANALYSIS_TOKEN);
  }

  private TokenUserSession mockTokenUserSession(UserDto userDto) {
    return new TokenUserSession(dbClient, userDto, mockUserTokenDto());
  }

  private TokenUserSession mockProjectAnalysisTokenUserSession(UserDto userDto, ComponentDto componentDto) {
    return new TokenUserSession(dbClient, userDto, mockProjectAnalysisTokenDto(componentDto));
  }

  private TokenUserSession mockGlobalAnalysisTokenUserSession(UserDto userDto) {
    return new TokenUserSession(dbClient, userDto, mockGlobalAnalysisTokenDto());
  }

  private TokenUserSession mockProjectBadgeTokenSession(UserDto userDto) {
    return new TokenUserSession(dbClient, userDto, mockBadgeTokenDto());
  }

  private static UserTokenDto mockUserTokenDto() {
    UserTokenDto userTokenDto = new UserTokenDto();
    userTokenDto.setType(USER_TOKEN.name());
    userTokenDto.setName("User Token");
    userTokenDto.setUserUuid("userUid");
    return userTokenDto;
  }

  private static UserTokenDto mockProjectAnalysisTokenDto(ComponentDto componentDto) {
    UserTokenDto userTokenDto = new UserTokenDto();
    userTokenDto.setType(PROJECT_ANALYSIS_TOKEN.name());
    userTokenDto.setName("Project Analysis Token");
    userTokenDto.setUserUuid("userUid");
    userTokenDto.setProjectKey(componentDto.getKey());
    userTokenDto.setProjectName(componentDto.name());
    userTokenDto.setProjectUuid(componentDto.branchUuid());
    return userTokenDto;
  }

  private static UserTokenDto mockGlobalAnalysisTokenDto() {
    UserTokenDto userTokenDto = new UserTokenDto();
    userTokenDto.setType(GLOBAL_ANALYSIS_TOKEN.name());
    userTokenDto.setName("Global Analysis Token");
    userTokenDto.setUserUuid("userUid");
    return userTokenDto;
  }

  private static UserTokenDto mockBadgeTokenDto() {
    UserTokenDto userTokenDto = new UserTokenDto();
    userTokenDto.setType(PROJECT_BADGE_TOKEN.name());
    userTokenDto.setName("Badge token");
    userTokenDto.setUserUuid("userUid");
    return userTokenDto;
  }

}
