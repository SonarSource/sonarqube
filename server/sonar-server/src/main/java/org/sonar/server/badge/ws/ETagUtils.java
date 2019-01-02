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
package org.sonar.server.badge.ws;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ETagUtils {
  // Format for Expires Header
  static final String RFC1123_DATE = "EEE, dd MMM yyyy HH:mm:ss zzz";

  private static final long FNV1_INIT = 0xcbf29ce484222325L;
  private static final long FNV1_PRIME = 0x100000001b3L;

  private ETagUtils() {
    // Utility class no instantiation allowed
  }

  /**
   * hash method of a String independant of the JVM
   * FNV-1a hash method @see <a href="https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function#FNV-1a_hash"></a>
   */
  private static long hash(byte[] input) {
    long hash = FNV1_INIT;
    for (byte b : input) {
      hash ^= b & 0xff;
      hash *= FNV1_PRIME;
    }
    return hash;
  }

  /**
   * Calculate the ETag of the badge
   *
   * @see <a href="https://en.wikipedia.org/wiki/HTTP_ETag"></a>
   * <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.11"></a>
   */
  static String getETag(String output) {
    return "W/" + hash(output.getBytes(UTF_8));
  }
}
