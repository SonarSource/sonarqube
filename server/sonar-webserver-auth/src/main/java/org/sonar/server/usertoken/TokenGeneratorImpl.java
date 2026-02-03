/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.usertoken;

import java.security.SecureRandom;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.db.user.TokenType;

public class TokenGeneratorImpl implements TokenGenerator {

  private static final String SONARQUBE_TOKEN_PREFIX = "sq";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @Override
  public String generate(TokenType tokenType) {
    String rawToken = generateRawToken();
    return buildIdentifiablePartOfToken(tokenType) + rawToken;
  }

  private static String buildIdentifiablePartOfToken(TokenType tokenType) {
    return SONARQUBE_TOKEN_PREFIX + tokenType.getIdentifier() + "_";
  }

  private static String generateRawToken() {
    byte[] randomBytes = new byte[20];
    SECURE_RANDOM.nextBytes(randomBytes);
    return Hex.encodeHexString(randomBytes);
  }

  @Override
  public String hash(String token) {
    return DigestUtils.sha384Hex(token);
  }
}
