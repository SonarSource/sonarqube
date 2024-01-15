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
package org.sonar.server.user.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.SessionTokenDto;
import org.sonar.db.user.UserDismissedMessageDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.IdentityProviderRepository;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.user.UserAnonymizer;
import org.sonar.server.common.user.UserDeactivator;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;
import static org.sonar.test.JsonAssert.assertJson;

public class DeactivateActionIT {

  private final System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final UserAnonymizer userAnonymizer = new UserAnonymizer(db.getDbClient(), () -> "anonymized");
  private final UserDeactivator userDeactivator = new UserDeactivator(dbClient, userAnonymizer);
  private final ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);
  private final IdentityProviderRepository identityProviderRepository = mock();
  private final UserService userService = new UserService(dbClient, mock(AvatarResolver.class), mock(ManagedInstanceService.class), managedInstanceChecker, userDeactivator,
    mock(UserUpdater.class), identityProviderRepository);
  private final WsActionTester ws = new WsActionTester(new DeactivateAction(dbClient, userSession, new UserJsonWriter(userSession), userService));

  @Test
  public void deactivate_user_and_delete_their_related_data() {
    createAdminUser();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("ada.lovelace")
      .setEmail("ada.lovelace@noteg.com")
      .setName("Ada Lovelace")
      .setScmAccounts(singletonList("al")));
    logInAsSystemAdministrator();

    deactivate(user.getLogin());

    verifyThatUserIsDeactivated(user.getLogin());
  }

  @Test
  public void anonymize_user_if_param_provided() {
    createAdminUser();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("ada.lovelace")
      .setEmail("ada.lovelace@noteg.com")
      .setName("Ada Lovelace")
      .setScmAccounts(singletonList("al")));
    logInAsSystemAdministrator();

    deactivate(user.getLogin(), true);

    verifyThatUserIsDeactivated("anonymized");
    verifyThatUserIsAnomymized("anonymized");
  }

  @Test
  public void deactivate_user_deletes_their_group_membership() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    db.users().insertGroup();
    db.users().insertMember(group1, user);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().groupMembershipDao().selectGroupUuidsByUserUuid(dbSession, user.getUuid())).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_tokens() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    db.users().insertToken(user);
    db.commit();

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userTokenDao().selectByUser(dbSession, user)).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_properties() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.properties().insertProperty(newUserPropertyDto(user), null, null, null, user.getLogin());
    db.properties().insertProperty(newUserPropertyDto(user), null, null, null, user.getLogin());
    db.properties().insertProperty(newUserPropertyDto(user).setEntityUuid(project.uuid()), project.getKey(),
      project.name(), project.qualifier(), user.getLogin());

    deactivate(user.getLogin());

    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setUserUuid(user.getUuid()).build(), dbSession)).isEmpty();
    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setUserUuid(user.getUuid()).setEntityUuid(project.uuid()).build(), dbSession)).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_permissions() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.SCAN);
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER_QUALITY_PROFILES);
    db.users().insertProjectPermissionOnUser(user, UserRole.USER, project);
    db.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, project);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getUuid())).isEmpty();
    assertThat(db.getDbClient().userPermissionDao().selectEntityPermissionsOfUser(dbSession, user.getUuid(), project.uuid())).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_permission_templates() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();
    PermissionTemplateDto anotherTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(template.getUuid(), user.getUuid(), UserRole.USER, template.getName(), user.getLogin());
    db.permissionTemplates().addUserToTemplate(anotherTemplate.getUuid(), user.getUuid(), UserRole.CODEVIEWER, anotherTemplate.getName(), user.getLogin());

    deactivate(user.getLogin());

    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, template.getUuid())).extracting(PermissionTemplateUserDto::getUserUuid)
      .isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, anotherTemplate.getUuid())).extracting(PermissionTemplateUserDto::getUserUuid)
      .isEmpty();
  }

  @Test
  public void deactivate_user_deletes_their_qprofiles_permissions() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    QProfileDto profile = db.qualityProfiles().insert();
    db.qualityProfiles().addUserPermission(profile, user);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(dbSession, profile, user)).isFalse();
  }

  @Test
  public void deactivate_user_deletes_their_default_assignee_settings() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto anotherProject = db.components().insertPrivateProject().getMainBranchComponent();
    db.properties().insertProperty(new PropertyDto().setKey("sonar.issues.defaultAssigneeLogin").setValue(user.getLogin())
      .setEntityUuid(project.uuid()), project.getKey(), project.name(), project.qualifier(), user.getLogin());
    db.properties().insertProperty(new PropertyDto().setKey("sonar.issues.defaultAssigneeLogin").setValue(user.getLogin())
      .setEntityUuid(anotherProject.uuid()), anotherProject.getKey(), anotherProject.name(), anotherProject.qualifier(), user.getLogin());
    db.properties().insertProperty(new PropertyDto().setKey("other").setValue(user.getLogin())
      .setEntityUuid(anotherProject.uuid()), anotherProject.getKey(), anotherProject.name(), anotherProject.qualifier(), user.getLogin());

    deactivate(user.getLogin());

    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setKey("sonar.issues.defaultAssigneeLogin").build(), db.getSession())).isEmpty();
    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().build(), db.getSession())).extracting(PropertyDto::getKey).containsOnly("other");
  }

  @Test
  public void deactivate_user_deletes_their_qgate_permissions() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().addUserPermission(qualityGate, user);
    assertThat(db.countRowsOfTable("qgate_user_permissions")).isOne();

    deactivate(user.getLogin());

    assertThat(db.countRowsOfTable("qgate_user_permissions")).isZero();
  }

  @Test
  public void deactivate_user_deletes_their_alm_pat() {
    createAdminUser();
    logInAsSystemAdministrator();
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    UserDto user = db.users().insertUser();
    db.almPats().insert(p -> p.setUserUuid(user.getUuid()), p -> p.setAlmSettingUuid(almSettingDto.getUuid()));
    UserDto anotherUser = db.users().insertUser();
    db.almPats().insert(p -> p.setUserUuid(anotherUser.getUuid()), p -> p.setAlmSettingUuid(almSettingDto.getUuid()));

    deactivate(user.getLogin());

    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(dbSession, user.getUuid(), almSettingDto)).isEmpty();
    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(dbSession, anotherUser.getUuid(), almSettingDto)).isNotNull();
  }

  @Test
  public void deactivate_user_deletes_their_session_tokens() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    SessionTokenDto sessionToken1 = db.users().insertSessionToken(user);
    SessionTokenDto sessionToken2 = db.users().insertSessionToken(user);
    UserDto anotherUser = db.users().insertUser();
    SessionTokenDto sessionToken3 = db.users().insertSessionToken(anotherUser);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(dbSession, sessionToken1.getUuid())).isNotPresent();
    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(dbSession, sessionToken2.getUuid())).isNotPresent();
    assertThat(db.getDbClient().sessionTokensDao().selectByUuid(dbSession, sessionToken3.getUuid())).isPresent();
  }

  @Test
  public void deactivate_user_deletes_their_dismissed_messages() {
    createAdminUser();
    logInAsSystemAdministrator();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser();

    db.users().insertUserDismissedMessageOnProject(user, project1, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    db.users().insertUserDismissedMessageOnProject(user, project2, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDto anotherUser = db.users().insertUser();
    UserDismissedMessageDto msg3 = db.users().insertUserDismissedMessageOnProject(anotherUser, project1, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDismissedMessageDto msg4 = db.users().insertUserDismissedMessageOnProject(anotherUser, project2, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userDismissedMessagesDao().selectByUser(dbSession, user)).isEmpty();
    assertThat(db.getDbClient().userDismissedMessagesDao().selectByUser(dbSession, anotherUser))
      .extracting(UserDismissedMessageDto::getUuid)
      .containsExactlyInAnyOrder(msg3.getUuid(), msg4.getUuid());
  }

  @Test
  public void user_cannot_deactivate_itself_on_sonarqube() {
    createAdminUser();
    UserDto user = db.users().insertUser();
    userSession.logIn(user.getLogin()).setSystemAdministrator();

    assertThatThrownBy(() -> {
      deactivate(user.getLogin());

      verifyThatUserExists(user.getLogin());
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Self-deactivation is not possible");
  }

  @Test
  public void deactivation_requires_to_be_logged_in() {
    createAdminUser();

    assertThatThrownBy(() -> {
      deactivate("someone");
    })
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void deactivation_requires_administrator_permission_on_sonarqube() {
    createAdminUser();
    userSession.logIn();

    assertThatThrownBy(() -> {
      deactivate("someone");
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_user_does_not_exist() {
    createAdminUser();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      deactivate("someone");
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'someone' doesn't exist");
  }

  @Test
  public void fail_if_login_is_blank() {
    createAdminUser();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      deactivate("");
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'login' parameter is missing");
  }

  @Test
  public void fail_if_login_is_missing() {
    createAdminUser();
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      deactivate(null);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'login' parameter is missing");
  }

  @Test
  public void fail_to_deactivate_last_administrator() {
    UserDto admin = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(admin, GlobalPermission.ADMINISTER);
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> {
      deactivate(admin.getLogin());
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("User is last administrator, and cannot be deactivated");
  }

  @Test
  public void administrators_can_be_deactivated_if_there_are_still_other_administrators() {
    UserDto admin = createAdminUser();

    UserDto anotherAdmin = createAdminUser();
    logInAsSystemAdministrator();

    deactivate(admin.getLogin());

    verifyThatUserIsDeactivated(admin.getLogin());
    verifyThatUserExists(anotherAdmin.getLogin());
  }

  @Test
  public void test_definition() {
    assertThat(ws.getDef().isPost()).isTrue();
    assertThat(ws.getDef().isInternal()).isFalse();
    assertThat(ws.getDef().params()).hasSize(2);
  }

  @Test
  public void test_example() {
    createAdminUser();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("ada.lovelace")
      .setEmail("ada.lovelace@noteg.com")
      .setName("Ada Lovelace")
      .setLocal(true)
      .setScmAccounts(singletonList("al")));
    logInAsSystemAdministrator();

    String json = deactivate(user.getLogin()).getInput();

    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void anonymizeUser_whenSamlAndScimUser_shouldDeleteScimMapping() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    db.getDbClient().scimUserDao().enableScimForUser(dbSession, user.getUuid());
    db.commit();

    deactivate(user.getLogin(), true);

    assertThat(db.getDbClient().scimUserDao().findByUserUuid(dbSession, user.getUuid())).isEmpty();
  }

  @Test
  public void handle_whenUserManagedAndInstanceManaged_shouldThrow() {
    createAdminUser();
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    doThrow(new IllegalStateException("User managed")).when(managedInstanceChecker).throwIfUserIsManaged(any(), eq(user.getUuid()));

    String login = user.getLogin();
    assertThatThrownBy(() -> deactivate(login))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("User managed");
  }

  @Test
  public void handle_whenInstanceManagedAndNotSystemAdministrator_shouldThrowUnauthorizedException() {
    UserDto userDto = db.users().insertUser();
    String login = userDto.getLogin();

    assertThatThrownBy(() -> deactivate(login))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private TestResponse deactivate(@Nullable String login) {
    return deactivate(login, false);
  }

  private TestResponse deactivate(@Nullable String login, boolean anonymize) {
    return deactivate(ws, login, anonymize);
  }

  private TestResponse deactivate(WsActionTester ws, @Nullable String login, boolean anonymize) {
    TestRequest request = ws.newRequest()
      .setMethod("POST");
    Optional.ofNullable(login).ifPresent(t -> request.setParam("login", login));
    if (anonymize) {
      request.setParam("anonymize", "true");
    }
    return request.execute();
  }

  private void verifyThatUserExists(String login) {
    assertThat(db.users().selectUserByLogin(login)).isPresent();
  }

  private void verifyThatUserIsDeactivated(String login) {
    Optional<UserDto> user = db.users().selectUserByLogin(login);
    assertThat(user).isPresent();
    assertThat(user.get().isActive()).isFalse();
    assertThat(user.get().getEmail()).isNull();
    assertThat(user.get().getSortedScmAccounts()).isEmpty();
  }

  private void verifyThatUserIsAnomymized(String login) {
    Optional<UserDto> user = db.users().selectUserByLogin(login);
    assertThat(user).isPresent();
    assertThat(user.get().getName()).isEqualTo(login);
    assertThat(user.get().getExternalLogin()).isEqualTo(login);
    assertThat(user.get().getExternalId()).isEqualTo(login);
    assertThat(user.get().getExternalIdentityProvider()).isEqualTo(ExternalIdentity.SQ_AUTHORITY);
  }

  private UserDto createAdminUser() {
    UserDto admin = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(admin, GlobalPermission.ADMINISTER);
    db.commit();
    return admin;
  }

}
