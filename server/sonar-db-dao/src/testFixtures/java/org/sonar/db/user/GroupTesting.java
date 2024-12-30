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
package org.sonar.db.user;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;

import static org.apache.commons.lang3.RandomStringUtils.secure;

public class GroupTesting {

  private static final Random RANDOM = new SecureRandom();

  private GroupTesting() {
    // only statics
  }

  public static GroupDto newGroupDto() {
    return new GroupDto()
      .setUuid(secure().nextAlphanumeric(40))
      .setName(secure().nextAlphanumeric(255))
      .setDescription(secure().nextAlphanumeric(200))
      .setCreatedAt(new Date(RANDOM.nextLong(Long.MAX_VALUE)))
      .setUpdatedAt(new Date(RANDOM.nextLong(Long.MAX_VALUE)));
  }
}
