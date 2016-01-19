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

import com.google.common.base.Optional;
import javax.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.core.util.UuidFactory;
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

  static String USER_LOGIN = "ABCD";
  static UserDto ACTIVE_USER = new UserDto().setId(10L).setLogin(USER_LOGIN).setActive(true);
  static UserDto UNACTIVE_USER = new UserDto().setId(11L).setLogin("UNACTIVE").setActive(false);

  static UserIdentity USER_IDENTITY = UserIdentity.builder()
    .setId("johndoo")
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
  UuidFactory uuidFactory = mock(UuidFactory.class);

  UserIdentityAuthenticator underTest = new UserIdentityAuthenticator(dbClient, userUpdater, uuidFactory);

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.userDao()).thenReturn(userDao);
    when(uuidFactory.create()).thenReturn(USER_LOGIN);
  }

  @Test
  public void authenticate_new_user() throws Exception {
    when(userDao.selectByExternalIdentity(dbSession, USER_IDENTITY.getId(), IDENTITY_PROVIDER.getKey())).thenReturn(Optional.<UserDto>absent());
    when(userDao.selectOrFailByExternalIdentity(dbSession, USER_IDENTITY.getId(), IDENTITY_PROVIDER.getKey())).thenReturn(ACTIVE_USER);

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
    when(userDao.selectByExternalIdentity(dbSession, USER_IDENTITY.getId(), IDENTITY_PROVIDER.getKey())).thenReturn(Optional.of(ACTIVE_USER));

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, httpSession);

    ArgumentCaptor<UpdateUser> updateUserArgumentCaptor = ArgumentCaptor.forClass(UpdateUser.class);
    verify(userUpdater).update(eq(dbSession), updateUserArgumentCaptor.capture());
    UpdateUser newUser = updateUserArgumentCaptor.getValue();

    assertThat(newUser.login()).isEqualTo(USER_LOGIN);
    assertThat(newUser.name()).isEqualTo("John");
    assertThat(newUser.email()).isEqualTo("john@email.com");
  }

  @Test
  public void authenticate_existing_disabled_user() throws Exception {
    when(userDao.selectByExternalIdentity(dbSession, USER_IDENTITY.getId(), IDENTITY_PROVIDER.getKey())).thenReturn(Optional.of(UNACTIVE_USER));
    when(userDao.selectOrFailByExternalIdentity(dbSession, USER_IDENTITY.getId(), IDENTITY_PROVIDER.getKey())).thenReturn(UNACTIVE_USER);

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, httpSession);

    ArgumentCaptor<NewUser> newUserArgumentCaptor = ArgumentCaptor.forClass(NewUser.class);
    verify(userUpdater).create(eq(dbSession), newUserArgumentCaptor.capture());
  }

  @Test
  public void update_session_for_rails() throws Exception {
    when(userDao.selectByExternalIdentity(dbSession, USER_IDENTITY.getId(), IDENTITY_PROVIDER.getKey())).thenReturn(Optional.of(ACTIVE_USER));

    underTest.authenticate(USER_IDENTITY, IDENTITY_PROVIDER, httpSession);

    verify(httpSession).setAttribute("user_id", ACTIVE_USER.getId());
  }

  @Test
  public void fail_to_authenticate_new_user_when_allow_users_to_signup_is_false() throws Exception {
    when(userDao.selectByExternalIdentity(dbSession, USER_IDENTITY.getId(), IDENTITY_PROVIDER.getKey())).thenReturn(Optional.<UserDto>absent());
    when(userDao.selectOrFailByExternalIdentity(dbSession, USER_IDENTITY.getId(), IDENTITY_PROVIDER.getKey())).thenReturn(ACTIVE_USER);

    TestIdentityProvider identityProvider = new TestIdentityProvider()
      .setKey("github")
      .setName("Github")
      .setEnabled(true)
      .setAllowsUsersToSignUp(false);

    thrown.expect(NotAllowUserToSignUpException.class);
    underTest.authenticate(USER_IDENTITY, identityProvider, httpSession);
  }
}
