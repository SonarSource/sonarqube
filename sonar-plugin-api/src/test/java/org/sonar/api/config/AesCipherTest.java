/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.config;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;

import javax.crypto.BadPaddingException;
import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AesCipherTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void generateRandomSecretKey() {
    AesCipher cipher = new AesCipher(new Settings());

    String key = cipher.generateRandomSecretKey();

    assertThat(StringUtils.isNotBlank(key), is(true));
    assertThat(Base64.isArrayByteBase64(key.getBytes()), is(true));
  }

  @Test
  public void encrypt() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_SECRET_KEY_PATH, pathToSecretKey());
    AesCipher cipher = new AesCipher(settings);

    String encryptedText = cipher.encrypt("this is a secret");

    assertThat(StringUtils.isNotBlank(encryptedText), is(true));
    assertThat(Base64.isArrayByteBase64(encryptedText.getBytes()), is(true));
  }

  @Test
  public void encrypt_bad_key() throws Exception {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage("Invalid AES key");

    URL resource = getClass().getResource("/org/sonar/api/config/AesCipherTest/bad_secret_key.txt");
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_SECRET_KEY_PATH, new File(resource.toURI()).getCanonicalPath());
    AesCipher cipher = new AesCipher(settings);

    cipher.encrypt("this is a secret");
  }

  @Test
  public void decrypt() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_SECRET_KEY_PATH, pathToSecretKey());
    AesCipher cipher = new AesCipher(settings);

    // the following value has been encrypted with the key /org/sonar/api/config/AesCipherTest/aes_secret_key.txt
    String clearText = cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");

    assertThat(clearText, is("this is a secret"));
  }

  @Test
  public void decrypt_bad_key() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/AesCipherTest/bad_secret_key.txt");
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_SECRET_KEY_PATH, new File(resource.toURI()).getCanonicalPath());
    AesCipher cipher = new AesCipher(settings);

    try {
      cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
      fail();

    } catch (RuntimeException e) {
      assertThat(e.getCause(), is(InvalidKeyException.class));
    }
  }

  @Test
  public void decrypt_other_key() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/AesCipherTest/other_secret_key.txt");
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_SECRET_KEY_PATH, new File(resource.toURI()).getCanonicalPath());
    AesCipher cipher = new AesCipher(settings);

    try {
      // text encrypted with another key
      cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
      fail();

    } catch (RuntimeException e) {
      assertThat(e.getCause(), is(BadPaddingException.class));
    }
  }

  @Test
  public void encryptThenDecrypt() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_SECRET_KEY_PATH, pathToSecretKey());
    AesCipher cipher = new AesCipher(settings);

    assertThat(cipher.decrypt(cipher.encrypt("foo")), is("foo"));
  }

  @Test
  public void testDefaultPathToSecretKey() {
    AesCipher cipher = new AesCipher(new Settings());

    String path = cipher.getPathToSecretKey();

    assertThat(StringUtils.isNotBlank(path), is(true));
    assertThat(new File(path).getName(), is("sonar-secret.txt"));
  }

  @Test
  public void loadSecretKeyFromFile() throws Exception {
    AesCipher cipher = new AesCipher(new Settings());
    Key secretKey = cipher.loadSecretFileFromFile(pathToSecretKey());
    assertThat(secretKey.getAlgorithm(), is("AES"));
    assertThat(secretKey.getEncoded().length, greaterThan(10));
  }

  @Test
  public void loadSecretKeyFromFile_trim_content() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/AesCipherTest/non_trimmed_secret_key.txt");
    String path = new File(resource.toURI()).getCanonicalPath();
    AesCipher cipher = new AesCipher(new Settings());

    Key secretKey = cipher.loadSecretFileFromFile(path);

    assertThat(secretKey.getAlgorithm(), is("AES"));
    assertThat(secretKey.getEncoded().length, greaterThan(10));
  }

  @Test
  public void loadSecretKeyFromFile_file_does_not_exist() throws Exception {
    thrown.expect(IllegalStateException.class);

    AesCipher cipher = new AesCipher(new Settings());
    cipher.loadSecretFileFromFile("/file/does/not/exist");
  }

  @Test
  public void loadSecretKeyFromFile_no_property() throws Exception {
    thrown.expect(IllegalStateException.class);

    AesCipher cipher = new AesCipher(new Settings());
    cipher.loadSecretFileFromFile(null);
  }

  @Test
  public void hasSecretKey() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_SECRET_KEY_PATH, pathToSecretKey());
    AesCipher cipher = new AesCipher(settings);

    assertThat(cipher.hasSecretKey(), Matchers.is(true));
  }

  @Test
  public void doesNotHaveSecretKey() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.ENCRYPTION_SECRET_KEY_PATH, "/my/twitter/id/is/SimonBrandhof");
    AesCipher cipher = new AesCipher(settings);

    assertThat(cipher.hasSecretKey(), Matchers.is(false));
  }


  private String pathToSecretKey() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/AesCipherTest/aes_secret_key.txt");
    return new File(resource.toURI()).getCanonicalPath();
  }
}
