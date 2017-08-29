/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.process;

import com.google.common.io.Resources;
import java.io.File;
import java.security.InvalidKeyException;
import java.security.Key;
import javax.crypto.BadPaddingException;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AesCipherTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void decrypt() {
    AesCipher cipher = new AesCipher(pathToSecretKey());

    // the following value has been encrypted with the key /org/sonar/api/config/AesCipherTest/aes_secret_key.txt
    String clearText = cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");

    assertThat(clearText).isEqualTo("this is a secret");
  }

  @Test
  public void decrypt_bad_key() {
    AesCipher cipher = new AesCipher(getPath("bad_secret_key.txt"));

    try {
      cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
      fail();

    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(InvalidKeyException.class);
    }
  }

  @Test
  public void decrypt_other_key() {
    AesCipher cipher = new AesCipher(getPath("other_secret_key.txt"));

    try {
      // text encrypted with another key
      cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY=");
      fail();

    } catch (RuntimeException e) {
      assertThat(e.getCause()).isInstanceOf(BadPaddingException.class);
    }
  }

  @Test
  public void testDefaultPathToSecretKey() {
    AesCipher cipher = new AesCipher(null);

    String path = cipher.getPathToSecretKey();

    assertThat(StringUtils.isNotBlank(path)).isTrue();
    assertThat(new File(path).getName()).isEqualTo("sonar-secret.txt");
  }

  @Test
  public void loadSecretKeyFromFile() throws Exception {
    AesCipher cipher = new AesCipher(null);
    Key secretKey = cipher.loadSecretFileFromFile(pathToSecretKey());
    assertThat(secretKey.getAlgorithm()).isEqualTo("AES");
    assertThat(secretKey.getEncoded().length).isGreaterThan(10);
  }

  @Test
  public void loadSecretKeyFromFile_trim_content() throws Exception {
    String path = getPath("non_trimmed_secret_key.txt");
    AesCipher cipher = new AesCipher(null);

    Key secretKey = cipher.loadSecretFileFromFile(path);

    assertThat(secretKey.getAlgorithm()).isEqualTo("AES");
    assertThat(secretKey.getEncoded().length).isGreaterThan(10);
  }

  @Test
  public void loadSecretKeyFromFile_file_does_not_exist() throws Exception {
    thrown.expect(IllegalStateException.class);

    AesCipher cipher = new AesCipher(null);
    cipher.loadSecretFileFromFile("/file/does/not/exist");
  }

  @Test
  public void loadSecretKeyFromFile_no_property() throws Exception {
    thrown.expect(IllegalStateException.class);

    AesCipher cipher = new AesCipher(null);
    cipher.loadSecretFileFromFile(null);
  }

  private static String getPath(String file) {
    return Resources.getResource(AesCipherTest.class, "AesCipherTest/" + file).getPath();
  }

  private static String pathToSecretKey() {
    return getPath("aes_secret_key.txt");
  }

}
