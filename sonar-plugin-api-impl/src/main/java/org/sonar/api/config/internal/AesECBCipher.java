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

import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

/**
 * @deprecated since 8.7.0
 */
@Deprecated
final class AesECBCipher extends AesCipher {

  private static final String CRYPTO_ALGO = "AES";

  AesECBCipher(@Nullable String pathToSecretKey) {
    super(pathToSecretKey);
  }

  @Override
  public String encrypt(String clearText) {
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CRYPTO_ALGO);
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, loadSecretFile());
      byte[] cipherData = cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8.name()));
      return Base64.encodeBase64String(cipherData);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String decrypt(String encryptedText) {
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CRYPTO_ALGO);
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, loadSecretFile());
      byte[] cipherData = cipher.doFinal(Base64.decodeBase64(StringUtils.trim(encryptedText)));
      return new String(cipherData, StandardCharsets.UTF_8);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

}
