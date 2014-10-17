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

import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;

public class SnapshotTesting {

  /**
   * When project is null, that means that the component is a project
   */
  public static SnapshotDto createForComponent(ComponentDto component, ComponentDto project) {
    return new SnapshotDto()
      .setResourceId(component.getId())
      .setRootProjectId(project.getId())
      .setLast(true);
  }

  public static SnapshotDto createForProject(ComponentDto project) {
    return new SnapshotDto()
      .setResourceId(project.getId())
      .setRootProjectId(project.getId())
      .setLast(true);
  }

  public static SnapshotDto defaultSnapshot() {
    return new SnapshotDto()
      .setResourceId(3L)
      .setRootProjectId(1L)
      .setParentId(2L)
      .setRootId(1L)
      .setStatus("P")
      .setLast(true)
      .setPurgeStatus(1)
      .setDepth(1)
      .setScope("DIR")
      .setQualifier("PAC")
      .setVersion("2.1-SNAPSHOT")
      .setPath("1.2.")
      .setPeriodMode(1, "days1")
      .setPeriodMode(2, "days2")
      .setPeriodMode(3, "days3")
      .setPeriodMode(4, "days4")
      .setPeriodMode(5, "days5")
      .setPeriodParam(1, "30")
      .setPeriodParam(2, "31")
      .setPeriodParam(3, "32")
      .setPeriodParam(4, "33")
      .setPeriodParam(5, "34")
      .setPeriodDate(1, DateUtils.parseDate("2011-09-24"))
      .setPeriodDate(2, DateUtils.parseDate("2011-09-25"))
      .setPeriodDate(3, DateUtils.parseDate("2011-09-26"))
      .setPeriodDate(4, DateUtils.parseDate("2011-09-27"))
      .setPeriodDate(5, DateUtils.parseDate("2011-09-28"))
      .setBuildDate(DateUtils.parseDate("2011-09-29"));
  }
}
