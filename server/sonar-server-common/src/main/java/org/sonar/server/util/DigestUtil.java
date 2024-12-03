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
package org.sonar.server.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.util.encoders.Hex;

public class DigestUtil {

  private DigestUtil() {
    // utility class
  }

    public static String sha3_224Hex(String input) {
        try {
            Security.addProvider(new BouncyCastleFipsProvider());
            MessageDigest digest = MessageDigest.getInstance("SHA3-224", "BCFIPS");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Hex.toHexString(hashBytes);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Error while hashing with SHA3-224", e);
        } finally {
            Security.removeProvider(BouncyCastleFipsProvider.PROVIDER_NAME);
        }

    }

}
