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
package org.sonar.server.authentication;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.CredentialsAuthentication.ERROR_PASSWORD_CANNOT_BE_NULL;
import static org.sonar.server.authentication.CredentialsLocalAuthentication.ERROR_UNKNOWN_HASH_METHOD;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.SONARQUBE_TOKEN;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class CredentialsAuthenticationIT {

  private static final String LOGIN = "LOGIN";
  private static final String PASSWORD = "PASSWORD";
  private static final String SALT = "0242b0b4c0a93ddfe09dd886de50bc25ba000b51";
  private static final int NUMBER_OF_PBKDF2_ITERATIONS = 1;
  private static final String ENCRYPTED_PASSWORD = format("%d$%s", NUMBER_OF_PBKDF2_ITERATIONS, "FVu1Wtpe0MM/Rs+CcLT7nbzMMQ0emHDXpcfjJoQrDtCe8cQqWP4rpCXZenBw9bC3/UWx5+kA9go9zKkhq2UmAQ==");
  private static final String DEPRECATED_HASH_METHOD = "SHA1";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();
  private final HttpRequest request = mock(HttpRequest.class);
  private final AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", NUMBER_OF_PBKDF2_ITERATIONS);
  private final CredentialsExternalAuthentication externalAuthentication = mock(CredentialsExternalAuthentication.class);
  private final CredentialsLocalAuthentication localAuthentication = spy(new CredentialsLocalAuthentication(dbClient, settings.asConfig()));
  private final LdapCredentialsAuthentication ldapCredentialsAuthentication = mock(LdapCredentialsAuthentication.class);
  private final CredentialsAuthentication underTest = new CredentialsAuthentication(dbClient, authenticationEvent, externalAuthentication, localAuthentication,
    ldapCredentialsAuthentication);

  @Test
  public void authenticate_local_user() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(ENCRYPTED_PASSWORD)
      .setHashMethod(CredentialsLocalAuthentication.HashMethod.PBKDF2.name())
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
      .setCryptedPassword(format("%d$%s", NUMBER_OF_PBKDF2_ITERATIONS, "WrongPassword"))
      .setSalt("salt")
      .setHashMethod(CredentialsLocalAuthentication.HashMethod.PBKDF2.name())
      .setLocal(true));

    assertThatThrownBy(() -> executeAuthenticate(BASIC))
      .hasMessage("wrong password")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(BASIC))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyNoInteractions(authenticationEvent);

  }

  @Test
  public void authenticate_external_user() {
    when(externalAuthentication.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC)).thenReturn(Optional.of(newUserDto()));
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setLocal(false));

    executeAuthenticate(BASIC);

    verify(externalAuthentication).authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);
    verifyNoInteractions(ldapCredentialsAuthentication);
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void authenticate_ldap_user() {
    when(externalAuthentication.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC)).thenReturn(Optional.empty());

    String externalId = "12345";
    when(ldapCredentialsAuthentication.authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC)).thenReturn(Optional.of(newUserDto().setExternalId(externalId)));
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setLocal(false));

    assertThat(executeAuthenticate(BASIC).getExternalId()).isEqualTo(externalId);

    verify(externalAuthentication).authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);
    verify(ldapCredentialsAuthentication).authenticate(new Credentials(LOGIN, PASSWORD), request, BASIC);
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_external_user_when_no_external_and_ldap_authentication() {
    when(externalAuthentication.authenticate(new Credentials(LOGIN, PASSWORD), request, SONARQUBE_TOKEN)).thenReturn(Optional.empty());
    when(ldapCredentialsAuthentication.authenticate(new Credentials(LOGIN, PASSWORD), request, SONARQUBE_TOKEN)).thenReturn(Optional.empty());
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setLocal(false));

    assertThatThrownBy(() -> executeAuthenticate(SONARQUBE_TOKEN))
      .hasMessage("User is not local")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(SONARQUBE_TOKEN))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verify(externalAuthentication).authenticate(new Credentials(LOGIN, PASSWORD), request, SONARQUBE_TOKEN);
    verify(ldapCredentialsAuthentication).authenticate(new Credentials(LOGIN, PASSWORD), request, SONARQUBE_TOKEN);
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_local_user_that_have_no_password() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(null)
      .setSalt(SALT)
      .setHashMethod(CredentialsLocalAuthentication.HashMethod.PBKDF2.name())
      .setLocal(true));

    assertThatThrownBy(() -> executeAuthenticate(BASIC))
      .hasMessage("null password in DB")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(BASIC))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_local_user_that_have_no_salt() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(ENCRYPTED_PASSWORD)
      .setSalt(null)
      .setHashMethod(CredentialsLocalAuthentication.HashMethod.PBKDF2.name())
      .setLocal(true));

    assertThatThrownBy(() -> executeAuthenticate(SONARQUBE_TOKEN))
      .hasMessage("null salt")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(SONARQUBE_TOKEN))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_unknown_hash_method_should_force_hash() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(ENCRYPTED_PASSWORD)
      .setSalt(SALT)
      .setHashMethod(DEPRECATED_HASH_METHOD)
      .setLocal(true));

    assertThatThrownBy(() -> executeAuthenticate(SONARQUBE_TOKEN))
      .hasMessage(format(ERROR_UNKNOWN_HASH_METHOD, DEPRECATED_HASH_METHOD))
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(SONARQUBE_TOKEN))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verify(localAuthentication).generateHashToAvoidEnumerationAttack();
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void local_authentication_without_password_should_throw_IAE() {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(ENCRYPTED_PASSWORD)
      .setSalt(SALT)
      .setHashMethod(DEPRECATED_HASH_METHOD)
      .setLocal(true));

    Credentials credentials = new Credentials(LOGIN, null);
    assertThatThrownBy(() -> underTest.authenticate(credentials, request, SONARQUBE_TOKEN))
      .hasMessage(ERROR_PASSWORD_CANNOT_BE_NULL)
      .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_unknown_user_after_forcing_hash() {
    assertThatThrownBy(() -> executeAuthenticate(BASIC))
      .hasMessage("No active user for login")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(BASIC))
      .hasFieldOrPropertyWithValue("login", LOGIN);

    verify(localAuthentication).generateHashToAvoidEnumerationAttack();

    verifyNoInteractions(authenticationEvent);
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
