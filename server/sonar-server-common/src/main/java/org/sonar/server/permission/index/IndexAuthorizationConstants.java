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
package org.sonar.server.permission.index;

public final class IndexAuthorizationConstants {
  public static final String TYPE_AUTHORIZATION = "auth";
  public static final String FIELD_GROUP_IDS = TYPE_AUTHORIZATION + "_groupIds";
  public static final String FIELD_USER_IDS = TYPE_AUTHORIZATION + "_userIds";
  /**
   * When true, then anybody can access to the project. In that case
   * it's useless to store granted groups and users. The related
   * fields are empty.
   */
  public static final String FIELD_ALLOW_ANYONE = TYPE_AUTHORIZATION + "_allowAnyone";

  private IndexAuthorizationConstants() {
    // prevents instantiation
  }
}
