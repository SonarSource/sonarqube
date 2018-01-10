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

import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationCreation;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UpdateActionTest {

  private static final OrganizationCreation ORGANIZATION_CREATION_NOT_USED_FOR_UPDATE = null;
  private MapSettings settings = new MapSettings();
  private System2 system2 = new System2();

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = new EsTester(new UserIndexDefinition(settings.asConfig()));
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private UserIndexer userIndexer = new UserIndexer(dbClient, es.client());
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();

  private WsActionTester ws = new WsActionTester(new UpdateAction(
    new UserUpdater(mock(NewUserNotifier.class), dbClient, userIndexer, organizationFlags, defaultOrganizationProvider, ORGANIZATION_CREATION_NOT_USED_FOR_UPDATE,
      new DefaultGroupFinder(db.getDbClient()), settings.asConfig()), userSession, new UserJsonWriter(userSession), dbClient));

  @Before
  public void setUp() {
    db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
  }

  @Test
  public void update_user() throws Exception {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setParam("name", "Jon Snow")
      .setParam("email", "jon.snow@thegreatw.all")
      .setParam("scmAccounts", "jon.snow")
      .execute()
      .assertJson(getClass(), "update_user.json");
  }

  @Test
  public void update_only_name() throws Exception {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setParam("name", "Jon Snow")
      .execute()
      .assertJson(getClass(), "update_name.json");
  }

  @Test
  public void update_only_email() throws Exception {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setParam("email", "jon.snow@thegreatw.all")
      .execute()
      .assertJson(getClass(), "update_email.json");
  }

  @Test
  public void blank_email_is_updated_to_null() throws Exception {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setParam("email", "")
      .execute()
      .assertJson(getClass(), "blank_email_is_updated_to_null.json");

    UserDto userDto = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(userDto.getEmail()).isNull();
  }

  @Test
  public void remove_scm_accounts() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setMultiParam("scmAccount", singletonList(""))
      .execute();

    UserDto userDto = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(userDto.getScmAccounts()).isNull();
  }

  @Test
  public void update_only_scm_accounts() throws Exception {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setMultiParam("scmAccount", singletonList("jon.snow"))
      .execute()
      .assertJson(getClass(), "update_scm_accounts.json");

    UserDto user = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(user.getScmAccountsAsList()).containsOnly("jon.snow");
  }

  @Test
  public void update_scm_account_having_coma() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setMultiParam("scmAccount", singletonList("jon,snow"))
      .execute();

    UserDto user = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(user.getScmAccountsAsList()).containsOnly("jon,snow");
  }

  @Test
  public void update_scm_account_ignores_duplicates() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setMultiParam("scmAccount", Arrays.asList("jon.snow", "jon.snow", "jon.jon", "jon.snow"))
      .execute();

    UserDto user = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(user.getScmAccountsAsList()).containsExactlyInAnyOrder("jon.jon", "jon.snow");
  }

  @Test
  public void update_scm_account_ordered_case_insensitive() {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setMultiParam("scmAccount", Arrays.asList("jon.3", "Jon.1", "JON.2"))
      .execute();

    UserDto user = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(user.getScmAccountsAsList()).containsExactly("Jon.1", "JON.2", "jon.3");
  }

  @Test
  public void update_only_scm_accounts_with_deprecated_scmAccounts_parameter() throws Exception {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setParam("scmAccounts", "jon.snow")
      .execute()
      .assertJson(getClass(), "update_scm_accounts.json");

    UserDto user = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(user.getScmAccountsAsList()).containsOnly("jon.snow");
  }

  @Test
  public void update_only_scm_accounts_with_deprecated_scm_accounts_parameter() throws Exception {
    createUser();

    ws.newRequest()
      .setParam("login", "john")
      .setParam("scm_accounts", "jon.snow")
      .execute()
      .assertJson(getClass(), "update_scm_accounts.json");

    UserDto user = dbClient.userDao().selectByLogin(dbSession, "john");
    assertThat(user.getScmAccountsAsList()).containsOnly("jon.snow");
  }

  @Test
  public void fail_on_missing_permission() {
    createUser();
    userSession.logIn("polop");

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("login", "john")
      .execute();
  }

  @Test
  public void fail_on_unknown_user() {
    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("login", "john")
      .execute();
  }

  @Test
  public void fail_on_invalid_email() {
    createUser();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Email 'invalid-email' is not valid");

    ws.newRequest()
      .setParam("login", "john")
      .setParam("email", "invalid-email")
      .execute();
  }

  private void createUser() {
    UserDto userDto = newUserDto()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts(newArrayList("jn"))
      .setActive(true)
      .setLocal(true)
      .setExternalIdentity("jo")
      .setExternalIdentityProvider("sonarqube");
    dbClient.userDao().insert(dbSession, userDto);
    userIndexer.commitAndIndex(dbSession, userDto);
  }
}
