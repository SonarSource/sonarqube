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
import java.io.IOException;
import java.security.Key;
import java.security.SecureRandom;
import javax.annotation.Nullable;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.api.CoreProperties.ENCRYPTION_SECRET_KEY_PATH;

abstract class AesCipher implements Cipher {
  static final int KEY_SIZE_IN_BITS = 256;

  private static final String CRYPTO_KEY = "AES";

  private String pathToSecretKey;

  AesCipher(@Nullable String pathToSecretKey) {
    this.pathToSecretKey = pathToSecretKey;
  }

  /**
   * This method checks the existence of the file, but not the validity of the contained key.
   */
  boolean hasSecretKey() {
    String path = getPathToSecretKey();
    if (StringUtils.isNotBlank(path)) {
      File file = new File(path);
      return file.exists() && file.isFile();
    }
    return false;
  }

  protected Key loadSecretFile() throws IOException {
    String path = getPathToSecretKey();
    return loadSecretFileFromFile(path);
  }

  Key loadSecretFileFromFile(@Nullable String path) throws IOException {
    if (StringUtils.isBlank(path)) {
      throw new IllegalStateException("Secret key not found. Please set the property " + ENCRYPTION_SECRET_KEY_PATH);
    }
    File file = new File(path);
    if (!file.exists() || !file.isFile()) {
      throw new IllegalStateException("The property " + ENCRYPTION_SECRET_KEY_PATH + " does not link to a valid file: " + path);
    }
    String s = FileUtils.readFileToString(file, UTF_8);
    if (StringUtils.isBlank(s)) {
      throw new IllegalStateException("No secret key in the file: " + path);
    }
    return new SecretKeySpec(Base64.decodeBase64(StringUtils.trim(s)), CRYPTO_KEY);
  }

  String generateRandomSecretKey() {
    try {
      KeyGenerator keyGen = KeyGenerator.getInstance(CRYPTO_KEY);
      keyGen.init(KEY_SIZE_IN_BITS, new SecureRandom());
      SecretKey secretKey = keyGen.generateKey();
      return Base64.encodeBase64String(secretKey.getEncoded());

    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate secret key", e);
    }
  }

  String getPathToSecretKey() {
    if (StringUtils.isBlank(pathToSecretKey)) {
      pathToSecretKey = new File(FileUtils.getUserDirectoryPath(), ".sonar/sonar-secret.txt").getPath();
    }
    return pathToSecretKey;
  }

  public void setPathToSecretKey(@Nullable String pathToSecretKey) {
    this.pathToSecretKey = pathToSecretKey;
  }
}
