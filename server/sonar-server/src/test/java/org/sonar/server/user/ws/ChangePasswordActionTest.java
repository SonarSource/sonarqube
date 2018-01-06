/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbTester;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.OrganizationCreation;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ChangePasswordActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(new MapSettings().asConfig()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().logIn();

  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();

  private UserUpdater userUpdater = new UserUpdater(mock(NewUserNotifier.class), db.getDbClient(), new UserIndexer(db.getDbClient(), esTester.client()),
    organizationFlags,
    TestDefaultOrganizationProvider.from(db),
    mock(OrganizationCreation.class),
    new DefaultGroupFinder(db.getDbClient()),
    new MapSettings().asConfig());

  private WsTester tester = new WsTester(new UsersWs(new ChangePasswordAction(db.getDbClient(), userUpdater, userSessionRule)));

  @Before
  public void setUp() {
    db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
  }

  @Test
  public void fail_on_missing_permission() throws Exception {
    createUser();
    userSessionRule.logIn("polop");

    expectedException.expect(ForbiddenException.class);
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .execute();
  }

  @Test
  public void fail_on_unknown_user() throws Exception {
    userSessionRule.logIn().setSystemAdministrator();

    expectedException.expect(NotFoundException.class);

    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "polop")
      .setParam("password", "polop")
      .execute();
  }

  @Test
  public void system_administrator_can_update_password_of_user() throws Exception {
    userSessionRule.logIn().setSystemAdministrator();
    createUser();
    String originalPassword = db.getDbClient().userDao().selectOrFailByLogin(db.getSession(), "john").getCryptedPassword();

    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute()
      .assertNoContent();

    String newPassword = db.getDbClient().userDao().selectOrFailByLogin(db.getSession(), "john").getCryptedPassword();
    assertThat(newPassword).isNotEqualTo(originalPassword);
  }

  @Test
  public void a_user_can_update_his_password() throws Exception {
    createUser();
    String originalPassword = db.getDbClient().userDao().selectOrFailByLogin(db.getSession(), "john").getCryptedPassword();

    userSessionRule.logIn("john");
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("previousPassword", "Valar Dohaeris")
      .setParam("password", "Valar Morghulis")
      .execute()
      .assertNoContent();

    String newPassword = db.getDbClient().userDao().selectOrFailByLogin(db.getSession(), "john").getCryptedPassword();
    assertThat(newPassword).isNotEqualTo(originalPassword);
  }

  @Test
  public void fail_to_update_password_on_self_without_old_password() throws Exception {
    createUser();
    userSessionRule.logIn("john");

    expectedException.expect(IllegalArgumentException.class);
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute();
  }

  @Test
  public void fail_to_update_password_on_self_with_bad_old_password() throws Exception {
    createUser();
    userSessionRule.logIn("john");

    expectedException.expect(IllegalArgumentException.class);
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("previousPassword", "I dunno")
      .setParam("password", "Valar Morghulis")
      .execute();
  }

  @Test
  public void fail_to_update_password_on_external_auth() throws Exception {
    userSessionRule.logIn().setSystemAdministrator();

    NewUser newUser = NewUser.builder()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts(newArrayList("jn"))
      .setExternalIdentity(new ExternalIdentity("gihhub", "john"))
      .build();
    userUpdater.createAndCommit(db.getSession(), newUser, u -> {
    });

    expectedException.expect(BadRequestException.class);
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute();
  }

  private void createUser() {
    userUpdater.createAndCommit(db.getSession(), NewUser.builder()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts(newArrayList("jn"))
      .setPassword("Valar Dohaeris")
      .build(), u -> {
      });
  }
}
