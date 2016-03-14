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

import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserIdentityAuthenticatorTest {

  static String USER_LOGIN = "github-johndoo";
  static UserDto ACTIVE_USER = new UserDto().setId(10L).setLogin(USER_LOGIN).setActive(true);
  static UserDto UNACTIVE_USER = new UserDto().setId(11L).setLogin("UNACTIVE").setActive(false);

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

  DbClient dbClient = mock(DbClient.class);
  DbSession dbSession = mock(DbSession.class);
  UserDao userDao = mock(UserDao.class);

  HttpSession httpSession = mock(HttpSession.class);
  UserUpdater userUpdater = mock(UserUpdater.class);

  UserIdentityAuthenticator underTest = new UserIdentityAuthenticator(dbClient, userUpdater);

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.userDao()).thenReturn(userDao);
  }

  @Test
  public void authenticate_new_user() throws Exception {
    when(userDao.selectByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(null);
    when(userDao.selectOrFailByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(ACTIVE_USER);

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, httpSession);

    ArgumentCaptor<NewUser> newUserArgumentCaptor = ArgumentCaptor.forClass(NewUser.class);
    verify(userUpdater).create(eq(dbSession), newUserArgumentCaptor.capture());
    NewUser newUser = newUserArgumentCaptor.getValue();

    assertThat(newUser.login()).isEqualTo(USER_LOGIN);
    assertThat(newUser.name()).isEqualTo("John");
    assertThat(newUser.email()).isEqualTo("john@email.com");
    assertThat(newUser.externalIdentity().getProvider()).isEqualTo("github");
    assertThat(newUser.externalIdentity().getId()).isEqualTo("johndoo");
  }

  @Test
  public void authenticate_existing_user() throws Exception {
    when(userDao.selectByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(ACTIVE_USER);

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, httpSession);

    ArgumentCaptor<UpdateUser> updateUserArgumentCaptor = ArgumentCaptor.forClass(UpdateUser.class);
    verify(userUpdater).update(eq(dbSession), updateUserArgumentCaptor.capture());
    UpdateUser updateUser = updateUserArgumentCaptor.getValue();

    assertThat(updateUser.login()).isEqualTo(USER_LOGIN);
    assertThat(updateUser.name()).isEqualTo("John");
    assertThat(updateUser.email()).isEqualTo("john@email.com");
    assertThat(updateUser.externalIdentity().getProvider()).isEqualTo("github");
    assertThat(updateUser.externalIdentity().getId()).isEqualTo("johndoo");
    assertThat(updateUser.isPasswordChanged()).isTrue();
    assertThat(updateUser.password()).isNull();
  }

  @Test
  public void authenticate_existing_disabled_user() throws Exception {
    when(userDao.selectByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(UNACTIVE_USER);
    when(userDao.selectOrFailByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(UNACTIVE_USER);

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, httpSession);

    ArgumentCaptor<NewUser> newUserArgumentCaptor = ArgumentCaptor.forClass(NewUser.class);
    verify(userUpdater).create(eq(dbSession), newUserArgumentCaptor.capture());
  }

  @Test
  public void update_session_for_rails() throws Exception {
    when(userDao.selectByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(ACTIVE_USER);

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, httpSession);

    verify(httpSession).setAttribute("user_id", ACTIVE_USER.getId());
  }

  @Test
  public void fail_to_authenticate_new_user_when_allow_users_to_signup_is_false() throws Exception {
    when(userDao.selectByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(null);
    when(userDao.selectOrFailByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(ACTIVE_USER);

    TestIdentityProvider identityProvider = new TestIdentityProvider()
      .setKey("github")
      .setName("Github")
      .setEnabled(true)
      .setAllowsUsersToSignUp(false);

    thrown.expect(UnauthorizedException.class);
    thrown.expectMessage("'github' users are not allowed to sign up");
    underTest.authenticate(USER_IDENTITY, identityProvider, httpSession);
  }

  @Test
  public void fail_to_authenticate_new_user_when_email_already_exists() throws Exception {
    when(userDao.selectByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(null);
    when(userDao.selectOrFailByLogin(dbSession, USER_IDENTITY.getLogin())).thenReturn(ACTIVE_USER);
    when(userDao.doesEmailExist(dbSession, USER_IDENTITY.getEmail())).thenReturn(true);

    thrown.expect(UnauthorizedException.class);
    thrown.expectMessage("You can't sign up because email 'john@email.com' is already used by an existing user. This means that you probably already registered with another account.");
    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, httpSession);
  }
}
