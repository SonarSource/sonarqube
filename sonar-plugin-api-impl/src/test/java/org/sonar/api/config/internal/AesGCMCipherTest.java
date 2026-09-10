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
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.config.internal.EnvironmentVariableSecretKeySource.PREVIOUS_KEY_ENVIRONMENT_VARIABLE;

public class AesGCMCipherTest {

  private static final String A_PATH_TO_NO_FILE = "/no/such/sonar-secret.txt";
  private static final String A_BASE64_VALUE_THAT_IS_NOT_AN_AES_KEY = "bm90IGFuIEFFUyBrZXk=";

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void encrypt_should_generate_different_value_everytime() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey());

    String encryptedText1 = cipher.encrypt("this is a secret");
    String encryptedText2 = cipher.encrypt("this is a secret");

    assertThat(StringUtils.isNotBlank(encryptedText1)).isTrue();
    assertThat(StringUtils.isNotBlank(encryptedText2)).isTrue();
    assertThat(encryptedText1).isNotEqualTo(encryptedText2);
  }

  @Test
  public void encrypt_bad_key() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/bad_secret_key.txt");
    String path = new File(resource.toURI()).getCanonicalPath();
    AesGCMCipher cipher = new AesGCMCipher(path);

    assertThatThrownBy(() -> cipher.encrypt("this is a secret"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The secret key provided by the file " + path + " is not a base64 encoded AES key of 128, 192 or 256 bits");
  }

  @Test
  public void decrypt() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey());
    String input1 = "this is a secret";
    String input2 = "asdkfja;ksldjfowiaqueropijadfskncmnv/sdjflskjdflkjiqoeuwroiqu./qewirouasoidfhjaskldfhjkhckjnkiuoewiruoasdjkfalkufoiwueroijuqwoerjsdkjflweoiru";

    assertThat(cipher.decrypt(cipher.encrypt(input1))).isEqualTo(input1);
    assertThat(cipher.decrypt(cipher.encrypt(input1))).isEqualTo(input1);
    assertThat(cipher.decrypt(cipher.encrypt(input2))).isEqualTo(input2);
    assertThat(cipher.decrypt(cipher.encrypt(input2))).isEqualTo(input2);
  }

  @Test
  public void decrypt_bad_key() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/bad_secret_key.txt");
    String path = new File(resource.toURI()).getCanonicalPath();
    AesGCMCipher cipher = new AesGCMCipher(path);

    assertThatThrownBy(() -> cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY="))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("The secret key provided by the file " + path + " is not a base64 encoded AES key of 128, 192 or 256 bits");
  }

  @Test
  public void decrypt_other_key() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/other_secret_key.txt");
    AesGCMCipher originalCipher = new AesGCMCipher(pathToSecretKey());
    AesGCMCipher cipher = new AesGCMCipher(new File(resource.toURI()).getCanonicalPath());

    assertThatThrownBy(() -> cipher.decrypt(originalCipher.encrypt("this is a secret")))
      .hasMessage(AesCipher.DECRYPTION_FAILURE_MESSAGE)
      .hasCauseInstanceOf(AEADBadTagException.class);
  }

  @Test
  public void decrypt_truncated_ciphertext() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey());

    assertThatThrownBy(() -> cipher.decrypt("AA=="))
      .hasMessage(AesCipher.DECRYPTION_FAILURE_MESSAGE)
      .hasCauseInstanceOf(BufferUnderflowException.class);
  }

  @Test
  public void hasSecretKey_whenKeyComesFromAnotherSource_shouldBeTrue() {
    AesGCMCipher cipher = new AesGCMCipher(null, secretKeySource(new Encryption(null).generateRandomSecretKey()));

    assertThat(cipher.hasSecretKey()).isTrue();
  }

  @Test
  public void canLoadSecretKey_whenTheConfiguredFileHoldsAValidKey_shouldBeTrue() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey());

    assertThat(cipher.canLoadSecretKey()).isTrue();
  }

  @Test
  public void canLoadSecretKey_whenTheConfiguredFileHoldsAnUnusableKey_shouldBeFalseWhileTheKeyStaysConfigured() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/bad_secret_key.txt");
    AesGCMCipher cipher = new AesGCMCipher(new File(resource.toURI()).getCanonicalPath());

    // writing stores clear text wherever no key is configured, so an unusable key must keep counting as configured
    // and keep failing the write, rather than passing for an absent one
    assertThat(cipher.hasSecretKey()).isTrue();
    assertThat(cipher.canLoadSecretKey()).isFalse();
  }

  @Test
  public void canLoadSecretKey_whenNoKeyIsConfiguredAnywhere_shouldBeFalse() {
    AesGCMCipher cipher = new AesGCMCipher(A_PATH_TO_NO_FILE, secretKeySource(null), secretKeySource(null));

    assertThat(cipher.canLoadSecretKey()).isFalse();
  }

  @Test
  public void canLoadSecretKey_whenTheKeyOfAnotherSourceHasAnInvalidLength_shouldBeFalse() {
    String aTruncatedKey = new Encryption(null).generateRandomSecretKey().substring(0, 8);
    AesGCMCipher cipher = new AesGCMCipher(null, secretKeySource(aTruncatedKey), secretKeySource(null));

    assertThat(cipher.hasSecretKey()).isTrue();
    assertThat(cipher.canLoadSecretKey()).isFalse();
  }

  @Test
  public void decrypt_whenKeyComesFromAnotherSource_shouldUseThatKey() {
    AesGCMCipher cipher = new AesGCMCipher(null, secretKeySource(new Encryption(null).generateRandomSecretKey()));

    assertThat(cipher.decrypt(cipher.encrypt("this is a secret"))).isEqualTo("this is a secret");
  }

  @Test
  public void encrypt_whenSecretKeyPathIsSet_shouldPreferItOverOtherSources() throws Exception {
    AesGCMCipher cipherWithBoth = new AesGCMCipher(pathToSecretKey(), secretKeySource(new Encryption(null).generateRandomSecretKey()));

    String encryptedText = cipherWithBoth.encrypt("this is a secret");

    // readable by a cipher that only knows the file, which it would not be had the other source won
    assertThat(new AesGCMCipher(pathToSecretKey()).decrypt(encryptedText)).isEqualTo("this is a secret");
  }

  @Test
  public void hasSecretKey_whenSecretKeyPathPointsAtNoFileAndKeyComesFromAnotherSource_shouldBeTrue() {
    AesGCMCipher cipher = new AesGCMCipher(A_PATH_TO_NO_FILE, secretKeySource(new Encryption(null).generateRandomSecretKey()));

    assertThat(cipher.hasSecretKey()).isTrue();
  }

  @Test
  public void encrypt_whenSecretKeyPathPointsAtNoFileAndKeyComesFromAnotherSource_shouldUseThatSource() {
    AesGCMCipher cipher = new AesGCMCipher(A_PATH_TO_NO_FILE,
      secretKeySource(new Encryption(null).generateRandomSecretKey()), secretKeySource(null));

    assertThat(cipher.decrypt(cipher.encrypt("this is a secret"))).isEqualTo("this is a secret");
  }

  @Test
  public void encrypt_whenSecretKeyPathPointsAtNoFileAndNoSourceHoldsAKey_shouldStillReportThatPath() {
    AesGCMCipher cipher = new AesGCMCipher(A_PATH_TO_NO_FILE, secretKeySource(null));

    assertThatThrownBy(() -> cipher.encrypt("this is a secret"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(A_PATH_TO_NO_FILE);
  }

  @Test
  public void encrypt_whenTheKeyOfAnotherSourceIsNotAUsableAesKey_shouldNameThatSource() {
    AesGCMCipher cipherWithNoKeyBytes = new AesGCMCipher(null, secretKeySource("@@@"));
    AesGCMCipher cipherWithTooFewKeyBytes = new AesGCMCipher(null, secretKeySource("changeme"));

    assertThatThrownBy(() -> cipherWithNoKeyBytes.encrypt("this is a secret"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("a source used by tests")
      .hasMessageContaining("128, 192 or 256 bits");
    assertThatThrownBy(() -> cipherWithTooFewKeyBytes.encrypt("this is a secret"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("a source used by tests");
  }

  @Test
  public void encrypt_whenTheKeyOfAnotherSourceIs128Bits_shouldAcceptIt() throws Exception {
    // the key files an instance may already use are not all 256 bits, and moving such a key to another
    // source must not stop it from working
    AesGCMCipher cipher = new AesGCMCipher(null, secretKeySource(secretKeyFileContent()), secretKeySource(null));

    assertThat(cipher.decrypt(cipher.encrypt("this is a secret"))).isEqualTo("this is a secret");
  }

  @Test
  public void hasPreviousSecretKey_whenTheConfiguredPathHoldsAFile_shouldBeTrue() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null), secretKeySource(null));
    cipher.setPathToPreviousSecretKey(pathToAnotherSecretKey());

    assertThat(cipher.hasPreviousSecretKey()).isTrue();
  }

  @Test
  public void hasPreviousSecretKey_whenTheConfiguredPathHoldsNoFileAndNoSourceHasTheKey_shouldBeFalse() throws Exception {
    // this reports what the key loading can deliver, so a rotation is not reported while the key it would need is
    // unreadable everywhere
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null), secretKeySource(null));
    cipher.setPathToPreviousSecretKey(A_PATH_TO_NO_FILE);

    assertThat(cipher.hasPreviousSecretKey()).isFalse();
  }

  @Test
  public void hasPreviousSecretKey_whenTheConfiguredPathHoldsNoFileAndASourceHasTheKey_shouldBeTrue() throws Exception {
    // the key loading falls back to the source here, as it does for the current key, so the rotation has to run rather
    // than stall until a volume that is not mounted yet comes back
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null),
      secretKeySource(new Encryption(null).generateRandomSecretKey()));
    cipher.setPathToPreviousSecretKey(A_PATH_TO_NO_FILE);

    assertThat(cipher.hasPreviousSecretKey()).isTrue();
  }

  @Test
  public void hasPreviousSecretKey_whenNoPathIsConfiguredAndASourceHasTheKey_shouldBeTrue() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null),
      secretKeySource(new Encryption(null).generateRandomSecretKey()));

    assertThat(cipher.hasPreviousSecretKey()).isTrue();
  }

  @Test
  public void hasPreviousSecretKey_whenNoPathIsConfiguredAndNoSourceHasTheKey_shouldBeFalse() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null), secretKeySource(null));

    assertThat(cipher.hasPreviousSecretKey()).isFalse();
  }

  @Test
  public void hasPreviousSecretKey_whenTheConfiguredPathHoldsABlankFile_shouldBeFalse() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null), secretKeySource(null));
    File blankPreviousSecretKeyFile = temporaryFolder.newFile();
    cipher.setPathToPreviousSecretKey(blankPreviousSecretKeyFile.getCanonicalPath());

    assertThat(cipher.hasPreviousSecretKey()).isFalse();

    // the misconfiguration is what the operator is told about, once, instead of every value it would fail to rewrite
    assertThat(logTester.logs(Level.WARN)).hasSize(1);
    assertThat(logTester.logs(Level.WARN).get(0)).contains("secret key being replaced cannot be loaded");
    assertThat(logTester.getLogs(Level.WARN).get(0).getThrowable())
      .hasMessageContaining(blankPreviousSecretKeyFile.getCanonicalPath());
  }

  @Test
  public void hasPreviousSecretKey_whenTheEnvironmentVariableHoldsNoUsableKey_shouldBeFalse() throws Exception {
    System2 system2 = mock(System2.class);
    when(system2.envVariable(PREVIOUS_KEY_ENVIRONMENT_VARIABLE)).thenReturn(A_BASE64_VALUE_THAT_IS_NOT_AN_AES_KEY);
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null),
      new EnvironmentVariableSecretKeySource(system2, PREVIOUS_KEY_ENVIRONMENT_VARIABLE));

    assertThat(cipher.hasPreviousSecretKey()).isFalse();

    assertThat(logTester.logs(Level.WARN)).hasSize(1);
    assertThat(logTester.getLogs(Level.WARN).get(0).getThrowable())
      .hasMessageContaining(PREVIOUS_KEY_ENVIRONMENT_VARIABLE);
  }

  @Test
  public void decrypt_whenValueWasWrittenWithThePreviousKey_shouldStillReadIt() throws Exception {
    String previousBase64Key = new Encryption(null).generateRandomSecretKey();
    AesGCMCipher previousCipher = new AesGCMCipher(null, secretKeySource(previousBase64Key));
    String encryptedWithPreviousKey = previousCipher.encrypt("this is a secret");

    AesGCMCipher rotatedCipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(previousBase64Key), secretKeySource(previousBase64Key));

    assertThat(rotatedCipher.decrypt(encryptedWithPreviousKey)).isEqualTo("this is a secret");
  }

  @Test
  public void decrypt_whenPreviousKeyIsConfigured_shouldStillReadValuesWrittenWithTheCurrentKey() throws Exception {
    AesGCMCipher rotatedCipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(new Encryption(null).generateRandomSecretKey()),
      secretKeySource(new Encryption(null).generateRandomSecretKey()));

    assertThat(rotatedCipher.decrypt(rotatedCipher.encrypt("this is a secret"))).isEqualTo("this is a secret");
  }

  @Test
  public void decrypt_whenNeitherKeyWorks_shouldFailAsADecryptionFailure() throws Exception {
    AesGCMCipher originalCipher = new AesGCMCipher(null, secretKeySource(new Encryption(null).generateRandomSecretKey()));
    AesGCMCipher rotatedCipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(new Encryption(null).generateRandomSecretKey()),
      secretKeySource(new Encryption(null).generateRandomSecretKey()));
    String encryptedWithAnotherKey = originalCipher.encrypt("this is a secret");

    // which of the two failures is reported cannot be told apart from here: this algorithm rejects every wrong key the
    // same way, and a key that would fail differently is reported as absent rather than failing the decryption
    assertThatThrownBy(() -> rotatedCipher.decrypt(encryptedWithAnotherKey))
      .hasMessage(AesCipher.DECRYPTION_FAILURE_MESSAGE)
      .hasCauseInstanceOf(BadPaddingException.class);
  }

  @Test
  public void decrypt_whenThePreviousKeyFileIsGone_shouldStillReadValuesWrittenWithTheCurrentKey() throws Exception {
    // the state an instance is left in when a rotation is finished and the old key file is deleted while the property
    // pointing at it is still set: the fallback is unusable, but the current key is not
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null), secretKeySource(null));
    cipher.setPathToPreviousSecretKey(A_PATH_TO_NO_FILE);
    String encryptedWithTheCurrentKey = cipher.encrypt("this is a secret");

    assertThat(cipher.decrypt(encryptedWithTheCurrentKey)).isEqualTo("this is a secret");
  }

  @Test
  public void decrypt_whenThePreviousKeyPathHoldsNoFileAndASourceHasTheKey_shouldStillReadValuesWrittenWithIt() throws Exception {
    // the current key already resolves this way: a path holding no file is how the key is taken from elsewhere, so a
    // volume that is not mounted yet must not strand the values written with the key being replaced
    String previousBase64Key = new Encryption(null).generateRandomSecretKey();
    AesGCMCipher previousCipher = new AesGCMCipher(null, secretKeySource(previousBase64Key));
    String encryptedWithPreviousKey = previousCipher.encrypt("this is a secret");

    AesGCMCipher rotatedCipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null), secretKeySource(previousBase64Key));
    rotatedCipher.setPathToPreviousSecretKey(A_PATH_TO_NO_FILE);

    assertThat(rotatedCipher.decrypt(encryptedWithPreviousKey)).isEqualTo("this is a secret");
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  public void decrypt_whenThePreviousKeyFileIsGone_shouldWarnOnceAndNameThatProperty() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null), secretKeySource(null));
    cipher.setPathToPreviousSecretKey(A_PATH_TO_NO_FILE);
    String encryptedWithTheCurrentKey = cipher.encrypt("this is a secret");

    cipher.decrypt(encryptedWithTheCurrentKey);
    cipher.decrypt(encryptedWithTheCurrentKey);

    // decryption runs on every settings read, so the condition is reported once rather than per call
    assertThat(logTester.logs(Level.WARN)).hasSize(1);
    assertThat(logTester.logs(Level.WARN).get(0)).contains("secret key being replaced cannot be loaded");
    assertThat(logTester.getLogs(Level.WARN).get(0).getThrowable())
      .hasMessageContaining(Encryption.PREVIOUS_SECRET_KEY_PATH)
      .hasMessageContaining(A_PATH_TO_NO_FILE);
  }

  @Test
  public void decrypt_whenThePreviousKeyFileIsGoneAndWarningsAreSuppressed_shouldStillWarnOnceTheyAreEnabledAgain() throws Exception {
    AesGCMCipher cipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(null), secretKeySource(null));
    cipher.setPathToPreviousSecretKey(A_PATH_TO_NO_FILE);
    String encryptedWithTheCurrentKey = cipher.encrypt("this is a secret");
    logTester.setLevel(Level.ERROR);

    cipher.decrypt(encryptedWithTheCurrentKey);
    logTester.setLevel(Level.WARN);
    cipher.decrypt(encryptedWithTheCurrentKey);

    // the warn-once flag is not spent while the level drops the message, so the operator still gets told about the
    // misconfiguration once the level lets them
    assertThat(logTester.logs(Level.WARN)).hasSize(1);
    assertThat(logTester.logs(Level.WARN).get(0)).contains("secret key being replaced cannot be loaded");
  }

  @Test
  public void encrypt_whenTheConfiguredPathHoldsNoFileAndASourceHasTheKey_shouldWarnOnceAboutTheSubstitution() {
    AesGCMCipher cipher = new AesGCMCipher(A_PATH_TO_NO_FILE,
      secretKeySource(new Encryption(null).generateRandomSecretKey()), secretKeySource(null));

    cipher.encrypt("this is a secret");
    cipher.encrypt("this is another secret");

    assertThat(logTester.logs(Level.WARN)).hasSize(1);
    assertThat(logTester.logs(Level.WARN).get(0))
      .contains(A_PATH_TO_NO_FILE)
      .contains("a source used by tests");
  }

  @Test
  public void encrypt_whenNoPathIsConfiguredAndASourceHasTheKey_shouldNotWarnAboutASubstitution() {
    AesGCMCipher cipher = new AesGCMCipher(null,
      secretKeySource(new Encryption(null).generateRandomSecretKey()), secretKeySource(null));

    cipher.encrypt("this is a secret");

    // nothing was bypassed: supplying the key elsewhere is how an instance without that property is meant to work
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  public void encrypt_whenPreviousKeyIsConfigured_shouldUseTheCurrentKey() throws Exception {
    String previousBase64Key = new Encryption(null).generateRandomSecretKey();
    AesGCMCipher rotatedCipher = new AesGCMCipher(pathToSecretKey(), secretKeySource(previousBase64Key), secretKeySource(previousBase64Key));

    String encryptedText = rotatedCipher.encrypt("this is a secret");

    // readable by a cipher that only knows the current key, so the previous one was not used to write
    assertThat(new AesGCMCipher(pathToSecretKey(), secretKeySource(null), secretKeySource(null)).decrypt(encryptedText))
      .isEqualTo("this is a secret");
  }

  private String pathToSecretKey() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/aes_secret_key.txt");
    return new File(resource.toURI()).getCanonicalPath();
  }

  private String pathToAnotherSecretKey() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/other_secret_key.txt");
    return new File(resource.toURI()).getCanonicalPath();
  }

  private String secretKeyFileContent() throws Exception {
    return StringUtils.trim(Files.readString(Path.of(pathToSecretKey())));
  }

  private static SecretKeySource secretKeySource(@Nullable String base64Key) {
    return new SecretKeySource() {
      @Override
      public Optional<String> loadBase64Key() {
        return Optional.ofNullable(base64Key);
      }

      @Override
      public String describe() {
        return "a source used by tests";
      }
    };
  }
}
