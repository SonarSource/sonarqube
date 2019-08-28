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
package org.sonar.db.component;

import org.apache.commons.lang.math.RandomUtils;
import org.sonar.core.util.Uuids;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class ProjectLinkTesting {

  public static ProjectLinkDto newProvidedLinkDto() {
    return newCommonLinkDto()
      .setName(null)
      .setType(ProjectLinkDto.PROVIDED_TYPES.get(RandomUtils.nextInt(ProjectLinkDto.PROVIDED_TYPES.size() - 1)));
  }

  public static ProjectLinkDto newCustomLinkDto() {
    String nameAndType = randomAlphabetic(20);
    return newCommonLinkDto()
      .setName(nameAndType)
      .setType(nameAndType);
  }

  private static ProjectLinkDto newCommonLinkDto() {
    return new ProjectLinkDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid(Uuids.createFast())
      .setHref(randomAlphanumeric(128))
      .setCreatedAt(System.currentTimeMillis())
      .setUpdatedAt(System.currentTimeMillis());
  }

}
