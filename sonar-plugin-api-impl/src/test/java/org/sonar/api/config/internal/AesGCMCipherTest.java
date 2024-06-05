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
import javax.crypto.BadPaddingException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AesGCMCipherTest {

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
    AesGCMCipher cipher = new AesGCMCipher(new File(resource.toURI()).getCanonicalPath());

    assertThatThrownBy(() -> cipher.encrypt("this is a secret"))
      .hasCauseInstanceOf(InvalidKeyException.class);
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
    AesGCMCipher cipher = new AesGCMCipher(new File(resource.toURI()).getCanonicalPath());

    assertThatThrownBy(() -> cipher.decrypt("9mx5Zq4JVyjeChTcVjEide4kWCwusFl7P2dSVXtg9IY="))
      .hasCauseInstanceOf(InvalidKeyException.class);
  }

  @Test
  public void decrypt_other_key() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/other_secret_key.txt");
    AesGCMCipher originalCipher = new AesGCMCipher(pathToSecretKey());
    AesGCMCipher cipher = new AesGCMCipher(new File(resource.toURI()).getCanonicalPath());

    assertThatThrownBy(() -> cipher.decrypt(originalCipher.encrypt("this is a secret")))
      .hasCauseInstanceOf(BadPaddingException.class);
  }

  private String pathToSecretKey() throws Exception {
    URL resource = getClass().getResource("/org/sonar/api/config/internal/AesCipherTest/aes_secret_key.txt");
    return new File(resource.toURI()).getCanonicalPath();
  }
}
