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
package org.sonar.server.user.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;
import static org.sonar.process.ProcessProperties.Property.SONARCLOUD_ENABLED;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_ACTIVE;
import static org.sonar.server.user.index.UserIndexDefinition.FIELD_UUID;
import static org.sonar.test.JsonAssert.assertJson;

public class DeactivateActionTest {

  private System2 system2 = AlwaysIncreasingSystem2.INSTANCE;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = EsTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private DbClient dbClient = db.getDbClient();
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private DbSession dbSession = db.getSession();
  private MapSettings settings = new MapSettings();
  private WsActionTester ws = new WsActionTester(new DeactivateAction(dbClient, userIndexer, userSession,
    new UserJsonWriter(userSession), defaultOrganizationProvider, settings.asConfig()));

  @Test
  public void deactivate_user_and_delete_his_related_data() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("ada.lovelace")
      .setEmail("ada.lovelace@noteg.com")
      .setName("Ada Lovelace")
      .setScmAccounts(singletonList("al")));
    logInAsSystemAdministrator();

    deactivate(user.getLogin());

    verifyThatUserIsDeactivated(user.getLogin(), false);
    assertThat(es.client().prepareSearch(UserIndexDefinition.TYPE_USER)
      .setQuery(boolQuery()
        .must(termQuery(FIELD_UUID, user.getUuid()))
        .must(termQuery(FIELD_ACTIVE, "false")))
      .get().getHits().getHits()).hasSize(1);
  }

  @Test
  public void deactivate_user_deletes_his_group_membership() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    GroupDto group1 = db.users().insertGroup();
    db.users().insertGroup();
    db.users().insertMember(group1, user);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().groupMembershipDao().selectGroupIdsByUserId(dbSession, user.getId())).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_tokens() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);
    db.users().insertToken(user);
    db.commit();

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userTokenDao().selectByUser(dbSession, user)).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_properties() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.properties().insertProperty(newUserPropertyDto(user));
    db.properties().insertProperty(newUserPropertyDto(user));
    db.properties().insertProperty(newUserPropertyDto(user).setResourceId(project.getId()));

    deactivate(user.getLogin());

    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setUserId(user.getId()).build(), dbSession)).isEmpty();
    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setUserId(user.getId()).setComponentId(project.getId()).build(), dbSession)).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_permissions() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertPermissionOnUser(user, SCAN);
    db.users().insertPermissionOnUser(user, ADMINISTER_QUALITY_PROFILES);
    db.users().insertProjectPermissionOnUser(user, USER, project);
    db.users().insertProjectPermissionOnUser(user, CODEVIEWER, project);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userPermissionDao().selectGlobalPermissionsOfUser(dbSession, user.getId(), db.getDefaultOrganization().getUuid())).isEmpty();
    assertThat(db.getDbClient().userPermissionDao().selectProjectPermissionsOfUser(dbSession, user.getId(), project.getId())).isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_permission_templates() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();
    PermissionTemplateDto anotherTemplate = db.permissionTemplates().insertTemplate();
    db.permissionTemplates().addUserToTemplate(template.getId(), user.getId(), USER);
    db.permissionTemplates().addUserToTemplate(anotherTemplate.getId(), user.getId(), CODEVIEWER);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, template.getId())).extracting(PermissionTemplateUserDto::getUserId).isEmpty();
    assertThat(db.getDbClient().permissionTemplateDao().selectUserPermissionsByTemplateId(dbSession, anotherTemplate.getId())).extracting(PermissionTemplateUserDto::getUserId)
      .isEmpty();
  }

  @Test
  public void deactivate_user_deletes_his_qprofiles_permissions() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());
    db.qualityProfiles().addUserPermission(profile, user);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().qProfileEditUsersDao().exists(dbSession, profile, user)).isFalse();
  }

  @Test
  public void deactivate_user_deletes_his_default_assignee_settings() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto anotherProject = db.components().insertPrivateProject();
    db.properties().insertProperty(new PropertyDto().setKey("sonar.issues.defaultAssigneeLogin").setValue(user.getLogin()).setResourceId(project.getId()));
    db.properties().insertProperty(new PropertyDto().setKey("sonar.issues.defaultAssigneeLogin").setValue(user.getLogin()).setResourceId(anotherProject.getId()));
    db.properties().insertProperty(new PropertyDto().setKey("other").setValue(user.getLogin()).setResourceId(anotherProject.getId()));

    deactivate(user.getLogin());

    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().setKey("sonar.issues.defaultAssigneeLogin").build(), db.getSession())).isEmpty();
    assertThat(db.getDbClient().propertiesDao().selectByQuery(PropertyQuery.builder().build(), db.getSession())).extracting(PropertyDto::getKey).containsOnly("other");
  }

  @Test
  public void deactivate_user_deletes_his_organization_membership() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    OrganizationDto organization = db.organizations().insert();
    db.organizations().addMember(organization, user);
    OrganizationDto anotherOrganization = db.organizations().insert();
    db.organizations().addMember(anotherOrganization, user);

    deactivate(user.getLogin());

    assertThat(dbClient.organizationMemberDao().select(db.getSession(), organization.getUuid(), user.getId())).isNotPresent();
    assertThat(dbClient.organizationMemberDao().select(db.getSession(), anotherOrganization.getUuid(), user.getId())).isNotPresent();
  }

  @Test
  public void deactivate_user_deletes_his_user_settings() {
    logInAsSystemAdministrator();
    UserDto user = db.users().insertUser();
    db.users().insertUserSetting(user);
    db.users().insertUserSetting(user);
    UserDto anotherUser = db.users().insertUser();
    db.users().insertUserSetting(anotherUser);

    deactivate(user.getLogin());

    assertThat(db.getDbClient().userPropertiesDao().selectByUser(dbSession, user)).isEmpty();
    assertThat(db.getDbClient().userPropertiesDao().selectByUser(dbSession, anotherUser)).hasSize(1);
  }

  @Test
  public void user_can_deactivate_itself_on_sonarcloud() {
    WsActionTester customWs = newSonarCloudWs();

    UserDto user = db.users().insertUser();
    userSession.logIn(user.getLogin());

    deactivate(customWs, user.getLogin());

    verifyThatUserIsDeactivated(user.getLogin(), true);
  }

  @Test
  public void user_cannot_deactivate_another_user_on_sonarcloud() {
    WsActionTester customWs = newSonarCloudWs();

    UserDto user = db.users().insertUser();
    userSession.logIn(user.getLogin());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privilege");

    deactivate(customWs, "other user");
  }

  @Test
  public void user_cannot_deactivate_itself_on_sonarqube() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user.getLogin()).setSystemAdministrator();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Self-deactivation is not possible");

    deactivate(user.getLogin());

    verifyThatUserExists(user.getLogin());
  }

  @Test
  public void deactivation_requires_to_be_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    deactivate("someone");
  }

  @Test
  public void deactivation_requires_administrator_permission_on_sonarqube() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    deactivate("someone");
  }

  @Test
  public void fail_if_user_does_not_exist() {
    logInAsSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User 'someone' doesn't exist");

    deactivate("someone");
  }

  @Test
  public void fail_if_login_is_blank() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'login' parameter is missing");

    deactivate("");
  }

  @Test
  public void fail_if_login_is_missing() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'login' parameter is missing");

    deactivate(null);
  }

  @Test
  public void fail_to_deactivate_last_administrator_of_default_organization() {
    UserDto admin = db.users().insertUser();
    db.users().insertPermissionOnUser(admin, ADMINISTER);
    logInAsSystemAdministrator();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("User is last administrator, and cannot be deactivated");

    deactivate(admin.getLogin());
  }

  @Test
  public void fail_to_deactivate_last_administrator_of_organization() {
    // user1 is the unique administrator of org1 and org2.
    // user1 and user2 are both administrators of org3
    UserDto user1 = db.users().insertUser(u -> u.setLogin("test"));
    OrganizationDto org1 = db.organizations().insert(newOrganizationDto().setKey("org1"));
    OrganizationDto org2 = db.organizations().insert(newOrganizationDto().setKey("org2"));
    OrganizationDto org3 = db.organizations().insert(newOrganizationDto().setKey("org3"));
    db.users().insertPermissionOnUser(org1, user1, ADMINISTER);
    db.users().insertPermissionOnUser(org2, user1, ADMINISTER);
    db.users().insertPermissionOnUser(org3, user1, ADMINISTER);
    UserDto user2 = db.users().insertUser();
    db.users().insertPermissionOnUser(org3, user2, ADMINISTER);
    logInAsSystemAdministrator();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("User 'test' is last administrator of organizations [org1, org2], and cannot be deactivated");

    deactivate(user1.getLogin());
  }

  @Test
  public void administrators_can_be_deactivated_if_there_are_still_other_administrators() {
    UserDto admin = db.users().insertUser();
    UserDto anotherAdmin = db.users().insertUser();
    db.users().insertPermissionOnUser(admin, ADMINISTER);
    db.users().insertPermissionOnUser(anotherAdmin, ADMINISTER);
    db.commit();
    logInAsSystemAdministrator();

    deactivate(admin.getLogin());

    verifyThatUserIsDeactivated(admin.getLogin(), false);
    verifyThatUserExists(anotherAdmin.getLogin());
  }

  @Test
  public void test_definition() {
    assertThat(ws.getDef().isPost()).isTrue();
    assertThat(ws.getDef().isInternal()).isFalse();
    assertThat(ws.getDef().params()).hasSize(1);
  }

  @Test
  public void test_example() {
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

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private TestResponse deactivate(@Nullable String login) {
    return deactivate(ws, login);
  }

  private TestResponse deactivate(WsActionTester ws, @Nullable String login) {
    TestRequest request = ws.newRequest()
      .setMethod("POST");
    Optional.ofNullable(login).ifPresent(t -> request.setParam("login", login));
    return request.execute();
  }

  private void verifyThatUserExists(String login) {
    assertThat(db.users().selectUserByLogin(login)).isPresent();
  }

  private void verifyThatUserIsDeactivated(String login, boolean isSonarCloud) {
    Optional<UserDto> user = db.users().selectUserByLogin(login);
    assertThat(user).isPresent();
    assertThat(user.get().isActive()).isFalse();
    assertThat(user.get().getEmail()).isNull();
    assertThat(user.get().getScmAccountsAsList()).isEmpty();
    if (isSonarCloud) {
      assertThat(user.get().getName()).isNull();
    }
  }

  private WsActionTester newSonarCloudWs() {
    settings.setProperty(SONARCLOUD_ENABLED.getKey(), true);
    return new WsActionTester(new DeactivateAction(dbClient, userIndexer, userSession,
      new UserJsonWriter(userSession), defaultOrganizationProvider, settings.asConfig()));
  }
}
