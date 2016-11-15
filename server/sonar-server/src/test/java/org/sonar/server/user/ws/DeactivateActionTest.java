/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.db.user.UserTokenTesting.newUserToken;
import static org.sonar.test.JsonAssert.assertJson;

public class DeactivateActionTest {

  private System2 system2 = AlwaysIncreasingSystem2.INSTANCE;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private WsActionTester ws;
  private UserIndex index;
  private DbClient dbClient = db.getDbClient();
  private UserIndexer userIndexer;
  private DbSession dbSession = db.getSession();

  @Before
  public void setUp() {
    userIndexer = new UserIndexer(system2, dbClient, esTester.client());
    index = new UserIndex(esTester.client());
    userIndexer = new UserIndexer(system2, dbClient, esTester.client());
    ws = new WsActionTester(new DeactivateAction(
      dbClient, userIndexer, userSession, new UserJsonWriter(userSession), defaultOrganizationProvider));
  }

  @Test
  public void test_definition() {
    assertThat(ws.getDef().isPost()).isTrue();
    assertThat(ws.getDef().isInternal()).isFalse();
    assertThat(ws.getDef().params()).hasSize(1);
  }

  @Test
  public void deactivate_user_and_delete_his_related_data() throws Exception {
    UserDto user = insertUser(newUserDto()
      .setLogin("ada.lovelace")
      .setEmail("ada.lovelace@noteg.com")
      .setName("Ada Lovelace")
      .setScmAccounts(singletonList("al")));
    loginAsAdmin();

    String json = deactivate(user.getLogin()).getInput();

    // scm accounts, groups and email are deleted
    assertJson(json).isSimilarTo(ws.getDef().responseExampleAsString());

    assertThat(index.getNullableByLogin(user.getLogin()).active()).isFalse();
    verifyThatUserIsDeactivated(user.getLogin());
    assertThat(dbClient.userTokenDao().selectByLogin(dbSession, user.getLogin())).isEmpty();
    assertThat(dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setUserId(user.getId().intValue()).build(), dbSession)).isEmpty();
  }

  @Test
  public void cannot_deactivate_self() throws Exception {
    UserDto user = createUser();
    userSession.login(user.getLogin()).setGlobalPermissions(SYSTEM_ADMIN);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Self-deactivation is not possible");

    deactivate(user.getLogin());

    verifyThatUserExists(user.getLogin());
  }

  @Test
  public void deactivation_requires_to_be_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    deactivate("someone");
  }

  @Test
  public void deactivation_requires_administrator_permission() throws Exception {
    userSession.login();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    deactivate("someone");
  }

  @Test
  public void fail_if_user_does_not_exist() throws Exception {
    loginAsAdmin();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User 'someone' doesn't exist");

    deactivate("someone");
  }

  @Test
  public void fail_if_login_is_blank() throws Exception {
    loginAsAdmin();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User '' doesn't exist");

    deactivate("");
  }

  @Test
  public void fail_to_deactivate_last_administrator_of_default_organization() throws Exception {
    UserDto admin = createUser();
    db.users().insertPermissionOnUser(admin, SYSTEM_ADMIN);
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("User is last administrator, and cannot be deactivated");

    deactivate(admin.getLogin());
  }

  @Test
  public void fail_to_deactivate_last_administrator_of_organization() throws Exception {
    // user1 is the unique administrator of org1 and org2.
    // user1 and user2 are both administrators of org3
    UserDto user1 = createUser();
    OrganizationDto org1 = db.organizations().insert(newOrganizationDto().setKey("org1"));
    OrganizationDto org2 = db.organizations().insert(newOrganizationDto().setKey("org2"));
    OrganizationDto org3 = db.organizations().insert(newOrganizationDto().setKey("org3"));
    db.users().insertPermissionOnUser(org1, user1, SYSTEM_ADMIN);
    db.users().insertPermissionOnUser(org2, user1, SYSTEM_ADMIN);
    db.users().insertPermissionOnUser(org3, user1, SYSTEM_ADMIN);
    UserDto user2 = createUser();
    db.users().insertPermissionOnUser(org3, user2, SYSTEM_ADMIN);
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("User is last administrator of organizations [org1, org2], and cannot be deactivated");

    deactivate(user1.getLogin());
  }

  @Test
  public void administrators_can_be_deactivated_if_there_are_still_other_administrators() throws Exception {
    UserDto admin = createUser();
    UserDto anotherAdmin = createUser();
    db.users().insertPermissionOnUser(admin, SYSTEM_ADMIN);
    db.users().insertPermissionOnUser(anotherAdmin, SYSTEM_ADMIN);
    db.commit();
    loginAsAdmin();

    deactivate(admin.getLogin());

    verifyThatUserIsDeactivated(admin.getLogin());
    verifyThatUserExists(anotherAdmin.getLogin());
  }

  private UserDto createUser() {
    return insertUser(newUserDto());
  }

  private UserDto insertUser(UserDto user) {
    user
      .setCreatedAt(system2.now())
      .setUpdatedAt(system2.now());
    dbClient.userDao().insert(dbSession, user);
    dbClient.userTokenDao().insert(dbSession, newUserToken().setLogin(user.getLogin()));
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setUserId(user.getId()).setKey("foo").setValue("bar"));
    dbSession.commit();
    userIndexer.index();
    return user;
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

  private TestResponse deactivate(String login) {
    return ws.newRequest()
      .setMethod("POST")
      .setParam("login", login)
      .execute();
  }

  private void verifyThatUserExists(String login) {
    assertThat(db.users().selectUserByLogin(login)).isPresent();
  }

  private void verifyThatUserIsDeactivated(String login) {
    Optional<UserDto> user = db.users().selectUserByLogin(login);
    assertThat(user).isPresent();
    assertThat(user.get().isActive()).isFalse();
    assertThat(user.get().getEmail()).isNull();
    assertThat(user.get().getScmAccountsAsList()).isEmpty();
  }
}
