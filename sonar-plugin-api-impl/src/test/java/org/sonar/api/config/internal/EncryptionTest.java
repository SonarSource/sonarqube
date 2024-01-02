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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EncryptionTest {

  @Test
  public void isEncrypted() {
    Encryption encryption = new Encryption(null);
    assertThat(encryption.isEncrypted("{aes}ADASDASAD")).isTrue();
    assertThat(encryption.isEncrypted("{b64}ADASDASAD")).isTrue();
    assertThat(encryption.isEncrypted("{abc}ADASDASAD")).isTrue();

    assertThat(encryption.isEncrypted("{}")).isFalse();
    assertThat(encryption.isEncrypted("{foo")).isFalse();
    assertThat(encryption.isEncrypted("foo{aes}")).isFalse();
  }

  @Test
  public void scramble() {
    Encryption encryption = new Encryption(null);
    assertThat(encryption.scramble("foo")).isEqualTo("{b64}Zm9v");
  }

  @Test
  public void decrypt() {
    Encryption encryption = new Encryption(null);
    assertThat(encryption.decrypt("{b64}Zm9v")).isEqualTo("foo");
  }

  @Test
  public void loadSecretKey() throws Exception {
    Encryption encryption = new Encryption(null);
    encryption.setPathToSecretKey(pathToSecretKey());
    assertThat(encryption.hasSecretKey()).isTrue();
  }

  @Test
  public void generate_secret_key() {
    Encryption encryption = new Encryption(null);
    String key1 = encryption.generateRandomSecretKey();
    String key2 = encryption.generateRandomSecretKey();
    assertThat(key1).isNotEqualTo(key2);
  }

  @Test
  public void gcm_encryption() throws Exception {
    Encryption encryption = new Encryption(pathToSecretKey());
    String clearText = "this is a secrit";
    String cipherText = encryption.encrypt(clearText);
    String decryptedText = encryption.decrypt(cipherText);
    assertThat(cipherText)
      .startsWith("{aes-gcm}")
      .isNotEqualTo(clearText);
    assertThat(decryptedText).isEqualTo(clearText);
  }

  @Test
  public void decrypt_unknown_algorithm() {
    Encryption encryption = new Encryption(null);
    assertThat(encryption.decrypt("{xxx}Zm9v")).isEqualTo("{xxx}Zm9v");
  }

  @Test
  public void decrypt_uncrypted_text() {
    Encryption encryption = new Encryption(null);
    assertThat(encryption.decrypt("foo")).isEqualTo("foo");
  }

  @Test
  public void should_notDecryptText_whenBadBraceSyntax(){
    Encryption encryption = new Encryption(null);
    assertThat(encryption.decrypt("}xxx{Zm9v")).isEqualTo("}xxx{Zm9v");
    assertThat(encryption.decrypt("}dcd}59LK")).isEqualTo("}dcd}59LK");
    assertThat(encryption.decrypt("}rrrRg6")).isEqualTo("}rrrRg6");
    assertThat(encryption.decrypt("{closedjdk")).isEqualTo("{closedjdk");

  }

  private String pathToSecretKey() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/aes_secret_key.txt");
    return new File(resource.toURI()).getCanonicalPath();
  }
}
