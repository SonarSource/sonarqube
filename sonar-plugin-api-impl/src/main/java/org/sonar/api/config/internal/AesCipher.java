/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.api.config.internal;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.utils.System2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.api.CoreProperties.ENCRYPTION_SECRET_KEY_PATH;

abstract class AesCipher implements Cipher {
  static final int KEY_SIZE_IN_BITS = 256;

  static final String DECRYPTION_FAILURE_MESSAGE = "Failed to decrypt value. This can happen if the secret key has changed since the value was encrypted, "
    + "or if the value is corrupted.";

  private static final String CRYPTO_KEY = "AES";
  private static final Set<Integer> VALID_KEY_SIZES_IN_BITS = Set.of(128, 192, 256);

  private final SecretKeySource secretKeySource;
  private String pathToSecretKey;

  AesCipher(@Nullable String pathToSecretKey) {
    this(pathToSecretKey, new EnvironmentVariableSecretKeySource(System2.INSTANCE));
  }

  AesCipher(@Nullable String pathToSecretKey, SecretKeySource secretKeySource) {
    this.pathToSecretKey = pathToSecretKey;
    this.secretKeySource = secretKeySource;
  }

  /**
   * This method checks the existence of the key, but not the validity of the key itself.
   * <p>
   * An explicitly configured {@code sonar.secretKeyPath} is honoured first, so that supplying the key elsewhere never
   * changes the behaviour of an instance which already sets that property. It only wins while it resolves to a file
   * though: a path pointing at nothing holds no key to be protected from, and treating it as one would leave a
   * deployment which also supplies the key elsewhere with no key at all.
   */
  boolean hasSecretKey() {
    if (hasExplicitSecretKeyFile()) {
      return true;
    }
    return secretKeySource.loadBase64Key().isPresent() || isExistingFile(getPathToSecretKey());
  }

  protected Key loadSecretFile() throws IOException {
    if (hasExplicitSecretKeyFile()) {
      return loadSecretFileFromFile(pathToSecretKey);
    }
    Optional<String> base64Key = secretKeySource.loadBase64Key();
    if (base64Key.isPresent()) {
      return toSecretKey(base64Key.get(), secretKeySource.describe());
    }
    return loadSecretFileFromFile(getPathToSecretKey());
  }

  private boolean hasExplicitSecretKeyFile() {
    return StringUtils.isNotBlank(pathToSecretKey) && isExistingFile(pathToSecretKey);
  }

  private static boolean isExistingFile(String path) {
    File file = new File(path);
    return file.exists() && file.isFile();
  }

  Key loadSecretFileFromFile(@Nullable String path) throws IOException {
    if (StringUtils.isBlank(path)) {
      throw new IllegalStateException("Secret key not found. Please set the property " + ENCRYPTION_SECRET_KEY_PATH);
    }
    File file = new File(path);
    if (!file.exists() || !file.isFile()) {
      throw new IllegalStateException("The property " + ENCRYPTION_SECRET_KEY_PATH + " does not link to a valid file: " + path);
    }
    String s = FileUtils.readFileToString(file, UTF_8);
    if (StringUtils.isBlank(s)) {
      throw new IllegalStateException("No secret key in the file: " + path);
    }
    return toSecretKey(StringUtils.trim(s), "the file " + path);
  }

  /**
   * Rejects a key AES cannot use, which would otherwise surface as an {@code Empty key} or {@code Invalid AES key
   * length} failure naming none of the places the key may come from. Every key goes through here, whichever of them
   * holds it, so that the size a key has is judged the same way wherever it is supplied. Sizes other than the
   * generated {@link #KEY_SIZE_IN_BITS} are accepted, so that a key already in use keeps working once it is supplied
   * somewhere else.
   *
   * @param source where the key comes from, phrased to be readable inside the message
   */
  private static Key toSecretKey(String base64Key, String source) {
    byte[] decodedKey = Base64.decodeBase64(base64Key);
    if (!VALID_KEY_SIZES_IN_BITS.contains(decodedKey.length * 8)) {
      throw new IllegalStateException("The secret key provided by " + source
        + " is not a base64 encoded AES key of 128, 192 or 256 bits");
    }
    return new SecretKeySpec(decodedKey, CRYPTO_KEY);
  }

  String generateRandomSecretKey() {
    try {
      KeyGenerator keyGen = KeyGenerator.getInstance(CRYPTO_KEY);
      keyGen.init(KEY_SIZE_IN_BITS, new SecureRandom());
      SecretKey secretKey = keyGen.generateKey();
      return Base64.encodeBase64String(secretKey.getEncoded());

    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate secret key", e);
    }
  }

  /**
   * Does not assign the default to {@link #pathToSecretKey}, so that a later call can still tell an explicitly
   * configured path apart from the default one.
   */
  String getPathToSecretKey() {
    if (StringUtils.isBlank(pathToSecretKey)) {
      return new File(FileUtils.getUserDirectoryPath(), ".sonar/sonar-secret.txt").getPath();
    }
    return pathToSecretKey;
  }

  public void setPathToSecretKey(@Nullable String pathToSecretKey) {
    this.pathToSecretKey = pathToSecretKey;
  }
}
