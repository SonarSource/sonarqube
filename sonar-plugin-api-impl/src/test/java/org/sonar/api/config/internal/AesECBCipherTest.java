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
package org.sonar.api.config.internal;

import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import javax.crypto.BadPaddingException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

public class AesECBCipherTest {

  @Test
  public void generateRandomSecretKey() {
    AesECBCipher cipher = new AesECBCipher(null);

    String key = cipher.generateRandomSecretKey();

    assertThat(StringUtils.isNotBlank(key)).isTrue();
    assertThat(Base64.isArrayByteBase64(key.getBytes())).isTrue();
  }

  @Test
  public void encrypt() throws Exception {
    AesECBCipher cipher = new AesECBCipher(pathToSecretKey());

    String encryptedText = cipher.encrypt("this is a secret");

    assertThat(StringUtils.isNotBlank(encryptedText)).isTrue();
    assertThat(Base64.isArrayByteBase64(encryptedText.getBytes())).isTrue();
  }

  @Test
  public void encrypt_bad_key() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/bad_secret_key.txt");
    AesECBCipher cipher = new AesECBCipher(new File(resource.toURI()).getCanonicalPath());

    assertThatThrownBy(() -> cipher.encrypt("this is a secret"))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Invalid AES key");
  }

  @Test
  public void decrypt() throws Exception {
    AesECBCipher cipher = new AesECBCipher(pathToSecretKey());

    // the following value has been encrypted with the key /org/sonar/api/config/internal/AesCipherTest/aes_secret_key.txt
    String clearText = cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");

    assertThat(clearText).isEqualTo("this is a secret");
  }

  @Test
  public void decrypt_bad_key() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/bad_secret_key.txt");
    AesECBCipher cipher = new AesECBCipher(new File(resource.toURI()).getCanonicalPath());

    try {
      cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
      fail();

    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(InvalidKeyException.class);
    }
  }

  @Test
  public void decrypt_other_key() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/other_secret_key.txt");
    AesECBCipher cipher = new AesECBCipher(new File(resource.toURI()).getCanonicalPath());

    try {
      // text encrypted with another key
      cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
      fail();

    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(BadPaddingException.class);
    }
  }

  @Test
  public void encryptThenDecrypt() throws Exception {
    AesECBCipher cipher = new AesECBCipher(pathToSecretKey());

    assertThat(cipher.decrypt(cipher.encrypt("foo"))).isEqualTo("foo");
  }

  @Test
  public void testDefaultPathToSecretKey() {
    AesECBCipher cipher = new AesECBCipher(null);

    String path = cipher.getPathToSecretKey();

    assertThat(StringUtils.isNotBlank(path)).isTrue();
    assertThat(new File(path)).hasName("sonar-secret.txt");
  }

  @Test
  public void loadSecretKeyFromFile() throws Exception {
    AesECBCipher cipher = new AesECBCipher(null);
    Key secretKey = cipher.loadSecretFileFromFile(pathToSecretKey());
    assertThat(secretKey.getAlgorithm()).isEqualTo("AES");
    assertThat(secretKey.getEncoded()).hasSizeGreaterThan(10);
  }

  @Test
  public void loadSecretKeyFromFile_trim_content() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/non_trimmed_secret_key.txt");
    String path = new File(resource.toURI()).getCanonicalPath();
    AesECBCipher cipher = new AesECBCipher(null);

    Key secretKey = cipher.loadSecretFileFromFile(path);

    assertThat(secretKey.getAlgorithm()).isEqualTo("AES");
    assertThat(secretKey.getEncoded()).hasSizeGreaterThan(10);
  }

  @Test
  public void loadSecretKeyFromFile_file_does_not_exist() throws Exception {
    AesECBCipher cipher = new AesECBCipher(null);

    assertThatThrownBy(() -> cipher.loadSecretFileFromFile("/file/does/not/exist"))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void loadSecretKeyFromFile_no_property() throws Exception {
    AesECBCipher cipher = new AesECBCipher(null);
    assertThatThrownBy(() -> cipher.loadSecretFileFromFile(null))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void hasSecretKey() throws Exception {
    AesECBCipher cipher = new AesECBCipher(pathToSecretKey());

    assertThat(cipher.hasSecretKey()).isTrue();
  }

  @Test
  public void doesNotHaveSecretKey() {
    AesECBCipher cipher = new AesECBCipher("/my/twitter/id/is/SimonBrandhof");

    assertThat(cipher.hasSecretKey()).isFalse();
  }

  private String pathToSecretKey() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/aes_secret_key.txt");
    return new File(resource.toURI()).getCanonicalPath();
  }
}
