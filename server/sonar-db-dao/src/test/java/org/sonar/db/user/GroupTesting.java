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
package org.sonar.db.user;

import java.util.Date;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.apache.commons.lang.math.RandomUtils.nextLong;

public class GroupTesting {

  private GroupTesting() {
    // only statics
  }

  public static GroupDto newGroupDto() {
    GroupDto group = new GroupDto()
      .setId(nextInt())
      .setOrganizationUuid(randomAlphanumeric(40))
      .setName(randomAlphanumeric(255))
      .setDescription(randomAlphanumeric(200))
      .setCreatedAt(new Date(nextLong()))
      .setUpdatedAt(new Date(nextLong()));
    return group;
  }
}
