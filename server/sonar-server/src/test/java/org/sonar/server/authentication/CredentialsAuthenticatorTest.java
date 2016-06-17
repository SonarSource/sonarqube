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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;

public class CredentialsAuthenticatorTest {

  static final String LOGIN = "LOGIN";
  static final String PASSWORD = "PASSWORD";
  static final String SALT = "0242b0b4c0a93ddfe09dd886de50bc25ba000b51";
  static final String CRYPTED_PASSWORD = "540e4fc4be4e047db995bc76d18374a5b5db08cc";

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  DbSession dbSession = dbTester.getSession();

  RealmAuthenticator externalAuthenticator = mock(RealmAuthenticator.class);
  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);

  CredentialsAuthenticator underTest = new CredentialsAuthenticator(dbClient, externalAuthenticator);

  @Test
  public void authenticate_local_user() throws Exception {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(CRYPTED_PASSWORD)
      .setSalt(SALT)
      .setLocal(true));

    UserDto userDto = executeAuthenticate();
    assertThat(userDto.getLogin()).isEqualTo(LOGIN);
  }

  @Test
  public void fail_to_authenticate_local_user_when_password_is_wrong() throws Exception {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword("Wrong password")
      .setSalt("Wrong salt")
      .setLocal(true));

    expectedException.expect(UnauthorizedException.class);
    executeAuthenticate();
  }

  @Test
  public void authenticate_none_local_user() throws Exception {
    when(externalAuthenticator.isExternalAuthenticationUsed()).thenReturn(true);
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setLocal(false));

    executeAuthenticate();

    verify(externalAuthenticator).authenticate(LOGIN, PASSWORD, request);
  }

  @Test
  public void fail_to_authenticate_authenticate_none_local_user_when_no_external_authentication() throws Exception {
    when(externalAuthenticator.isExternalAuthenticationUsed()).thenReturn(false);
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setLocal(false));

    expectedException.expect(UnauthorizedException.class);
    executeAuthenticate();
  }

  @Test
  public void fail_to_authenticate_local_user_that_have_no_password() throws Exception {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(null)
      .setSalt(SALT)
      .setLocal(true));

    expectedException.expect(UnauthorizedException.class);
    executeAuthenticate();
  }

  @Test
  public void fail_to_authenticate_local_user_that_have_no_salt() throws Exception {
    insertUser(newUserDto()
      .setLogin(LOGIN)
      .setCryptedPassword(CRYPTED_PASSWORD)
      .setSalt(null)
      .setLocal(true));

    expectedException.expect(UnauthorizedException.class);
    executeAuthenticate();
  }

  private UserDto executeAuthenticate(){
    return underTest.authenticate(LOGIN, PASSWORD, request);
  }

  private UserDto insertUser(UserDto userDto){
    dbClient.userDao().insert(dbSession, userDto);
    dbSession.commit();
    return userDto;
  }
}
