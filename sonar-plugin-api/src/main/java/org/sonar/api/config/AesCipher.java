/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

final class AesCipher extends Cipher {

  // Can't be increased because of Java 6 policy files :
  // https://confluence.terena.org/display/~visser/No+256+bit+ciphers+for+Java+apps
  // http://java.sun.com/javase/6/webnotes/install/jre/README
  public static final int KEY_SIZE_IN_BITS = 128;

  private static final String CRYPTO_KEY = "AES";

  private final Settings settings;

  AesCipher(Settings settings) {
    this.settings = settings;
  }

  String encrypt(String clearText) {
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CRYPTO_KEY);
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, loadSecretFile());
      return new String(Base64.encodeBase64(cipher.doFinal(clearText.getBytes(Charsets.UTF_8))));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  String decrypt(String encryptedText) {
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CRYPTO_KEY);
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, loadSecretFile());
      byte[] cipherData = cipher.doFinal(Base64.decodeBase64(StringUtils.trim(encryptedText)));
      return new String(cipherData);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
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

  private Key loadSecretFile() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidKeyException {
    String path = getPathToSecretKey();
    return loadSecretFileFromFile(path);
  }

  @VisibleForTesting
  Key loadSecretFileFromFile(String path) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidKeyException {
    if (StringUtils.isBlank(path)) {
      throw new IllegalStateException("Secret key not found. Please set the property " + CoreProperties.ENCRYPTION_SECRET_KEY_FILE);
    }
    File file = new File(path);
    if (!file.exists() || !file.isFile()) {
      throw new IllegalStateException("The property " + CoreProperties.ENCRYPTION_SECRET_KEY_FILE + " does not link to a valid file: " + path);
    }
    String s = FileUtils.readFileToString(file);
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
      return new String(Base64.encodeBase64(secretKey.getEncoded()));

    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate secret key", e);
    }
  }

  private String getPathToSecretKey() {
    return settings.getClearString(CoreProperties.ENCRYPTION_SECRET_KEY_FILE);
  }
}
