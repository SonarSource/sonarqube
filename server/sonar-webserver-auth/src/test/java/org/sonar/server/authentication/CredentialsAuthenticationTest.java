/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC_TOKEN;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class CredentialsAuthenticationTest {

  private static final String LOGIN = "LOGIN";
  private static final String PASSWORD = "PASSWORD";
  private static final String SALT = "0242b0b4c0a93ddfe09dd886de50bc25ba000b51";
  private static final String ENCRYPTED_PASSWORD = "540e4fc4be4e047db995bc76d18374a5b5db08cc";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private HttpServletRequest request = mock(HttpServletRequest.class);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private CredentialsExternalAuthentication externalAuthentication = mock(CredentialsExternalAuthentication.class);
  private CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(dbClient, settings.asConfig());
  private CredentialsAuthentication underTest = new CredentialsAuthentication(dbClient, authenticationEvent, externalAuthentication, localAuthentication);

  @Test
  public void authenticate_local_user() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(ENCRYPTED_PASSWORD)
      .setHashMethod(CredentialsLocalAuthentication.HashMethod.SHA1.name())
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
      .setHashMethod(CredentialsLocalAuthentication.HashMethod.SHA1.name())
      .setLocal(true));

    assertThatThrownBy(() -> executeAuthenticate(BASIC))
      .hasMessage("wrong password")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(BASIC))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyZeroInteractions(authenticationEvent);

  }

  @Test
  public void authenticate_external_user() {
    when(externalAuthentication.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC)).thenReturn(Optional.of(newUserDto()));
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setLocal(false));

    executeAuthenticate(BASIC);

    verify(externalAuthentication).authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_authenticate_external_user_when_no_external_authentication() {
    when(externalAuthentication.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC_TOKEN)).thenReturn(Optional.empty());
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setLocal(false));

    assertThatThrownBy(() -> executeAuthenticate(BASIC_TOKEN))
      .hasMessage("User is not local")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(BASIC_TOKEN))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_local_user_that_have_no_password() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(null)
      .setSalt(SALT)
      .setHashMethod(CredentialsLocalAuthentication.HashMethod.SHA1.name())
      .setLocal(true));

    assertThatThrownBy(() -> executeAuthenticate(BASIC))
      .hasMessage("null password in DB")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(BASIC))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_local_user_that_have_no_salt() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(ENCRYPTED_PASSWORD)
      .setSalt(null)
      .setHashMethod(CredentialsLocalAuthentication.HashMethod.SHA1.name())
      .setLocal(true));

    assertThatThrownBy(() -> executeAuthenticate(BASIC_TOKEN))
      .hasMessage("null salt")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(BASIC_TOKEN))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyZeroInteractions(authenticationEvent);

  }

  private UserDto executeAuthenticate(AuthenticationEvent.Method method) {
    return underTest.authenticate(new Credentials(LOGIN, PASSWORD), request, method);
  }

  private UserDto insertUser(UserDto userDto) {
    dbClient.userDao().insert(dbSession, userDto);
    dbSession.commit();
    return userDto;
  }
}
