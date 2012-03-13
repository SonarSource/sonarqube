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
import com.google.common.base.Throwables;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

final class RsaCipher extends Cipher {

  private final Settings settings;

  RsaCipher(Settings settings) {
    this.settings = settings;
  }

  String encrypt(String clearText) {
    String publicKey = settings.getClearString(CoreProperties.ENCRYPTION_PUBLIC_KEY);
    if (StringUtils.isBlank(publicKey)) {
      throw new IllegalStateException("RSA public key is missing. Please generate one.");
    }
    return encrypt(clearText, publicKey);
  }

  private String encrypt(String clearText, String publicKey) {
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, toPublicKey(publicKey));
      return new String(Base64.encodeBase64(cipher.doFinal(clearText.getBytes())));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  String decrypt(String encryptedText) {
    try {
      PrivateKey privateKey = loadPrivateKey();
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
      byte[] cipherData = cipher.doFinal(Base64.decodeBase64(StringUtils.trim(encryptedText)));
      return new String(cipherData);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private PrivateKey loadPrivateKey() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException {
    String path = settings.getClearString(CoreProperties.ENCRYPTION_PATH_TO_PRIVATE_KEY);
    return loadPrivateKeyFromFile(path);
  }

  @VisibleForTesting
  PrivateKey loadPrivateKeyFromFile(String path) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidKeyException {
    if (StringUtils.isBlank(path)) {
      throw new IllegalStateException("Impossible to decrypt text without the private key. Please set the property " + CoreProperties.ENCRYPTION_PATH_TO_PRIVATE_KEY);
    }
    File file = new File(path);
    if (!file.exists() || !file.isFile()) {
      throw new IllegalStateException("The property " + CoreProperties.ENCRYPTION_PATH_TO_PRIVATE_KEY + " does not link to a valid file: " + path);
    }

    String s = FileUtils.readFileToString(file);
    if (StringUtils.isBlank(s)) {
      throw new IllegalStateException("No private key in the file: " + path);
    }
    String[] fields = StringUtils.split(StringUtils.trim(s), ",");
    if (fields.length != 2) {
      throw new IllegalStateException("Badly formatted private key in the file: " + path);
    }
    BigInteger modulus = new BigInteger(fields[0]);
    BigInteger exponent = new BigInteger(fields[1]);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
  }

  @VisibleForTesting
  PublicKey toPublicKey(String text) throws InvalidKeySpecException, NoSuchAlgorithmException {
    if (StringUtils.isBlank(text)) {
      throw new IllegalArgumentException("The public key is blank");
    }
    String[] fields = StringUtils.split(StringUtils.trim(text), ",");
    if (fields.length != 2) {
      throw new IllegalStateException("Unknown format of public key: " + text);
    }
    BigInteger modulus = new BigInteger(fields[0]);
    BigInteger exponent = new BigInteger(fields[1]);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
  }

  String[] generateRandomKeys() {
    try {
      KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
      gen.initialize(1024, new SecureRandom());
      KeyPair pair = gen.generateKeyPair();

      KeyFactory fact = KeyFactory.getInstance("RSA");
      RSAPublicKeySpec pub = fact.getKeySpec(pair.getPublic(), RSAPublicKeySpec.class);
      RSAPrivateKeySpec priv = fact.getKeySpec(pair.getPrivate(), RSAPrivateKeySpec.class);

      String publicKey = pub.getModulus() + "," + pub.getPublicExponent();
      String privateKey = priv.getModulus() + "," + priv.getPrivateExponent();
      return new String[]{publicKey, privateKey};

    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate random RSA keys", e);
    }
  }
}
