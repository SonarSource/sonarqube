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
package org.sonar.server.webhook;

import java.util.Objects;
import javax.annotation.concurrent.Immutable;

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

  public String getUuid() {
    return uuid;
  }

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
    return Objects.equals(uuid, project.uuid) &&
      Objects.equals(key, project.key) &&
      Objects.equals(name, project.name);
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
    sb.append('}');
    return sb.toString();
  }

}
