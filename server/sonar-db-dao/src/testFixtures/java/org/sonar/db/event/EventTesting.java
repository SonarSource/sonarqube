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
package org.sonar.db.event;

import org.sonar.db.component.SnapshotDto;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.RandomStringUtils.secure;

public class EventTesting {

  public static EventDto newEvent(SnapshotDto analysis) {
    requireNonNull(analysis.getUuid());
    requireNonNull(analysis.getRootComponentUuid());

    return new EventDto()
      .setAnalysisUuid(analysis.getUuid())
      .setComponentUuid(analysis.getRootComponentUuid())
      .setUuid(secure().nextAlphanumeric(40))
      .setName(secure().nextAlphanumeric(400))
      .setDescription(null)
      .setCategory("Other")
      .setCreatedAt(System.currentTimeMillis())
      .setDate(System.currentTimeMillis());
  }
}
