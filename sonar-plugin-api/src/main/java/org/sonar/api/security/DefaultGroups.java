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
package org.sonar.api.security;

import javax.annotation.Nullable;

/**
 * Name of the default user groups
 *
 * @since 3.2
 */
public final class DefaultGroups {

  public static final String ANYONE = "Anyone";
  public static final String ADMINISTRATORS = "sonar-administrators";
  public static final String USERS = "sonar-users";

  private DefaultGroups() {
    // only statics
  }

  public static boolean isAnyone(@Nullable String groupName) {
    return ANYONE.equalsIgnoreCase(groupName);
  }
}
