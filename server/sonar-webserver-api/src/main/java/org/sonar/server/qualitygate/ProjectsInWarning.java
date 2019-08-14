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
package org.sonar.server.qualitygate;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Store number of projects in warning in order for the web service api/components/search to know if warning value should be return in the quality gate facet.
 * The value is updated each time the daemon {@link ProjectsInWarningDaemon} is executed
 */
public class ProjectsInWarning {

  private Long projectsInWarning;

  public void update(long projectsInWarning) {
    this.projectsInWarning = projectsInWarning;
  }

  public long count() {
    checkArgument(isInitialized(), "Initialization has not be done");
    return projectsInWarning;
  }

  boolean isInitialized() {
    return projectsInWarning != null;
  }
}
