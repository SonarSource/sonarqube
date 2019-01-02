/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * @since 3.0
 */
public final class Encryption {

  private static final String BASE64_ALGORITHM = "b64";

  private static final String AES_ALGORITHM = "aes";
  private final AesCipher aesCipher;

  private final Map<String, Cipher> ciphers;
  private static final Pattern ENCRYPTED_PATTERN = Pattern.compile("\\{(.*?)\\}(.*)");

  public Encryption(@Nullable String pathToSecretKey) {
    aesCipher = new AesCipher(pathToSecretKey);
    ciphers = new HashMap<>();
    ciphers.put(BASE64_ALGORITHM, new Base64Cipher());
    ciphers.put(AES_ALGORITHM, aesCipher);
  }

  public void setPathToSecretKey(@Nullable String pathToSecretKey) {
    aesCipher.setPathToSecretKey(pathToSecretKey);
  }

  /**
   * Checks the availability of the secret key, that is required to encrypt and decrypt.
   */
  public boolean hasSecretKey() {
    return aesCipher.hasSecretKey();
  }

  public boolean isEncrypted(String value) {
    return value.indexOf('{') == 0 && value.indexOf('}') > 1;
  }

  public String encrypt(String clearText) {
    return encrypt(AES_ALGORITHM, clearText);
  }

  public String scramble(String clearText) {
    return encrypt(BASE64_ALGORITHM, clearText);
  }

  public String generateRandomSecretKey() {
    return aesCipher.generateRandomSecretKey();
  }

  public String decrypt(String encryptedText) {
    Matcher matcher = ENCRYPTED_PATTERN.matcher(encryptedText);
    if (matcher.matches()) {
      Cipher cipher = ciphers.get(matcher.group(1).toLowerCase(Locale.ENGLISH));
      if (cipher != null) {
        return cipher.decrypt(matcher.group(2));
      }
    }
    return encryptedText;
  }

  private String encrypt(String algorithm, String clearText) {
    Cipher cipher = ciphers.get(algorithm);
    if (cipher == null) {
      throw new IllegalArgumentException("Unknown cipher algorithm: " + algorithm);
    }
    return String.format("{%s}%s", algorithm, cipher.encrypt(clearText));
  }
}
