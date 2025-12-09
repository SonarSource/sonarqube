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
package org.sonar.db.component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAscii;
import static org.apache.commons.lang3.RandomStringUtils.secure;

public class SnapshotTesting {

  private SnapshotTesting() {
    // nothing to do
  }

  public static SnapshotDto newAnalysis(ComponentDto rootComponent) {
    checkNotNull(rootComponent.uuid(), "Project UUID must be set");
    checkArgument(rootComponent.uuid().equals(rootComponent.branchUuid()), "Component is not a tree root");
    return newAnalysis(rootComponent.uuid());
  }

  public static SnapshotDto newAnalysis(BranchDto branchDto) {
    checkNotNull(branchDto.getUuid(), "Project UUID must be set");
    return newAnalysis(branchDto.getUuid());
  }

  public static SnapshotDto newAnalysis(String uuid) {
    return new SnapshotDto()
      .setUuid(secure().nextAlphanumeric(40))
      .setRootComponentUuid(uuid)
      .setStatus(SnapshotDto.STATUS_PROCESSED)
      .setCreatedAt(System.currentTimeMillis())
      .setAnalysisDate(System.currentTimeMillis())
      .setRevision(secure().nextAlphanumeric(50))
      .setLast(true);
  }

  public static SnapshotDto newSnapshot() {
    return new SnapshotDto()
      .setUuid(secure().nextAlphanumeric(40))
      .setRootComponentUuid(secure().nextAlphanumeric(40))
      .setStatus(randomAscii(1))
      .setCreatedAt(System.currentTimeMillis())
      .setAnalysisDate(System.currentTimeMillis())
      .setLast(true);
  }
}
