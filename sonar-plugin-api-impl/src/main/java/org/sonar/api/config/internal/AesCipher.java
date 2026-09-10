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
import java.nio.BufferUnderflowException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.CoreProperties.ENCRYPTION_SECRET_KEY_PATH;
import static org.sonar.api.config.internal.EnvironmentVariableSecretKeySource.PREVIOUS_KEY_ENVIRONMENT_VARIABLE;

abstract class AesCipher implements Cipher {
  static final int KEY_SIZE_IN_BITS = 256;

  static final String DECRYPTION_FAILURE_MESSAGE = "Failed to decrypt value. This can happen if the secret key has changed since the value was encrypted, "
    + "or if the value is corrupted.";

  private static final Logger LOG = LoggerFactory.getLogger(AesCipher.class);
  private static final String CRYPTO_KEY = "AES";
  private static final Set<Integer> VALID_KEY_SIZES_IN_BITS = Set.of(128, 192, 256);

  private final SecretKeySource secretKeySource;
  private final SecretKeySource previousSecretKeySource;
  // decryption runs on every settings read, so each of these conditions is reported once rather than per call
  private final AtomicBoolean previousSecretKeyFailureWarned = new AtomicBoolean();
  private final AtomicBoolean configuredPathBypassWarned = new AtomicBoolean();
  private String pathToSecretKey;
  private String pathToPreviousSecretKey;

  AesCipher(@Nullable String pathToSecretKey) {
    this(pathToSecretKey,
      new EnvironmentVariableSecretKeySource(System2.INSTANCE),
      new EnvironmentVariableSecretKeySource(System2.INSTANCE, PREVIOUS_KEY_ENVIRONMENT_VARIABLE));
  }

  AesCipher(@Nullable String pathToSecretKey, SecretKeySource secretKeySource) {
    this(pathToSecretKey, secretKeySource, new EnvironmentVariableSecretKeySource(System2.INSTANCE, PREVIOUS_KEY_ENVIRONMENT_VARIABLE));
  }

