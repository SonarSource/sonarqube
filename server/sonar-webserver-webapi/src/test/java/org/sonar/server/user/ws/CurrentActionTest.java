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

import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.AvatarResolverImpl;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Users.CurrentWsResponse;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.test.JsonAssert.assertJson;

public class CurrentActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PluginRepository pluginRepository = mock(PluginRepository.class);
  private MapSettings settings = new MapSettings();
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private HomepageTypesImpl homepageTypes = new HomepageTypesImpl(settings.asConfig(), organizationFlags, db.getDbClient());
  private PermissionService permissionService = new PermissionServiceImpl(new ResourceTypes(new ResourceTypeTree[] {
    ResourceTypeTree.builder().addType(ResourceType.builder(Qualifiers.PROJECT).build()).build()}));

  private WsActionTester ws = new WsActionTester(
    new CurrentAction(userSession, db.getDbClient(), TestDefaultOrganizationProvider.from(db), new AvatarResolverImpl(), homepageTypes, pluginRepository, permissionService));

  @Test
  public void return_user_info() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("obiwan.kenobi")
      .setName("Obiwan Kenobi")
      .setEmail("obiwan.kenobi@starwars.com")
      .setLocal(true)
      .setExternalLogin("obiwan")
      .setExternalIdentityProvider("sonarqube")
      .setScmAccounts(newArrayList("obiwan:github", "obiwan:bitbucket"))
      .setOnboarded(false));
    userSession.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response)
      .extracting(CurrentWsResponse::getIsLoggedIn, CurrentWsResponse::getLogin, CurrentWsResponse::getName, CurrentWsResponse::getEmail, CurrentWsResponse::getAvatar,
        CurrentWsResponse::getLocal,
        CurrentWsResponse::getExternalIdentity, CurrentWsResponse::getExternalProvider, CurrentWsResponse::getScmAccountsList, CurrentWsResponse::getShowOnboardingTutorial)
      .containsExactly(true, "obiwan.kenobi", "Obiwan Kenobi", "obiwan.kenobi@starwars.com", "f5aa64437a1821ffe8b563099d506aef", true, "obiwan", "sonarqube",
        newArrayList("obiwan:github", "obiwan:bitbucket"), true);
  }

  @Test
  public void return_minimal_user_info() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("obiwan.kenobi")
      .setName("Obiwan Kenobi")
      .setEmail(null)
      .setLocal(true)
      .setExternalLogin("obiwan")
      .setExternalIdentityProvider("sonarqube")
      .setScmAccounts((String) null));
    userSession.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response)
      .extracting(CurrentWsResponse::getIsLoggedIn, CurrentWsResponse::getLogin, CurrentWsResponse::getName, CurrentWsResponse::hasAvatar, CurrentWsResponse::getLocal,
        CurrentWsResponse::getExternalIdentity, CurrentWsResponse::getExternalProvider, CurrentWsResponse::getSettingsList)
      .containsExactly(true, "obiwan.kenobi", "Obiwan Kenobi", false, true, "obiwan", "sonarqube", Collections.emptyList());
    assertThat(response.hasEmail()).isFalse();
    assertThat(response.getScmAccountsList()).isEmpty();
    assertThat(response.getGroupsList()).isEmpty();
    assertThat(response.getPermissions().getGlobalList()).isEmpty();
  }

  @Test
  public void convert_empty_email_to_null() {
    UserDto user = db.users().insertUser(u -> u
      .setLogin("obiwan.kenobi")
      .setEmail(""));
    userSession.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.hasEmail()).isFalse();
  }

  @Test
  public void return_group_membership() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    db.users().insertMember(db.users().insertGroup(newGroupDto().setName("Jedi")), user);
    db.users().insertMember(db.users().insertGroup(newGroupDto().setName("Rebel")), user);

    CurrentWsResponse response = call();

    assertThat(response.getGroupsList()).containsOnly("Jedi", "Rebel");
  }

  @Test
  public void return_permissions() {
    UserDto user = db.users().insertUser();
    userSession
      .logIn(user)
      // permissions on default organization
      .addPermission(SCAN, db.getDefaultOrganization())
      .addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization())
      // permissions on other organizations are ignored
      .addPermission(ADMINISTER, db.organizations().insert());

    CurrentWsResponse response = call();

    assertThat(response.getPermissions().getGlobalList()).containsOnly("profileadmin", "scan");
  }

  @Test
  public void return_user_settings() {
    UserDto user = db.users().insertUser();
    db.users().insertUserSetting(user, userSetting -> userSetting
      .setKey("notifications.readDate")
      .setValue("1234"));
    db.users().insertUserSetting(user, userSetting -> userSetting
      .setKey("notifications.optOut")
      .setValue("true"));
    db.commit();
    userSession.logIn(user);

    CurrentWsResponse response = call();

    assertThat(response.getSettingsList())
      .extracting(CurrentWsResponse.Setting::getKey, CurrentWsResponse.Setting::getValue)
      .containsExactlyInAnyOrder(
        tuple("notifications.optOut", "true"),
        tuple("notifications.readDate", "1234"));
  }

  @Test
  public void fail_with_ISE_when_user_login_in_db_does_not_exist() {
    db.users().insertUser(usert -> usert.setLogin("another"));
    userSession.logIn("obiwan.kenobi");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("User login 'obiwan.kenobi' cannot be found");

    call();
  }

  @Test
  public void anonymous() {
    userSession
      .anonymous()
      .addPermission(SCAN, db.getDefaultOrganization())
      .addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());

    CurrentWsResponse response = call();

    assertThat(response.getIsLoggedIn()).isFalse();
    assertThat(response.getPermissions().getGlobalList()).containsOnly("scan", "provisioning");
    assertThat(response)
      .extracting(CurrentWsResponse::hasLogin, CurrentWsResponse::hasName, CurrentWsResponse::hasEmail, CurrentWsResponse::hasLocal,
        CurrentWsResponse::hasExternalIdentity, CurrentWsResponse::hasExternalProvider)
      .containsOnly(false);
    assertThat(response.getScmAccountsList()).isEmpty();
    assertThat(response.getGroupsList()).isEmpty();
  }

  @Test
  public void json_example() {
    ComponentDto componentDto = db.components().insertPrivateProject(u -> u.setUuid("UUID-of-the-death-star"), u -> u.setDbKey("death-star-key"));
    UserDto obiwan = db.users().insertUser(user -> user
      .setLogin("obiwan.kenobi")
      .setName("Obiwan Kenobi")
      .setEmail("obiwan.kenobi@starwars.com")
      .setLocal(true)
      .setExternalLogin("obiwan.kenobi")
      .setExternalIdentityProvider("sonarqube")
      .setScmAccounts(newArrayList("obiwan:github", "obiwan:bitbucket"))
      .setOnboarded(true)
      .setHomepageType("PROJECT")
      .setHomepageParameter("UUID-of-the-death-star"));
    userSession
      .logIn(obiwan)
      .addPermission(SCAN, db.getDefaultOrganization())
      .addPermission(ADMINISTER_QUALITY_PROFILES, db.getDefaultOrganization())
      .addProjectPermission(USER, componentDto);
    db.users().insertMember(db.users().insertGroup(newGroupDto().setName("Jedi")), obiwan);
    db.users().insertMember(db.users().insertGroup(newGroupDto().setName("Rebel")), obiwan);

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("current-example.json"));
  }

  @Test
  public void test_definition() {
    WebService.Action definition = ws.getDef();
    assertThat(definition.key()).isEqualTo("current");
    assertThat(definition.description()).isEqualTo("Get the details of the current authenticated user.");
    assertThat(definition.since()).isEqualTo("5.2");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).isEmpty();
    assertThat(definition.changelog()).hasSize(2);
  }

  private CurrentWsResponse call() {
    return ws.newRequest().executeProtobuf(CurrentWsResponse.class);
  }

}
