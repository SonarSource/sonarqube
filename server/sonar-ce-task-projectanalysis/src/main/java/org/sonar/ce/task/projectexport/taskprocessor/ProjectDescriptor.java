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
package org.sonar.ce.task.projectexport.taskprocessor;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.project.ProjectDto;

import static java.util.Objects.requireNonNull;

@Immutable
public class ProjectDescriptor {
  private final String uuid;
  private final String key;
  private final String name;

  public ProjectDescriptor(String uuid, String key, String name) {
    this.uuid = requireNonNull(uuid);
    this.key = requireNonNull(key);
    this.name = requireNonNull(name);
  }

  /**
   * Build a {@link ProjectDescriptor} without checking qualifier of ComponentDto.
   */
  public static ProjectDescriptor of(ProjectDto project) {
    return new ProjectDescriptor(project.getUuid(), project.getKey(), project.getName());
  }

  public final String getUuid() {
    return uuid;
  }

  public final String getKey() {
    return key;
  }

  public final String getName() {
    return name;
  }

  @Override
  public final boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectDescriptor that = (ProjectDescriptor) o;
    return key.equals(that.key);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(key);
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + "{" +
      "uuid='" + uuid + '\'' +
      ", key='" + key + '\'' +
      ", name='" + name + '\'' +
      '}';
  }
}
