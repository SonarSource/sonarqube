/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.component;

import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;

public class SnapshotTesting {

  /**
   * Can be used for modules and files
   */
  public static SnapshotDto createForComponent(ComponentDto component, SnapshotDto parentSnapshot) {
    Long parentRootId = parentSnapshot.getRootId();
    return new SnapshotDto()
      .setResourceId(component.getId())
      .setRootProjectId(parentSnapshot.getRootProjectId())
      .setRootId(parentRootId != null ? parentRootId : parentSnapshot.getId())
      .setStatus(SnapshotDto.STATUS_PROCESSED)
      .setQualifier(component.qualifier())
      .setScope(component.scope())
      .setParentId(parentSnapshot.getId())
      .setPath(parentSnapshot.getPath() == null ? Long.toString(parentSnapshot.getId()) + "." : parentSnapshot.getPath() + Long.toString(parentSnapshot.getId()) + ".")
      .setLast(true)
      .setBuildDate(System.currentTimeMillis());
  }

  public static SnapshotDto createForProject(ComponentDto project) {
    return new SnapshotDto()
      .setResourceId(project.getId())
      .setRootProjectId(project.getId())
      .setStatus(SnapshotDto.STATUS_PROCESSED)
      .setQualifier(project.qualifier())
      .setScope(project.scope())
      .setPath("")
      .setLast(true)
      .setBuildDate(System.currentTimeMillis());
  }

}
