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
package org.sonar.server.project;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;

import static java.util.Collections.emptyList;

@Immutable
public class Project {

  private final String uuid;
  private final String key;
  private final String name;
  private final String description;
  private final List<String> tags;

  public Project(String uuid, String key, String name, @Nullable String description, List<String> tags) {
    this.uuid = uuid;
    this.key = key;
    this.name = name;
    this.description = description;
    this.tags = tags;
  }

  /**
   * Should use {@link org.sonar.server.project.Project#fromProjectDtoWithTags(org.sonar.db.project.ProjectDto)} instead
   */
  @Deprecated(since = "10.2")
  public static Project from(ComponentDto project) {
    return new Project(project.uuid(), project.getKey(), project.name(), project.description(), emptyList());
  }

  public static Project fromProjectDtoWithTags(ProjectDto project) {
    return new Project(project.getUuid(), project.getKey(), project.getName(), project.getDescription(), project.getTags());
  }

  public static Project from(EntityDto entityDto) {
    return new Project(entityDto.getUuid(), entityDto.getKey(), entityDto.getName(), entityDto.getDescription(), emptyList());
  }

  /**
   * Always links to a row that exists in database.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Always links to a row that exists in database.
   */
  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getTags() {
    return tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Project project = (Project) o;
    return uuid.equals(project.uuid)
      && key.equals(project.key)
      && name.equals(project.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, key, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Project{");
    sb.append("uuid='").append(uuid).append('\'');
    sb.append(", key='").append(key).append('\'');
    sb.append(", name='").append(name).append('\'');
    sb.append(", description=").append(toString(this.description));
    sb.append('}');
    return sb.toString();
  }

  private static String toString(@Nullable String s) {
    if (s == null) {
      return null;
    }
    return '\'' + s + '\'';
  }

}
