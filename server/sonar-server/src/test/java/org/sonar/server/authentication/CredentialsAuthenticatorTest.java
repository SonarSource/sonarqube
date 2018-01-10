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
package org.sonar.server.authentication;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC_TOKEN;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

public class CredentialsAuthenticatorTest {

  private static final String LOGIN = "LOGIN";
  private static final String PASSWORD = "PASSWORD";
  private static final String SALT = "0242b0b4c0a93ddfe09dd886de50bc25ba000b51";
  private static final String CRYPTED_PASSWORD = "540e4fc4be4e047db995bc76d18374a5b5db08cc";

  @Rule
  public ExpectedException expectedException = none();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();

  private DbSession dbSession = dbTester.getSession();

  private RealmAuthenticator externalAuthenticator = mock(RealmAuthenticator.class);
  private HttpServletRequest request = mock(HttpServletRequest.class);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);

  private CredentialsAuthenticator underTest = new CredentialsAuthenticator(dbClient, externalAuthenticator, authenticationEvent);

  @Test
  public void authenticate_local_user() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(CRYPTED_PASSWORD)
      .setSalt(SALT)
      .setLocal(true));

    UserDto userDto = executeAuthenticate(BASIC);
    assertThat(userDto.getLogin()).isEqualTo(LOGIN);
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.local(BASIC));
  }

  @Test
  public void fail_to_authenticate_local_user_when_password_is_wrong() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword("Wrong password")
      .setSalt("Wrong salt")
      .setLocal(true));

    expectedException.expect(authenticationException().from(Source.local(BASIC)).withLogin(LOGIN).andNoPublicMessage());
    expectedException.expectMessage("wrong password");
    try {
      executeAuthenticate(BASIC);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void authenticate_external_user() {
    when(externalAuthenticator.authenticate(LOGIN, PASSWORD, request, BASIC)).thenReturn(Optional.of(newUserDto()));
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setLocal(false));

    executeAuthenticate(BASIC);

    verify(externalAuthenticator).authenticate(LOGIN, PASSWORD, request, BASIC);
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_authenticate_external_user_when_no_external_authentication() {
    when(externalAuthenticator.authenticate(LOGIN, PASSWORD, request, BASIC_TOKEN)).thenReturn(Optional.empty());
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setLocal(false));

    expectedException.expect(authenticationException().from(Source.local(BASIC_TOKEN)).withLogin(LOGIN).andNoPublicMessage());
    expectedException.expectMessage("User is not local");
    try {
      executeAuthenticate(BASIC_TOKEN);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void fail_to_authenticate_local_user_that_have_no_password() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(null)
      .setSalt(SALT)
      .setLocal(true));

    expectedException.expect(authenticationException().from(Source.local(BASIC)).withLogin(LOGIN).andNoPublicMessage());
    expectedException.expectMessage("null password in DB");
    try {
      executeAuthenticate(BASIC);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void fail_to_authenticate_local_user_that_have_no_salt() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(CRYPTED_PASSWORD)
      .setSalt(null)
      .setLocal(true));

    expectedException.expect(authenticationException().from(Source.local(BASIC_TOKEN)).withLogin(LOGIN).andNoPublicMessage());
    expectedException.expectMessage("null salt");
    try {
      executeAuthenticate(BASIC_TOKEN);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  private UserDto executeAuthenticate(AuthenticationEvent.Method method) {
    return underTest.authenticate(LOGIN, PASSWORD, request, method);
  }

  private UserDto insertUser(UserDto userDto) {
    dbClient.userDao().insert(dbSession, userDto);
    dbSession.commit();
    return userDto;
  }
}
