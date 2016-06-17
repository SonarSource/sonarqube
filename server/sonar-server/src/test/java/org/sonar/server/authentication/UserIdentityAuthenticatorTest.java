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
package org.sonar.server.authentication;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexer;

public class UserIdentityAuthenticatorTest {

  static String USER_LOGIN = "github-johndoo";

  static String DEFAULT_GROUP = "default";

  static UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setProviderLogin("johndoo")
    .setLogin(USER_LOGIN)
    .setName("John")
    .setEmail("john@email.com")
    .build();

  static TestIdentityProvider IDENTITY_PROVIDER = new TestIdentityProvider()
    .setKey("github")
    .setEnabled(true)
    .setAllowsUsersToSignUp(true);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  DbClient dbClient = dbTester.getDbClient();
  DbSession dbSession = dbTester.getSession();
  UserDao userDao = dbClient.userDao();
  GroupDao groupDao = dbClient.groupDao();
  Settings settings = new Settings();

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);

  UserUpdater userUpdater = new UserUpdater(
    mock(NewUserNotifier.class),
    settings,
    dbClient,
    mock(UserIndexer.class),
    system2);

  UserIdentityAuthenticator underTest = new UserIdentityAuthenticator(dbClient, userUpdater);

  @Before
  public void setUp() throws Exception {
    settings.setProperty("sonar.defaultGroup", DEFAULT_GROUP);
    addGroup(DEFAULT_GROUP);
  }

  @Test
  public void authenticate_new_user() throws Exception {
    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER);
    dbSession.commit();

    UserDto userDto = userDao.selectByLogin(dbSession, USER_LOGIN);
    assertThat(userDto).isNotNull();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo("John");
    assertThat(userDto.getEmail()).isEqualTo("john@email.com");
    assertThat(userDto.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("github");

    verifyUserGroups(USER_LOGIN, DEFAULT_GROUP);
  }

  @Test
  public void authenticate_new_user_with_groups() throws Exception {
    addGroup("group1");
    addGroup("group2");

    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(USER_LOGIN)
      .setName("John")
      // group3 doesn't exist in db, it will be ignored
      .setGroups(newHashSet("group1", "group2", "group3"))
      .build(), IDENTITY_PROVIDER);
    dbSession.commit();

    UserDto userDto = userDao.selectByLogin(dbSession, USER_LOGIN);
    assertThat(userDto).isNotNull();

    verifyUserGroups(USER_LOGIN, "group1", "group2");
  }

  @Test
  public void authenticate_existing_user() throws Exception {
    userDao.insert(dbSession, new UserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("Old name")
      .setEmail("Old email")
      .setExternalIdentity("old identity")
      .setExternalIdentityProvider("old provide"));
    dbSession.commit();

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER);
    dbSession.commit();

    UserDto userDto = userDao.selectByLogin(dbSession, USER_LOGIN);
    assertThat(userDto).isNotNull();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo("John");
    assertThat(userDto.getEmail()).isEqualTo("john@email.com");
    assertThat(userDto.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("github");
  }

  @Test
  public void authenticate_existing_disabled_user() throws Exception {
    userDao.insert(dbSession, new UserDto()
      .setLogin(USER_LOGIN)
      .setActive(false)
      .setName("Old name")
      .setEmail("Old email")
      .setExternalIdentity("old identity")
      .setExternalIdentityProvider("old provide"));
    dbSession.commit();

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER);
    dbSession.commit();

    UserDto userDto = userDao.selectByLogin(dbSession, USER_LOGIN);
    assertThat(userDto).isNotNull();
    assertThat(userDto.isActive()).isTrue();
    assertThat(userDto.getName()).isEqualTo("John");
    assertThat(userDto.getEmail()).isEqualTo("john@email.com");
    assertThat(userDto.getExternalIdentity()).isEqualTo("johndoo");
    assertThat(userDto.getExternalIdentityProvider()).isEqualTo("github");
  }

  @Test
  public void authenticate_existing_user_and_add_new_groups() throws Exception {
    userDao.insert(dbSession, new UserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John"));
    addGroup("group1");
    addGroup("group2");
    dbSession.commit();

    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(USER_LOGIN)
      .setName("John")
      // group3 doesn't exist in db, it will be ignored
      .setGroups(newHashSet("group1", "group2", "group3"))
      .build(), IDENTITY_PROVIDER);
    dbSession.commit();

    Set<String> userGroups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(USER_LOGIN)).get(USER_LOGIN));
    assertThat(userGroups).containsOnly("group1", "group2");
  }

  @Test
  public void authenticate_existing_user_and_remove_groups() throws Exception {
    UserDto user = new UserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John");
    userDao.insert(dbSession, user);

    GroupDto group1 = addGroup("group1");
    GroupDto group2 = addGroup("group2");
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(user.getId()).setGroupId(group1.getId()));
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(user.getId()).setGroupId(group2.getId()));
    dbSession.commit();

    Set<String> userGroups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(USER_LOGIN)).get(USER_LOGIN));
    assertThat(userGroups).containsOnly("group1", "group2");

    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(USER_LOGIN)
      .setName("John")
      // Only group1 is returned by the id provider => group2 will be removed
      .setGroups(newHashSet("group1"))
      .build(), IDENTITY_PROVIDER);
    dbSession.commit();

    verifyUserGroups(USER_LOGIN, "group1");
  }

  @Test
  public void authenticate_existing_user_and_remove_all_groups() throws Exception {
    UserDto user = new UserDto()
      .setLogin(USER_LOGIN)
      .setActive(true)
      .setName("John");
    userDao.insert(dbSession, user);

    GroupDto group1 = addGroup("group1");
    GroupDto group2 = addGroup("group2");
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(user.getId()).setGroupId(group1.getId()));
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(user.getId()).setGroupId(group2.getId()));
    dbSession.commit();

    Set<String> userGroups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(USER_LOGIN)).get(USER_LOGIN));
    assertThat(userGroups).containsOnly("group1", "group2");

    underTest.authenticate(UserIdentity.builder()
      .setProviderLogin("johndoo")
      .setLogin(USER_LOGIN)
      .setName("John")
      // No group => group1 and group2 will be removed
      .setGroups(Collections.<String>emptySet())
      .build(), IDENTITY_PROVIDER);
    dbSession.commit();

    verifyNoUserGroups(USER_LOGIN);
  }

  @Test
  public void fail_to_authenticate_new_user_when_allow_users_to_signup_is_false() throws Exception {
    TestIdentityProvider identityProvider = new TestIdentityProvider()
      .setKey("github")
      .setName("Github")
      .setEnabled(true)
      .setAllowsUsersToSignUp(false);

    thrown.expect(UnauthorizedException.class);
    thrown.expectMessage("'github' users are not allowed to sign up");
    underTest.authenticate(USER_IDENTITY, identityProvider);
  }

  @Test
  public void fail_to_authenticate_new_user_when_email_already_exists() throws Exception {
    UserDto userDto = UserTesting.newUserDto()
      .setLogin("Existing user with same email")
      .setActive(true)
      .setEmail("john@email.com");
    userDao.insert(dbSession, userDto);
    dbSession.commit();

    thrown.expect(UnauthorizedException.class);
    thrown.expectMessage("You can't sign up because email 'john@email.com' is already used by an existing user. " +
      "This means that you probably already registered with another account.");
    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER);
  }

  private void verifyUserGroups(String userLogin, String... groups) {
    Set<String> userGroups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(USER_LOGIN)).get(userLogin));
    assertThat(userGroups).containsOnly(groups);
  }

  private void verifyNoUserGroups(String userLogin) {
    Set<String> userGroups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(USER_LOGIN)).get(userLogin));
    assertThat(userGroups).isEmpty();
  }

  private GroupDto addGroup(String name) {
    GroupDto group = new GroupDto().setName(name);
    groupDao.insert(dbSession, group);
    dbSession.commit();
    return group;
  }
}
