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
package org.sonar.db.component;

import org.assertj.core.util.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

public class SnapshotTesting {

  /**
   * Can be used for modules and files
   */
  public static SnapshotDto createForComponent(ComponentDto component, SnapshotDto parentSnapshot) {
    checkNotNull(parentSnapshot.getId(), "The parent snapshot need to be persisted before creating this snapshot");
    Long parentRootId = parentSnapshot.getRootId();
    return createBasicSnapshot(component, parentSnapshot.getRootProjectId())
      .setRootId(parentRootId != null ? parentRootId : parentSnapshot.getId())
      .setParentId(parentSnapshot.getId())
      .setDepth(parentSnapshot.getDepth()+1)
      .setPath(
        Strings.isNullOrEmpty(parentSnapshot.getPath()) ? Long.toString(parentSnapshot.getId()) + "." : parentSnapshot.getPath() + Long.toString(parentSnapshot.getId()) + ".");
  }

  public static SnapshotDto newSnapshotForProject(ComponentDto project) {
    return createBasicSnapshot(project, project.getId())
      .setDepth(0)
      .setPath("");
  }

  public static SnapshotDto newSnapshotForView(ComponentDto view) {
    return createBasicSnapshot(view, view.getId())
      .setDepth(0)
      .setPath("");
  }

  public static SnapshotDto newSnapshotForDeveloper(ComponentDto developer) {
    return createBasicSnapshot(developer, developer.getId())
      .setDepth(0)
      .setPath("");
  }

  private static SnapshotDto createBasicSnapshot(ComponentDto component, Long rootProjectId) {
    checkNotNull(component.getId(), "The project need to be persisted before creating this snapshot");
    checkNotNull(rootProjectId, "Root project id is null");
    return new SnapshotDto()
      .setComponentId(component.getId())
      .setRootProjectId(rootProjectId)
      .setStatus(SnapshotDto.STATUS_PROCESSED)
      .setQualifier(component.qualifier())
      .setScope(component.scope())
      .setCreatedAt(System.currentTimeMillis())
      .setBuildDate(System.currentTimeMillis())
      .setLast(true);
  }

}
