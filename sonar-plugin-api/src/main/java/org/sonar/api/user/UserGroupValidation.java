/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.user;

import org.sonar.api.security.DefaultGroups;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class UserGroupValidation {

  public static final int GROUP_NAME_MAX_LENGTH = 255;

  private UserGroupValidation() {
    // Only static methods
  }

  public static void validateGroupName(String groupName) {
    checkArgument(!isNullOrEmpty(groupName) && groupName.trim().length() > 0, "Group cannot contain empty group name");
    checkArgument(groupName.length() <= GROUP_NAME_MAX_LENGTH, "Group name cannot be longer than %s characters", GROUP_NAME_MAX_LENGTH);
    checkArgument(!DefaultGroups.isAnyone(groupName), "Anyone group cannot be used");
  }
}
