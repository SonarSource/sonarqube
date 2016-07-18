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
package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.security.SecurityRealm;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.loadedtemplate.LoadedTemplateDto.ONE_SHOT_TASK_TYPE;

public class FeedUsersLocalStartupTaskTest {

  static final long NOW = 20000000L;
  static final long PAST = 10000000L;

  static final String USER1_LOGIN = "USER1";
  static final String USER2_LOGIN = "USER2";

  static final String REALM_NAME = "LDAP";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  System2 system2 = mock(System2.class);

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  DbClient dbClient = dbTester.getDbClient();

  DbSession dbSession = dbTester.getSession();

  UserDao userDao = dbClient.userDao();

  Settings settings = new Settings();

  FeedUsersLocalStartupTask underTest;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  public void set_user_local_when_id_provider_is_sonarqube_and_realm_exists_and_local_users_property_contains_user_login() throws Exception {
    initTaskWithRealm();
    settings.setProperty("sonar.security.realm", REALM_NAME);
    settings.setProperty("sonar.security.localUsers", USER1_LOGIN);
    UserDto user = addUser(USER1_LOGIN, false, "sonarqube");

    underTest.start();

    verifyUserIsUpdated(user.getId(), true);
    verifyLogAboutRemovalOfLocalUsersProperty();
  }

  @Test
  public void set_user_as_not_local_when_id_provider_is_sonarqube_and_ream_exists_and_no_local_users_property() throws Exception {
    initTaskWithRealm();
    settings.setProperty("sonar.security.realm", REALM_NAME);
    UserDto user = addUser(USER1_LOGIN, false, "sonarqube");

    underTest.start();

    verifyUserIsUpdated(user.getId(), false);
    verifyEmptyLog();
  }

  @Test
  public void set_user_as_not_local_when_id_provider_is_sonarqube_and_ream_exists_and_local_users_property_does_not_contain_user_login() throws Exception {
    initTaskWithRealm();
    settings.setProperty("sonar.security.realm", REALM_NAME);
    settings.setProperty("sonar.security.localUsers", USER2_LOGIN);
    UserDto user = addUser(USER1_LOGIN, true, "sonarqube");

    underTest.start();

    verifyUserIsUpdated(user.getId(), false);
    verifyLogAboutRemovalOfLocalUsersProperty();
  }

  @Test
  public void set_user_as_local_when_id_provider_is_sonarqube_and_no_realm() throws Exception {
    initTaskWithNoRealm();
    settings.setProperty("sonar.security.realm", (String) null);
    UserDto user = addUser(USER1_LOGIN, false, "sonarqube");

    underTest.start();

    verifyUserIsUpdated(user.getId(), true);
    verifyEmptyLog();
  }

  @Test
  public void set_user_as_not_local_when_external_identity_is_not_sonarqube() throws Exception {
    initTaskWithNoRealm();
    UserDto user = addUser(USER1_LOGIN, false, "github");

    underTest.start();

    verifyUserIsUpdated(user.getId(), false);
    verifyEmptyLog();
  }

  @Test
  public void does_not_update_removed_user() throws Exception {
    initTaskWithRealm();
    settings.setProperty("sonar.security.realm", REALM_NAME);

    UserDto user = UserTesting.newUserDto()
      .setLogin(USER1_LOGIN)
      .setActive(false)
      .setLocal(false)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST);
    userDao.insert(dbSession, user);
    dbSession.commit();

    underTest.start();

    UserDto userReloaded = userDao.selectUserById(dbSession, user.getId());
    assertThat(userReloaded.isLocal()).isFalse();
    assertThat(userReloaded.getUpdatedAt()).isEqualTo(PAST);
    verifyTaskIsRegistered();
  }

  @Test
  public void does_nothing_when_task_has_already_been_executed() throws Exception {
    initTaskWithRealm();
    settings.setProperty("sonar.security.realm", REALM_NAME);
    settings.setProperty("sonar.security.localUsers", USER1_LOGIN);
    UserDto user = addUser(USER1_LOGIN, false, "github");

    underTest.start();
    verifyLogAboutRemovalOfLocalUsersProperty();

    logTester.clear();
    UserDto userReloaded = userDao.selectUserById(dbSession, user.getId());
    assertThat(userReloaded.getUpdatedAt()).isEqualTo(NOW);
    verifyTaskIsRegistered();

    when(system2.now()).thenReturn(NOW + 1000L);

    underTest.start();
    userReloaded = userDao.selectUserById(dbSession, user.getId());
    assertThat(userReloaded.getUpdatedAt()).isEqualTo(NOW);
    verifyEmptyLog();
  }

  @Test
  public void fail_when_realm_found_but_no_configuration() throws Exception {
    initTask(createRealm("REALM1"), createRealm("REALM2"));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("External authentication plugin [REALM1, REALM2] has been found, but no related configuration has been set. " +
      "Either update your configuration or remove the plugin");
    underTest.start();
  }

  private UserDto addUser(String login, boolean local, String externalIdentityProvider) {
    UserDto user = UserTesting.newUserDto()
      .setLogin(login)
      .setActive(true)
      .setLocal(local)
      .setExternalIdentityProvider(externalIdentityProvider)
      .setExternalIdentity(login)
      .setCreatedAt(PAST)
      .setUpdatedAt(PAST);
    userDao.insert(dbSession, user);
    dbSession.commit();
    return user;
  }

  private void verifyUserIsUpdated(long userId, boolean expectedLocal) {
    UserDto userReloaded = userDao.selectUserById(dbSession, userId);
    assertThat(userReloaded.isLocal()).isEqualTo(expectedLocal);
    assertThat(userReloaded.getUpdatedAt()).isEqualTo(NOW);
    verifyTaskIsRegistered();
  }

  private void verifyTaskIsRegistered() {
    assertThat(dbClient.loadedTemplateDao().countByTypeAndKey(ONE_SHOT_TASK_TYPE, "UpdateUsersLocal")).isEqualTo(1);
  }

  private void verifyLogAboutRemovalOfLocalUsersProperty() {
    assertThat(logTester.logs(LoggerLevel.INFO)).containsOnly(
      "NOTE : The property 'sonar.security.localUsers' is now no more needed, you can safely remove it.");
  }

  private void verifyEmptyLog() {
    assertThat(logTester.logs(LoggerLevel.INFO)).isEmpty();
  }

  private void initTask(SecurityRealm... realms) {
    if (realms.length == 0) {
      underTest = new FeedUsersLocalStartupTask(system2, dbTester.getDbClient(), settings);
    } else {
      underTest = new FeedUsersLocalStartupTask(system2, dbTester.getDbClient(), settings, realms);
    }
  }

  private void initTaskWithRealm() {
    initTask(createRealm(REALM_NAME));
  }

  private void initTaskWithNoRealm() {
    initTask();
  }

  private static SecurityRealm createRealm(String name) {
    SecurityRealm realm = mock(SecurityRealm.class);
    when(realm.getName()).thenReturn(name);
    return realm;
  }
}
