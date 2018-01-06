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
package org.sonar.server.computation.task.projectanalysis.analysis;

import javax.annotation.concurrent.Immutable;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.task.projectanalysis.component.Component;

@Immutable
public class Project {

  private final String uuid;
  private final String key;
  private final String name;

  public Project(String uuid, String key, String name) {
    this.uuid = uuid;
    this.key = key;
    this.name = name;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Project project = (Project) o;
    return uuid.equals(project.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Project{");
    sb.append("uuid='").append(uuid).append('\'');
    sb.append(", key='").append(key).append('\'');
    sb.append(", name='").append(name).append('\'');
    sb.append('}');
    return sb.toString();
  }

  public static Project copyOf(Component component) {
    return new Project(component.getUuid(), component.getKey(), component.getName());
  }

  public static Project copyOf(ComponentDto component) {
    return new Project(component.uuid(), component.getDbKey(), component.name());
  }
}
