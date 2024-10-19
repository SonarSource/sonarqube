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

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.CredentialsLocalAuthentication.HashMethod.BCRYPT;
import static org.sonar.server.authentication.CredentialsLocalAuthentication.HashMethod.PBKDF2;

public class CredentialsLocalAuthenticationIT {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String PBKDF2_SALT = generatePBKDF2Salt();

  @Rule
  public DbTester db = DbTester.create();

  private static final Random RANDOM = new Random();
  private static final MapSettings settings = new MapSettings();

  private CredentialsLocalAuthentication underTest = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());

  @Before
  public void setup() {
    settings.setProperty("sonar.internal.pbkdf2.iterations", 1);
  }

  @Test
  public void incorrect_hash_should_throw_AuthenticationException() {
    DbSession dbSession = db.getSession();
    UserDto user = newUserDto()
      .setHashMethod("ALGON2");

    assertThatThrownBy(() -> underTest.authenticate(dbSession, user, "whatever", AuthenticationEvent.Method.BASIC))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage(format(CredentialsLocalAuthentication.ERROR_UNKNOWN_HASH_METHOD, "ALGON2"));
  }

  @Test
  public void null_hash_should_throw_AuthenticationException() {
    DbSession dbSession = db.getSession();
    UserDto user = newUserDto();

    assertThatThrownBy(() -> underTest.authenticate(dbSession, user, "whatever", AuthenticationEvent.Method.BASIC))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage(CredentialsLocalAuthentication.ERROR_NULL_HASH_METHOD);
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
  public void authentication_with_sha1_should_throw_AuthenticationException() {
    String password = randomAlphanumeric(60);

    byte[] saltRandom = new byte[20];
    RANDOM.nextBytes(saltRandom);
    String salt = DigestUtils.sha1Hex(saltRandom);

    UserDto user = newUserDto()
      .setHashMethod("SHA1")
      .setCryptedPassword(DigestUtils.sha1Hex("--" + salt + "--" + password + "--"))
      .setSalt(salt);

    DbSession session = db.getSession();
    assertThatExceptionOfType(AuthenticationException.class)
      .isThrownBy(() -> underTest.authenticate(session, user, password, AuthenticationEvent.Method.BASIC))
      .withMessage("Unknown hash method [SHA1]");
  }

  @Test
  public void authentication_with_bcrypt_with_incorrect_password_should_throw_AuthenticationException() {
    DbSession dbSession = db.getSession();
    String password = randomAlphanumeric(60);

    UserDto user = newUserDto()
      .setHashMethod(BCRYPT.name())
      .setCryptedPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));

    assertThatThrownBy(() -> underTest.authenticate(dbSession, user, "WHATEVER", AuthenticationEvent.Method.BASIC))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage(CredentialsLocalAuthentication.ERROR_WRONG_PASSWORD);
  }

  @Test
  public void authentication_with_bcrypt_with_empty_password_should_throw_AuthenticationException() {
    DbSession dbSession = db.getSession();
    UserDto user = newUserDto()
      .setCryptedPassword(null)
      .setHashMethod(BCRYPT.name());

    assertThatThrownBy(() -> underTest.authenticate(dbSession, user, "WHATEVER", AuthenticationEvent.Method.BASIC))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage(CredentialsLocalAuthentication.ERROR_NULL_PASSWORD_IN_DB);
  }

  @Test
  public void authentication_upgrade_hash_function_when_BCRYPT_was_used() {
    String password = randomAlphanumeric(60);

    UserDto user = newUserDto()
      .setLogin("myself")
      .setHashMethod(BCRYPT.name())
      .setCryptedPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)))
      .setSalt(null);
    db.users().insertUser(user);

    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);

    Optional<UserDto> myself = db.users().selectUserByLogin("myself");
    assertThat(myself).isPresent();
    assertThat(myself.get().getHashMethod()).isEqualTo(PBKDF2.name());
    assertThat(myself.get().getSalt()).isNotNull();

    // authentication must work with upgraded hash method
    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_updates_db_if_PBKDF2_iterations_changes() {
    String password = randomAlphanumeric(60);

    UserDto user = newUserDto().setLogin("myself");
    db.users().insertUser(user);
    underTest.storeHashPassword(user, password);

    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);
    assertThat(user.getCryptedPassword()).startsWith("1$");

    settings.setProperty("sonar.internal.pbkdf2.iterations", 3);
    CredentialsLocalAuthentication underTest = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());

    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);
    assertThat(user.getCryptedPassword()).startsWith("3$");
    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_with_pbkdf2_with_correct_password_should_work() {
    String password = randomAlphanumeric(60);
    UserDto user = newUserDto()
      .setHashMethod(PBKDF2.name());

    underTest.storeHashPassword(user, password);
    assertThat(user.getCryptedPassword()).hasSize(88 + 2);
    assertThat(user.getCryptedPassword()).startsWith("1$");
    assertThat(user.getSalt()).hasSize(28);

    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_with_pbkdf2_with_default_number_of_iterations() {
    settings.clear();
    CredentialsLocalAuthentication underTest = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());

    String password = randomAlphanumeric(60);
    UserDto user = newUserDto()
      .setHashMethod(PBKDF2.name());

    underTest.storeHashPassword(user, password);
    assertThat(user.getCryptedPassword()).hasSize(88 + 7);
    assertThat(user.getCryptedPassword()).startsWith("100000$");
    assertThat(user.getSalt()).hasSize(28);

    underTest.authenticate(db.getSession(), user, password, AuthenticationEvent.Method.BASIC);
  }

  @Test
  public void authentication_with_pbkdf2_with_incorrect_password_should_throw_AuthenticationException() {
    DbSession dbSession = db.getSession();
    UserDto user = newUserDto()
      .setHashMethod(PBKDF2.name())
      .setCryptedPassword("1$hash")
      .setSalt("salt");

    assertThatThrownBy(() -> underTest.authenticate(dbSession, user, "WHATEVER", AuthenticationEvent.Method.BASIC))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage(CredentialsLocalAuthentication.ERROR_WRONG_PASSWORD);
  }

  @Test
  public void authentication_with_pbkdf2_with_invalid_hash_should_throw_AuthenticationException() {
    DbSession dbSession = db.getSession();
    String password = randomAlphanumeric(60);

    UserDto userInvalidHash = newUserDto()
      .setHashMethod(PBKDF2.name())
      .setCryptedPassword(password)
      .setSalt(PBKDF2_SALT);

    assertThatThrownBy(() -> underTest.authenticate(dbSession, userInvalidHash, password, AuthenticationEvent.Method.BASIC))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage("invalid hash stored");

    UserDto userInvalidIterations = newUserDto()
      .setHashMethod(PBKDF2.name())
      .setCryptedPassword("a$" + password)
      .setSalt(PBKDF2_SALT);

    assertThatThrownBy(() -> underTest.authenticate(dbSession, userInvalidIterations, password, AuthenticationEvent.Method.BASIC))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage("invalid hash stored");
  }

  @Test
  public void authentication_with_pbkdf2_with_empty_password_should_throw_AuthenticationException() {
    DbSession dbSession = db.getSession();

    UserDto user = newUserDto()
      .setCryptedPassword(null)
      .setHashMethod(PBKDF2.name())
      .setSalt(PBKDF2_SALT);

    assertThatThrownBy(() -> underTest.authenticate(dbSession, user, "WHATEVER", AuthenticationEvent.Method.BASIC))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage(CredentialsLocalAuthentication.ERROR_NULL_PASSWORD_IN_DB);
  }

  @Test
  public void authentication_with_pbkdf2_with_empty_salt_should_throw_AuthenticationException() {
    String password = randomAlphanumeric(60);
    DbSession dbSession = db.getSession();

    UserDto user = newUserDto()
      .setHashMethod(PBKDF2.name())
      .setCryptedPassword("1$" + password)
      .setSalt(null);

    assertThatThrownBy(() -> underTest.authenticate(dbSession, user, password, AuthenticationEvent.Method.BASIC))
      .isInstanceOf(AuthenticationException.class)
      .hasMessage(CredentialsLocalAuthentication.ERROR_NULL_SALT);
  }

  private static String generatePBKDF2Salt() {
    byte[] salt = new byte[20];
    SECURE_RANDOM.nextBytes(salt);
    String saltStr = Base64.getEncoder().encodeToString(salt);
    return saltStr;
  }
}
