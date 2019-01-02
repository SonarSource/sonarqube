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
package org.sonar.server.project;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public final class RekeyedProject {
  private final Project project;
  private final String previousKey;

  public RekeyedProject(Project project, String previousKey) {
    this.project = checkNotNull(project, "project can't be null");
    this.previousKey = checkNotNull(previousKey, "previousKey can't be null");
  }

  public Project getProject() {
    return project;
  }

  public String getPreviousKey() {
    return previousKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RekeyedProject that = (RekeyedProject) o;
    return Objects.equals(project, that.project) &&
      Objects.equals(previousKey, that.previousKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(project, previousKey);
  }

  @Override
  public String toString() {
    return "RekeyedProject{" +
      "project=" + project +
      ", previousKey='" + previousKey + '\'' +
      '}';
  }
}
