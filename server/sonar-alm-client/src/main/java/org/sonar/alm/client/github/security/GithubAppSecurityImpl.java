/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client.github.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GithubAppSecurityImpl implements GithubAppSecurity {

  private final Clock clock;

  public GithubAppSecurityImpl(Clock clock) {
    this.clock = clock;
  }

  @Override
  public AppToken createAppToken(long appId, String privateKey) {
    Algorithm algorithm = readApplicationPrivateKey(appId, privateKey);
    LocalDateTime now = LocalDateTime.now(clock);
    // Expiration period is configurable and could be greater if needed.
    // See https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#authenticating-as-a-github-app
    LocalDateTime expiresAt = now.plus(AppToken.EXPIRATION_PERIOD_IN_MINUTES, ChronoUnit.MINUTES);
    ZoneOffset offset = clock.getZone().getRules().getOffset(now);
    Date nowDate = Date.from(now.toInstant(offset));
    Date expiresAtDate = Date.from(expiresAt.toInstant(offset));
    JWTCreator.Builder builder = JWT.create()
      .withIssuer(String.valueOf(appId))
      .withIssuedAt(nowDate)
      .withExpiresAt(expiresAtDate);
    return new AppToken(builder.sign(algorithm));
  }

  private static Algorithm readApplicationPrivateKey(long appId, String encodedPrivateKey) {
    byte[] decodedPrivateKey = encodedPrivateKey.getBytes(UTF_8);
    try (PemReader pemReader = new PemReader(new InputStreamReader(new ByteArrayInputStream(decodedPrivateKey)))) {
      Security.addProvider(new BouncyCastleProvider());

      PemObject pemObject = pemReader.readPemObject();
      if (pemObject == null) {
        throw new IllegalArgumentException("Failed to decode Github Application private key");
      }

      PKCS8EncodedKeySpec keySpec1 = new PKCS8EncodedKeySpec(pemObject.getContent());
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      PrivateKey privateKey = keyFactory.generatePrivate(keySpec1);
      return Algorithm.RSA256(new RSAKeyProvider() {
        @Override
        public RSAPublicKey getPublicKeyById(String keyId) {
          throw new UnsupportedOperationException("getPublicKeyById not implemented");
        }

        @Override
        public RSAPrivateKey getPrivateKey() {
          return (RSAPrivateKey) privateKey;
        }

        @Override
        public String getPrivateKeyId() {
          return "github_app_" + appId;
        }
      });
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid Github Application private key", e);
    } finally {
      Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }
  }

}
