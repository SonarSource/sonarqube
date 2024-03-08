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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.annotation.Nullable;
import javax.crypto.spec.GCMParameterSpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

final class AesGCMCipher extends AesCipher {
  private static final int GCM_TAG_LENGTH_IN_BITS = 128;
  private static final int GCM_IV_LENGTH_IN_BYTES = 12;

  private static final String CRYPTO_ALGO = "AES/GCM/NoPadding";

  AesGCMCipher(@Nullable String pathToSecretKey) {
    super(pathToSecretKey);
  }

  @Override
  public String encrypt(String clearText) {
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CRYPTO_ALGO);
      byte[] iv = new byte[GCM_IV_LENGTH_IN_BYTES];
      new SecureRandom().nextBytes(iv);
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, loadSecretFile(), new GCMParameterSpec(GCM_TAG_LENGTH_IN_BITS, iv));
      byte[] encryptedText = cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8.name()));
      return Base64.encodeBase64String(
        ByteBuffer.allocate(GCM_IV_LENGTH_IN_BYTES + encryptedText.length)
          .put(iv)
          .put(encryptedText)
          .array());
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
      ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.decodeBase64(StringUtils.trim(encryptedText)));
      byte[] iv = new byte[GCM_IV_LENGTH_IN_BYTES];
      byteBuffer.get(iv);
      byte[] cipherText = new byte[byteBuffer.remaining()];
      byteBuffer.get(cipherText);
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, loadSecretFile(), new GCMParameterSpec(GCM_TAG_LENGTH_IN_BITS, iv));
      byte[] cipherData = cipher.doFinal(cipherText);
      return new String(cipherData, StandardCharsets.UTF_8);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
