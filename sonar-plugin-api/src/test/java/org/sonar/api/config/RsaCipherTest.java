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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RsaCipherTest {
  @Test
  public void encrypt() throws IOException {
    Settings settings = new Settings();
    settings.setProperty("sonar.encryption.publicKey", loadPublicKey());
    RsaCipher cipher = new RsaCipher(settings);
    String encryptedText = cipher.encrypt("sonar");
    System.out.println(encryptedText);
    assertThat(StringUtils.isNotBlank(encryptedText), is(true));
    assertThat(Base64.isArrayByteBase64(encryptedText.getBytes()), is(true));
  }

  @Test
  public void decrypt() throws URISyntaxException, IOException {
    File file = new File(getClass().getResource("/org/sonar/api/config/RsaCipherTest/rsa_private_key.txt").toURI());
    Settings settings = new Settings();
    settings.setProperty("sonar.encryption.privateKeyPath", file.getCanonicalPath());
    RsaCipher cipher = new RsaCipher(settings);

    // the following value has been encrypted with the public key /org/sonar/api/config/RsaCipherTest/rsa_public_key.txt
    String clearText = cipher.decrypt("bnFlXnB5A8kLV4VR1FSGI4BmKd9I1E7euOQq/yB8a8RIpW34YYQX0toM5GTymY5EwkMO+KvfcpKXIvvhthr+5beW8v2nDux8n3VSH+tb+3wJZ+UYZQBQAQj2G8FVvYxbvRk3WVGn9bpw3x6195/gEneGvcG/A41/YsDHDce9zLw=");
    assertThat(clearText, is("this is a secret"));
  }

  @Test
  public void encryptThenDecrypt() throws URISyntaxException, IOException {
    File file = new File(getClass().getResource("/org/sonar/api/config/RsaCipherTest/rsa_private_key.txt").toURI());
    Settings settings = new Settings();
    settings.setProperty("sonar.encryption.publicKey", loadPublicKey());
    settings.setProperty("sonar.encryption.privateKeyPath", file.getCanonicalPath());
    RsaCipher cipher = new RsaCipher(settings);

    assertThat(cipher.decrypt(cipher.encrypt("foo")), is("foo"));
  }

  @Test
  public void loadPrivateKeyFromFile() throws Exception {
    File file = new File(getClass().getResource("/org/sonar/api/config/RsaCipherTest/rsa_private_key.txt").toURI());
    RsaCipher cipher = new RsaCipher(new Settings());
    PrivateKey privateKey = cipher.loadPrivateKeyFromFile(file.getPath());
    assertThat(privateKey.getAlgorithm(), is("RSA"));
  }

  @Test
  public void toPublicKey() throws Exception {
    RsaCipher cipher = new RsaCipher(new Settings());
    PublicKey publicKey = cipher.toPublicKey(loadPublicKey());
    assertThat(publicKey.getAlgorithm(), is("RSA"));
  }

  private String loadPublicKey() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/api/config/RsaCipherTest/rsa_public_key.txt");
    try {
      return IOUtils.toString(input);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
