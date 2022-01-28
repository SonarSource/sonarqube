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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.EnumMap;
import javax.annotation.Nullable;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.codec.digest.DigestUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.sonar.api.config.Configuration;
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
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final HashMethod DEFAULT = HashMethod.PBKDF2;
  private static final String PBKDF2_ITERATIONS_PROP = "sonar.internal.pbkdf2.iterations";
  public static final String ERROR_NULL_HASH_METHOD = "null hash method";
  public static final String ERROR_NULL_PASSWORD_IN_DB = "null password in DB";
  public static final String ERROR_NULL_SALT = "null salt";
  public static final String ERROR_WRONG_PASSWORD = "wrong password";
  public static final String ERROR_PASSWORD_CANNOT_BE_NULL = "Password cannot be null";
  public static final String ERROR_UNKNOWN_HASH_METHOD = "Unknown hash method [%s]";

  private final DbClient dbClient;
  private final EnumMap<HashMethod, HashFunction> hashFunctions = new EnumMap<>(HashMethod.class);

  public enum HashMethod {
    SHA1, BCRYPT, PBKDF2
  }

  public CredentialsLocalAuthentication(DbClient dbClient, Configuration configuration) {
    this.dbClient = dbClient;
    hashFunctions.put(HashMethod.BCRYPT, new BcryptFunction());
    hashFunctions.put(HashMethod.SHA1, new Sha1Function());
    hashFunctions.put(HashMethod.PBKDF2, new PBKDF2Function(configuration.getInt(PBKDF2_ITERATIONS_PROP).orElse(null)));

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
        .setMessage(ERROR_NULL_HASH_METHOD)
        .build();
    }

    HashMethod hashMethod;
    try {
      hashMethod = HashMethod.valueOf(user.getHashMethod());
    } catch (IllegalArgumentException ex) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(method))
        .setLogin(user.getLogin())
        .setMessage(format(ERROR_UNKNOWN_HASH_METHOD, user.getHashMethod()))
        .build();
    }

    HashFunction hashFunction = hashFunctions.get(hashMethod);

    AuthenticationResult result = hashFunction.checkCredentials(user, password);
    if (!result.isSuccessful()) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(method))
        .setLogin(user.getLogin())
        .setMessage(result.getFailureMessage())
        .build();
    }

    // Upgrade the password if it's an old hashMethod
    if (hashMethod != DEFAULT || result.needsUpdate) {
      hashFunctions.get(DEFAULT).storeHashPassword(user, password);
      dbClient.userDao().update(session, user);
    }
  }

  /**
   * Method used to store the password as a hash in database.
   * The crypted_password, salt and hash_method are set
   */
  public void storeHashPassword(UserDto user, String password) {
    hashFunctions.get(DEFAULT).storeHashPassword(user, password);
  }

  private static class AuthenticationResult {
    private final boolean successful;
    private final String failureMessage;
    private final boolean needsUpdate;

    private AuthenticationResult(boolean successful, String failureMessage) {
      this(successful, failureMessage, false);
    }

    private AuthenticationResult(boolean successful, String failureMessage, boolean needsUpdate) {
      checkArgument((successful && failureMessage.isEmpty()) || (!successful && !failureMessage.isEmpty()), "Incorrect parameters");
      this.successful = successful;
      this.failureMessage = failureMessage;
      this.needsUpdate = needsUpdate;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getFailureMessage() {
      return failureMessage;
    }

    public boolean isNeedsUpdate() {
      return needsUpdate;
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
        return new AuthenticationResult(false, ERROR_NULL_PASSWORD_IN_DB);
      }
      if (user.getSalt() == null) {
        return new AuthenticationResult(false, ERROR_NULL_SALT);
      }
      if (!user.getCryptedPassword().equals(hash(user.getSalt(), password))) {
        return new AuthenticationResult(false, ERROR_WRONG_PASSWORD);
      }
      return new AuthenticationResult(true, "");
    }

    @Override
    public void storeHashPassword(UserDto user, String password) {
      requireNonNull(password, ERROR_PASSWORD_CANNOT_BE_NULL);
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

  private static final class PBKDF2Function implements HashFunction {
    private static final int DEFAULT_ITERATIONS = 100_000;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int KEY_LEN = 512;
    public static final String ERROR_INVALID_HASH_STORED = "invalid hash stored";
    private final int gen_iterations;

    public PBKDF2Function(@Nullable Integer gen_iterations) {
      this.gen_iterations = gen_iterations != null ? gen_iterations : DEFAULT_ITERATIONS;
    }

    @Override
    public AuthenticationResult checkCredentials(UserDto user, String password) {
      if (user.getCryptedPassword() == null) {
        return new AuthenticationResult(false, ERROR_NULL_PASSWORD_IN_DB);
      }
      if (user.getSalt() == null) {
        return new AuthenticationResult(false, ERROR_NULL_SALT);
      }

      int pos = user.getCryptedPassword().indexOf('$');
      if (pos < 1) {
        return new AuthenticationResult(false, ERROR_INVALID_HASH_STORED);
      }
      int iterations;
      try {
        iterations = Integer.parseInt(user.getCryptedPassword().substring(0, pos));
      } catch (NumberFormatException e) {
        return new AuthenticationResult(false, ERROR_INVALID_HASH_STORED);
      }
      String hash = user.getCryptedPassword().substring(pos + 1);
      byte[] salt = Base64.getDecoder().decode(user.getSalt());

      if (!hash.equals(hash(salt, password, iterations))) {
        return new AuthenticationResult(false, ERROR_WRONG_PASSWORD);
      }
      boolean needsUpdate = iterations != gen_iterations;
      return new AuthenticationResult(true, "", needsUpdate);
    }

    @Override
    public void storeHashPassword(UserDto user, String password) {
      byte[] salt = new byte[20];
      SECURE_RANDOM.nextBytes(salt);
      String hashStr = hash(salt, password, gen_iterations);
      String saltStr = Base64.getEncoder().encodeToString(salt);
      user.setHashMethod(HashMethod.PBKDF2.name())
        .setCryptedPassword(gen_iterations + "$" + hashStr)
        .setSalt(saltStr);
    }

    private static String hash(byte[] salt, String password, int iterations) {
      try {
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LEN);
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Implementation of deprecated bcrypt hash function
   */
  private static final class BcryptFunction implements HashFunction {
    @Override
    public AuthenticationResult checkCredentials(UserDto user, String password) {
      if (user.getCryptedPassword() == null) {
        return new AuthenticationResult(false, ERROR_NULL_PASSWORD_IN_DB);
      }
      // This behavior is overridden in most of integration tests for performance reasons, any changes to BCrypt calls should be propagated to
      // Byteman classes
      if (!BCrypt.checkpw(password, user.getCryptedPassword())) {
        return new AuthenticationResult(false, ERROR_WRONG_PASSWORD);
      }
      return new AuthenticationResult(true, "");
    }

    @Override
    public void storeHashPassword(UserDto user, String password) {
      requireNonNull(password, ERROR_PASSWORD_CANNOT_BE_NULL);
      user.setHashMethod(HashMethod.BCRYPT.name())
        .setCryptedPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)))
        .setSalt(null);
    }
  }
}
