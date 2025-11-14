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
package org.sonar.db.user;

import org.sonar.api.utils.System2;

import static org.apache.commons.lang3.RandomStringUtils.secure;

public class UserTokenTesting {

  private static final long NOW = System2.INSTANCE.now();

  private UserTokenTesting() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static UserTokenDto newUserToken() {
    return new UserTokenDto()
      .setUserUuid("userUuid_" + secure().nextAlphanumeric(40))
      .setName("name_" + secure().nextAlphanumeric(20))
      .setTokenHash("hash_" + secure().nextAlphanumeric(30))
      .setCreatedAt(NOW)
      .setType("USER_TOKEN");
  }

  public static UserTokenDto newProjectAnalysisToken() {
    return new UserTokenDto()
      .setUserUuid("userUuid_" + secure().nextAlphanumeric(40))
      .setName("name_" + secure().nextAlphanumeric(20))
      .setTokenHash("hash_" + secure().nextAlphanumeric(30))
      .setProjectUuid("projectUuid_" + secure().nextAlphanumeric(20))
      .setProjectKey("projectKey_" + secure().nextAlphanumeric(40))
      .setProjectName("Project " + secure().nextAlphanumeric(40))
      .setCreatedAt(NOW)
      .setType("PROJECT_ANALYSIS_TOKEN");
  }

}