  AesCipher(@Nullable String pathToSecretKey, SecretKeySource secretKeySource, SecretKeySource previousSecretKeySource) {
    this.pathToSecretKey = pathToSecretKey;
    this.secretKeySource = secretKeySource;
    this.previousSecretKeySource = previousSecretKeySource;
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

  /**
   * Whether the key {@link #hasSecretKey()} reports can actually be loaded. That these are two different questions is
   * deliberate: writing falls back to clear text wherever no key is configured, so a key that is configured but
   * unusable has to keep failing the write instead of passing for an absent one and downgrading storage silently.
   * This only serves to tell an unusable key from a working one when a failure is reported.
   */
  boolean canLoadSecretKey() {
    try {
      loadSecretFile();
      return true;
    } catch (IOException | RuntimeException e) {
      // the caller reports the failure it already holds, which carries this same cause and the path it concerns
      return false;
    }
  }

  protected Key loadSecretFile() throws IOException {
    if (hasExplicitSecretKeyFile()) {
      return loadSecretFileFromFile(pathToSecretKey);
    }
    Optional<String> base64Key = secretKeySource.loadBase64Key();
    if (base64Key.isPresent()) {
      if (StringUtils.isNotBlank(pathToSecretKey)) {
        warnAboutTheConfiguredPathBeingBypassed();
      }
      return toSecretKey(base64Key.get(), secretKeySource.describe());
    }
    return loadSecretFileFromFile(getPathToSecretKey());
  }

  /**
   * A configured path holding no file is how the key is meant to be taken from elsewhere, but it also happens when a
   * volume is not mounted yet or a secret is rotated out from under the process. Values would then be written with a
   * key other than the configured one, and become unreadable once that file is back, so the substitution is logged.
   */
  private void warnAboutTheConfiguredPathBeingBypassed() {
    // the level is checked before the flag is consumed, so that raising it later still shows a warning that was
    // suppressed until then rather than one that has already been used up
    if (LOG.isWarnEnabled() && configuredPathBypassWarned.compareAndSet(false, true)) {
      LOG.warn("No file at {}, so the secret key provided by {} is used instead. Values are encrypted with that key "
        + "for as long as the file is missing.", pathToSecretKey, secretKeySource.describe());
    }
  }

  private boolean hasExplicitSecretKeyFile() {
    return StringUtils.isNotBlank(pathToSecretKey) && isExistingFile(pathToSecretKey);
  }

  /**
   * The keys decryption may be attempted with, most recent first. Holds more than one key only while the secret key is
   * being rotated, so that values written with the key being replaced can still be read until they have been rewritten.
   */
  protected List<Key> loadSecretKeys() throws IOException {
    Key secretKey = loadSecretFile();
    Optional<Key> previousSecretKey = loadPreviousSecretKey();
    if (previousSecretKey.isEmpty()) {
      return List.of(secretKey);
    }
    return List.of(secretKey, previousSecretKey.get());
  }

  /**
   * Decrypts with the secret key, then with the key it is replacing if one is configured. The failure reported when
   * none of them work is the one from the secret key, since that is the key a value is expected to have been written
   * with. Every algorithm reads its values this way, so it is implemented here rather than by each of them.
   */
  @Override
  public final String decrypt(String encryptedText) {
    return failingAsIllegalState(() -> decryptWithAnySecretKey(encryptedText));
  }

  private String decryptWithAnySecretKey(String encryptedText) throws IOException {
    RuntimeException firstFailure = null;
    for (Key secretKey : loadSecretKeys()) {
      try {
        return decrypt(encryptedText, secretKey);
      } catch (RuntimeException e) {
        firstFailure = firstFailure == null ? e : firstFailure;
      }
    }
    throw requireNonNull(firstFailure, "There is no secret key to decrypt with");
  }

  protected abstract String decrypt(String encryptedText, Key secretKey);

  /**
   * Reports the checked failures of a cryptographic operation as the unchecked failure {@link Cipher} exposes. A
   * failure that is already unchecked propagates as it is, so that it keeps the type callers test for.
   */
  protected static String failingAsIllegalState(CryptographicOperation operation) {
    try {
      return operation.run();
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reports the ways a value can fail to decrypt as {@link #DECRYPTION_FAILURE_MESSAGE}, which names the causes an
   * operator can act on rather than the exception of the algorithm. Both ciphers fail this way, the GCM one through
   * subclasses of these. Anything else keeps saying what it is, since a key that cannot be loaded is a different
   * problem from a value that cannot be read with it.
   */
  protected static String failingAsDecryptionFailure(CryptographicOperation operation) {
    try {
      return operation.run();
    } catch (BadPaddingException | IllegalBlockSizeException | BufferUnderflowException e) {
      throw new IllegalStateException(DECRYPTION_FAILURE_MESSAGE, e);
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @FunctionalInterface
  protected interface CryptographicOperation {
    String run() throws GeneralSecurityException, IOException;
  }

  /**
   * A key being replaced which cannot be loaded is reported as absent rather than failing the decryption. It is only
   * ever a fallback, so letting it fail would make values written with the current, perfectly valid key unreadable —
   * which is what happens once an operator finishes a rotation and deletes the old key file while the property that
   * points at it is still set.
   * <p>
   * It resolves in the same order {@link #loadSecretFile()} does, an explicitly configured file before the source, so
   * that a path whose volume is not mounted yet falls back rather than stranding the values written with that key. A
   * configured path is still read when it is the only place the key could come from, so that it is named in the
   * warning instead of the key passing for one that was never configured.
   */
  private Optional<Key> loadPreviousSecretKey() {
    try {
      if (hasExplicitPreviousSecretKeyFile()) {
        return Optional.of(loadPreviousSecretKeyFromFile());
      }
      Optional<String> base64Key = previousSecretKeySource.loadBase64Key();
      if (base64Key.isPresent()) {
        return Optional.of(toSecretKey(base64Key.get(), previousSecretKeySource.describe()));
      }
      if (StringUtils.isBlank(pathToPreviousSecretKey)) {
        return Optional.empty();
      }
      // the configured path was the only place this key could come from, so it is read to have its failure reported
      return Optional.of(loadPreviousSecretKeyFromFile());
    } catch (IOException | RuntimeException e) {
      warnAboutTheKeyBeingReplaced(e);
      return Optional.empty();
    }
  }

  private boolean hasExplicitPreviousSecretKeyFile() {
    return StringUtils.isNotBlank(pathToPreviousSecretKey) && isExistingFile(pathToPreviousSecretKey);
  }

  private Key loadPreviousSecretKeyFromFile() throws IOException {
    return loadSecretFileFromFile(pathToPreviousSecretKey, Encryption.PREVIOUS_SECRET_KEY_PATH);
  }

  private void warnAboutTheKeyBeingReplaced(Exception cause) {
    // the level is checked before the flag is consumed, for the reason given in warnAboutTheConfiguredPathBeingBypassed
    if (LOG.isWarnEnabled() && previousSecretKeyFailureWarned.compareAndSet(false, true)) {
      LOG.warn("The secret key being replaced cannot be loaded, so decryption is only attempted with the current "
        + "secret key. Values that were written with the key being replaced stay unreadable until it is configured "
        + "again.", cause);
    }
  }

  public void setPathToPreviousSecretKey(@Nullable String pathToPreviousSecretKey) {
    this.pathToPreviousSecretKey = pathToPreviousSecretKey;
    previousSecretKeyFailureWarned.set(false);
  }

  /**
   * Whether a key being replaced is configured, which is what tells a rotation is under way. It is answered by loading
   * that key, so presence and loadability cannot disagree: a key that is configured but unusable reports no rotation,
   * rather than starting one that has no key to rewrite values with and can only fail on every one of them.
   */
  boolean hasPreviousSecretKey() {
    return loadPreviousSecretKey().isPresent();
  }

  private static boolean isExistingFile(String path) {
    File file = new File(path);
    return file.exists() && file.isFile();
  }

  static Key loadSecretFileFromFile(@Nullable String path) throws IOException {
    return loadSecretFileFromFile(path, ENCRYPTION_SECRET_KEY_PATH);
  }

  /**
   * @param propertyKey the property the path comes from, so that a failure names the one that is misconfigured rather
   *                    than always the current key
   */
  private static Key loadSecretFileFromFile(@Nullable String path, String propertyKey) throws IOException {
    if (StringUtils.isBlank(path)) {
      throw new IllegalStateException("Secret key not found. Please set the property " + propertyKey);
    }
    File file = new File(path);
    if (!file.exists() || !file.isFile()) {
      throw new IllegalStateException("The property " + propertyKey + " does not link to a valid file: " + path);
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
    // the length is checked before the key is built, so that an empty one is reported this way rather than by AES
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
    configuredPathBypassWarned.set(false);
  }
}
