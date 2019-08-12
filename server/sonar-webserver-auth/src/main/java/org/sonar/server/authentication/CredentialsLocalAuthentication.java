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

import java.security.SecureRandom;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent.Method;
import org.sonar.server.authentication.event.AuthenticationEvent.Source;
import org.sonar.server.authentication.event.AuthenticationException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Validates the password of a "local" user (password is stored in
 * database).
 */
public class CredentialsLocalAuthentication {

  private final DbClient dbClient;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  // The default hash method that must be used is BCRYPT
  private static final HashMethod DEFAULT = HashMethod.BCRYPT;

  public CredentialsLocalAuthentication(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * This method authenticate a user with his password against the value stored in user.
   * If authentication failed an AuthenticationException will be thrown containing the failure message.
   * If the password must be updated because an old algorithm is used, the UserDto is updated but the session
   * is not committed
   */
  public void authenticate(DbSession session, UserDto user, @Nullable String password, Method method) {
    if (user.getHashMethod() == null) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(method))
        .setLogin(user.getLogin())
        .setMessage("null hash method")
        .build();
    }

    HashMethod hashMethod;
    try {
      hashMethod = HashMethod.valueOf(user.getHashMethod());
    } catch (IllegalArgumentException ex) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(method))
        .setLogin(user.getLogin())
        .setMessage(format("Unknown hash method [%s]", user.getHashMethod()))
        .build();
    }

    AuthenticationResult result = hashMethod.checkCredentials(user, password);
    if (!result.isSuccessful()) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(method))
        .setLogin(user.getLogin())
        .setMessage(result.getFailureMessage())
        .build();
    }

    // Upgrade the password if it's an old hashMethod
    if (hashMethod != DEFAULT) {
      DEFAULT.storeHashPassword(user, password);
      dbClient.userDao().update(session, user);
    }
  }

  /**
   * Method used to store the password as a hash in database.
   * The crypted_password, salt and hash_method are set
   */
  public void storeHashPassword(UserDto user, String password) {
    DEFAULT.storeHashPassword(user, password);
  }

  public enum HashMethod implements HashFunction {
    SHA1(new Sha1Function()), BCRYPT(new BcryptFunction());

    private HashFunction hashFunction;

    HashMethod(HashFunction hashFunction) {
      this.hashFunction = hashFunction;
    }

    @Override
    public AuthenticationResult checkCredentials(UserDto user, String password) {
      return hashFunction.checkCredentials(user, password);
    }

    @Override
    public void storeHashPassword(UserDto user, String password) {
      hashFunction.storeHashPassword(user, password);
    }
  }

  private static class AuthenticationResult {
    private final boolean successful;
    private final String failureMessage;

    private AuthenticationResult(boolean successful, String failureMessage) {
      checkArgument((successful && failureMessage.isEmpty()) || (!successful && !failureMessage.isEmpty()), "Incorrect parameters");
      this.successful = successful;
      this.failureMessage = failureMessage;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getFailureMessage() {
      return failureMessage;
    }
  }

  public interface HashFunction {
    AuthenticationResult checkCredentials(UserDto user, String password);

    void storeHashPassword(UserDto user, String password);
  }

  /**
   * Implementation of deprecated SHA1 hash function
   */
  private static final class Sha1Function implements HashFunction {
    @Override
    public AuthenticationResult checkCredentials(UserDto user, String password) {
      if (user.getCryptedPassword() == null) {
        return new AuthenticationResult(false, "null password in DB");
      }
      if (user.getSalt() == null) {
        return new AuthenticationResult(false, "null salt");
      }
      if (!user.getCryptedPassword().equals(hash(user.getSalt(), password))) {
        return new AuthenticationResult(false, "wrong password");
      }
      return new AuthenticationResult(true, "");
    }

    @Override
    public void storeHashPassword(UserDto user, String password) {
      requireNonNull(password, "Password cannot be null");
      byte[] saltRandom = new byte[20];
      SECURE_RANDOM.nextBytes(saltRandom);
      String salt = DigestUtils.sha1Hex(saltRandom);

      user.setHashMethod(HashMethod.SHA1.name())
        .setCryptedPassword(hash(salt, password))
        .setSalt(salt);
    }

    private static String hash(String salt, String password) {
      return DigestUtils.sha1Hex("--" + salt + "--" + password + "--");
    }
  }

  /**
   * Implementation of bcrypt hash function
   */
  private static final class BcryptFunction implements HashFunction {
    @Override
    public AuthenticationResult checkCredentials(UserDto user, String password) {
      if (!BCrypt.checkpw(password, user.getCryptedPassword())) {
        return new AuthenticationResult(false, "wrong password");
      }
      return new AuthenticationResult(true, "");
    }

    @Override
    public void storeHashPassword(UserDto user, String password) {
      requireNonNull(password, "Password cannot be null");
      user.setHashMethod(HashMethod.BCRYPT.name())
        .setCryptedPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)))
        .setSalt(null);
    }
  }
}
