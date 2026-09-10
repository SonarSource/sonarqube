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

import java.nio.charset.StandardCharsets;
import java.security.Key;
import javax.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

/**
 * Reads the values written before this algorithm was replaced. A key rotation has to keep them readable, so this falls
 * back to the key being replaced the same way the current algorithm does.
 *
 * @deprecated since 8.7.0
 */
@Deprecated
final class AesECBCipher extends AesCipher {

  private static final String CRYPTO_ALGO = "AES";

  AesECBCipher(@Nullable String pathToSecretKey) {
    super(pathToSecretKey);
  }

  AesECBCipher(@Nullable String pathToSecretKey, SecretKeySource secretKeySource) {
    super(pathToSecretKey, secretKeySource);
  }

  AesECBCipher(@Nullable String pathToSecretKey, SecretKeySource secretKeySource, SecretKeySource previousSecretKeySource) {
    super(pathToSecretKey, secretKeySource, previousSecretKeySource);
  }

  @Override
  public String encrypt(String clearText) {
    return failingAsIllegalState(() -> {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CRYPTO_ALGO);
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, loadSecretFile());
      byte[] cipherData = cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8.name()));
      return Base64.encodeBase64String(cipherData);
    });
  }

  @Override
  protected String decrypt(String encryptedText, Key secretKey) {
    return failingAsDecryptionFailure(() -> {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CRYPTO_ALGO);
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey);
      byte[] cipherData = cipher.doFinal(Base64.decodeBase64(StringUtils.trim(encryptedText)));
      return new String(cipherData, StandardCharsets.UTF_8);
    });
  }

}
