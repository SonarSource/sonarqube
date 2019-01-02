/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Random;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mindrot.jbcrypt.BCrypt;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.CredentialsLocalAuthentication.HashMethod.BCRYPT;
import static org.sonar.server.authentication.CredentialsLocalAuthentication.HashMethod.SHA1;

public class CredentialsLocalAuthenticationTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();

  private static final Random RANDOM = new Random();

  private CredentialsLocalAuthentication underTest = new CredentialsLocalAuthentication(db.getDbClient());

  @Test
  public void incorrect_hash_should_throw_AuthenticationException() {
    UserDto user = newUserDto()
      .setHashMethod("ALGON2");

    expectedException.expect(AuthenticationException.class);
    expectedException.expectMessage("Unknown hash method [ALGON2]");

    underTest.authenticate(db.getSession(), user, "whatever", AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void null_hash_should_throw_AuthenticationException() {
    UserDto user = newUserDto();

    expectedException.expect(AuthenticationException.class);
    expectedException.expectMessage("null hash method");

    underTest.authenticate(db.getSession(), user, "whatever", AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_with_bcrypt_with_correct_password_should_work() {
    String password = randomAlphanumeric(60);

    UserDto user = newUserDto()
      .setHashMethod(BCRYPT.name())
      .setCryptedPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));

    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_with_sha1_with_correct_password_should_work() {
    String password = randomAlphanumeric(60);

    byte[] saltRandom = new byte[20];
    RANDOM.nextBytes(saltRandom);
    String salt = DigestUtils.sha1Hex(saltRandom);

    UserDto user = newUserDto()
      .setHashMethod(SHA1.name())
      .setCryptedPassword(DigestUtils.sha1Hex("--" + salt + "--" + password + "--"))
      .setSalt(salt);

    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_with_sha1_with_incorrect_password_should_throw_AuthenticationException() {
    String password = randomAlphanumeric(60);

    byte[] saltRandom = new byte[20];
    RANDOM.nextBytes(saltRandom);
    String salt = DigestUtils.sha1Hex(saltRandom);

    UserDto user = newUserDto()
      .setHashMethod(SHA1.name())
      .setCryptedPassword(DigestUtils.sha1Hex("--" + salt + "--" + password + "--"))
      .setSalt(salt);

    expectedException.expect(AuthenticationException.class);
    expectedException.expectMessage("wrong password");

    underTest.authenticate(db.getSession(), user, "WHATEVER", AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_with_sha1_with_empty_password_should_throw_AuthenticationException() {
    byte[] saltRandom = new byte[20];
    RANDOM.nextBytes(saltRandom);
    String salt = DigestUtils.sha1Hex(saltRandom);

    UserDto user = newUserDto()
      .setCryptedPassword(null)
      .setHashMethod(SHA1.name())
      .setSalt(salt);

    expectedException.expect(AuthenticationException.class);
    expectedException.expectMessage("null password in DB");

    underTest.authenticate(db.getSession(), user, "WHATEVER", AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_with_sha1_with_empty_salt_should_throw_AuthenticationException() {
    String password = randomAlphanumeric(60);

    UserDto user = newUserDto()
      .setHashMethod(SHA1.name())
      .setCryptedPassword(DigestUtils.sha1Hex("--0242b0b4c0a93ddfe09dd886de50bc25ba000b51--" + password + "--"))
      .setSalt(null);

    expectedException.expect(AuthenticationException.class);
    expectedException.expectMessage("null salt");

    underTest.authenticate(db.getSession(), user, "WHATEVER", AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_with_bcrypt_with_incorrect_password_should_throw_AuthenticationException() {
    String password = randomAlphanumeric(60);

    UserDto user = newUserDto()
      .setHashMethod(BCRYPT.name())
      .setCryptedPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));

    expectedException.expect(AuthenticationException.class);
    expectedException.expectMessage("wrong password");

    underTest.authenticate(db.getSession(), user, "WHATEVER", AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_upgrade_hash_function_when_SHA1_was_used() {
    String password = randomAlphanumeric(60);

    byte[] saltRandom = new byte[20];
    RANDOM.nextBytes(saltRandom);
    String salt = DigestUtils.sha1Hex(saltRandom);

    UserDto user = newUserDto()
      .setLogin("myself")
      .setHashMethod(SHA1.name())
      .setCryptedPassword(DigestUtils.sha1Hex("--" + salt + "--" + password + "--"))
      .setSalt(salt);
    db.users().insertUser(user);

    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);

    Optional<UserDto> myself = db.users().selectUserByLogin("myself");
    assertThat(myself).isPresent();
    assertThat(myself.get().getHashMethod()).isEqualTo(BCRYPT.name());
    assertThat(myself.get().getSalt()).isNull();

    // authentication must work with upgraded hash method
    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);
  }
}
