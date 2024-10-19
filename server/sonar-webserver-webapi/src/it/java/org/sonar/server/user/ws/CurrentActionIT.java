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

import java.util.Collection;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Users.CurrentWsResponse;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.server.user.ws.DismissNoticeAction.AVAILABLE_NOTICE_KEYS;
import static org.sonar.test.JsonAssert.assertJson;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  CurrentActionIT.OtherTest.class,
  CurrentActionIT.DimissableNoticeTest.class
})
public class CurrentActionIT {
  public static class OtherTest {
    @Rule
    public UserSessionRule userSession = UserSessionRule.standalone();
    @Rule
    public DbTester db = DbTester.create(System2.INSTANCE);

    private final PlatformEditionProvider platformEditionProvider = mock(PlatformEditionProvider.class);
    private final HomepageTypesImpl homepageTypes = new HomepageTypesImpl();
    private final PermissionService permissionService = new PermissionServiceImpl(new ResourceTypes(new ResourceTypeTree[] {
      ResourceTypeTree.builder().addType(ResourceType.builder(Qualifiers.PROJECT).build()).build()}));
    private final WsActionTester ws = new WsActionTester(
      new CurrentAction(userSession, db.getDbClient(), new AvatarResolverImpl(), homepageTypes, platformEditionProvider, permissionService));

    private CurrentWsResponse call() {
      return ws.newRequest().executeProtobuf(CurrentWsResponse.class);
    }

    @Test
    public void return_user_info() {
      UserDto user = db.users().insertUser(u -> u
        .setLogin("obiwan.kenobi")
        .setName("Obiwan Kenobi")
        .setEmail("obiwan.kenobi@starwars.com")
        .setLocal(true)
        .setExternalLogin("obiwan")
        .setExternalIdentityProvider("sonarqube")
        .setScmAccounts(newArrayList("obiwan:github", "obiwan:bitbucket")));
      userSession.logIn(user);

      CurrentWsResponse response = call();

      assertThat(response)
        .extracting(CurrentWsResponse::getIsLoggedIn, CurrentWsResponse::getLogin, CurrentWsResponse::getName, CurrentWsResponse::getEmail, CurrentWsResponse::getAvatar,
          CurrentWsResponse::getLocal,
          CurrentWsResponse::getExternalIdentity, CurrentWsResponse::getExternalProvider, CurrentWsResponse::getScmAccountsList)
        .containsExactly(true, "obiwan.kenobi", "Obiwan Kenobi", "obiwan.kenobi@starwars.com", "f5aa64437a1821ffe8b563099d506aef", true, "obiwan", "sonarqube",
          newArrayList("obiwan:bitbucket", "obiwan:github"));
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
        .setScmAccounts(emptyList()));
      userSession.logIn(user);

      CurrentWsResponse response = call();

      assertThat(response)
        .extracting(CurrentWsResponse::getIsLoggedIn, CurrentWsResponse::getLogin, CurrentWsResponse::getName, CurrentWsResponse::hasAvatar, CurrentWsResponse::getLocal,
          CurrentWsResponse::getExternalIdentity, CurrentWsResponse::getExternalProvider, CurrentWsResponse::getUsingSonarLintConnectedMode)
        .containsExactly(true, "obiwan.kenobi", "Obiwan Kenobi", false, true, "obiwan", "sonarqube", false);
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
        .addPermission(SCAN)
        .addPermission(ADMINISTER_QUALITY_PROFILES);

      CurrentWsResponse response = call();
      assertThat(response.getPermissions().getGlobalList()).containsOnly("profileadmin", "scan");
    }

    @Test
    public void fail_with_ISE_when_user_login_in_db_does_not_exist() {
      db.users().insertUser(usert -> usert.setLogin("another"));
      userSession.logIn("obiwan.kenobi");

      assertThatThrownBy(this::call)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("User login 'obiwan.kenobi' cannot be found");
    }

