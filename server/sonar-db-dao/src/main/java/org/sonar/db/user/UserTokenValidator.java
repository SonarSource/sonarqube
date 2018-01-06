/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.user;

import static com.google.common.base.Preconditions.checkState;

public class UserTokenValidator {
  private static final int MAX_TOKEN_HASH_LENGTH = 255;

  private UserTokenValidator() {
    // utility methods
  }

  static String checkTokenHash(String hash) {
    checkState(hash.length() <= MAX_TOKEN_HASH_LENGTH, "Token hash length (%s) is longer than the maximum authorized (%s)", hash.length(), MAX_TOKEN_HASH_LENGTH);
    return hash;
  }
}
