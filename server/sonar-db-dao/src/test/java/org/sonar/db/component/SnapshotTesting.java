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
package org.sonar.db.component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.RandomStringUtils.randomAscii;

public class SnapshotTesting {

  public static SnapshotDto newAnalysis(ComponentDto rootComponent) {
    checkNotNull(rootComponent.uuid(), "Project UUID must be set");
    checkArgument(rootComponent.uuid().equals(rootComponent.projectUuid()), "Component is not a tree root");
    return new SnapshotDto()
      .setUuid(randomAlphanumeric(40))
      .setComponentUuid(rootComponent.uuid())
      .setStatus(SnapshotDto.STATUS_PROCESSED)
      .setCreatedAt(System.currentTimeMillis())
      .setBuildDate(System.currentTimeMillis())
      .setLast(true);
  }

  public static SnapshotDto newSnapshot() {
    return new SnapshotDto()
      .setUuid(randomAlphanumeric(40))
      .setComponentUuid(randomAlphanumeric(40))
      .setStatus(randomAscii(1))
      .setCreatedAt(System.currentTimeMillis())
      .setBuildDate(System.currentTimeMillis())
      .setLast(true);
  }
}