    @Test
    public void anonymous() {
      userSession
        .anonymous()
        .addPermission(SCAN)
        .addPermission(PROVISION_PROJECTS);

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
      ComponentDto componentDto = db.components().insertPrivateProject(u -> u.setUuid("UUID-of-the-death-star").setKey("death-star-key")).getMainBranchComponent();
      UserDto obiwan = db.users().insertUser(user -> user
        .setLogin("obiwan.kenobi")
        .setName("Obiwan Kenobi")
        .setEmail("obiwan.kenobi@starwars.com")
        .setLocal(true)
        .setExternalLogin("obiwan.kenobi")
        .setExternalIdentityProvider("sonarqube")
        .setScmAccounts(newArrayList("obiwan:github", "obiwan:bitbucket"))
        .setHomepageType("PROJECT")
        .setHomepageParameter("UUID-of-the-death-star"));
      userSession
        .logIn(obiwan)
        .addPermission(SCAN)
        .addPermission(ADMINISTER_QUALITY_PROFILES)
        .addProjectPermission(USER, db.components().getProjectDtoByMainBranch(componentDto));
      db.users().insertMember(db.users().insertGroup(newGroupDto().setName("Jedi")), obiwan);
      db.users().insertMember(db.users().insertGroup(newGroupDto().setName("Rebel")), obiwan);

      String response = ws.newRequest().execute().getInput();

      assertJson(response).isSimilarTo(getClass().getResource("current-example.json"));
    }

    @Test
    public void handle_givenSonarLintUserInDatabase_returnSonarLintUserFromTheEndpoint() {
      UserDto user = db.users().insertUser(u -> u.setLastSonarlintConnectionDate(System.currentTimeMillis()));
      userSession.logIn(user);

      CurrentWsResponse response = call();

      assertThat(response.getUsingSonarLintConnectedMode()).isTrue();
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
      assertThat(definition.changelog()).isNotEmpty();
    }
  }

  @RunWith(Parameterized.class)
  public static class DimissableNoticeTest {
    @Rule
    public UserSessionRule userSession = UserSessionRule.standalone();
    @Rule
    public DbTester db = DbTester.create(System2.INSTANCE);

    private final PlatformEditionProvider platformEditionProvider = mock(PlatformEditionProvider.class);
    private final HomepageTypesImpl homepageTypes = new HomepageTypesImpl();
    private final PermissionService permissionService = new PermissionServiceImpl(new ResourceTypes(new ResourceTypeTree[] {
      ResourceTypeTree.builder().addType(ResourceType.builder(Qualifiers.PROJECT).build()).build()}));
    private final WsActionTester ws = new WsActionTester(
      new CurrentAction(userSession, db.getDbClient(), new AvatarResolverImpl(), homepageTypes, platformEditionProvider, permissionService));

    private CurrentWsResponse call() {
      return ws.newRequest().executeProtobuf(CurrentWsResponse.class);
    }

    @Parameterized.Parameters
    public static Collection<String> parameterCombination() {
      return AVAILABLE_NOTICE_KEYS;
    }

    private final String notice;

    public DimissableNoticeTest(String notice) {
      this.notice = notice;
    }

    @Test
    public void return_dismissed_notice() {
      UserDto user = db.users().insertUser();
      userSession.logIn(user);

      PropertyDto property = new PropertyDto().setUserUuid(user.getUuid()).setKey("user.dismissedNotices." + this.notice);
      db.properties().insertProperties(userSession.getLogin(), null, null, null, property);

      CurrentWsResponse response = call();

      assertThat(response.getDismissedNoticesMap().entrySet())
        .extracting(Map.Entry::getKey, Map.Entry::getValue)
        .contains(Tuple.tuple(this.notice, true));
    }

    @Test
    public void return_not_dismissed_notice() {
      UserDto user = db.users().insertUser();
      userSession.logIn(user);

      CurrentWsResponse response = call();

      assertThat(response.getDismissedNoticesMap().entrySet())
        .extracting(Map.Entry::getKey, Map.Entry::getValue)
        .contains(Tuple.tuple(this.notice, false));
    }
  }
}
