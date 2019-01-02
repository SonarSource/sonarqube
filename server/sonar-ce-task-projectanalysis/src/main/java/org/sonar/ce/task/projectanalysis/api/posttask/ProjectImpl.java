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
package org.sonar.ce.task.projectanalysis.api.posttask;

import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.posttask.Project;

import static java.util.Objects.requireNonNull;

@Immutable
class ProjectImpl implements Project {
  private final String uuid;
  private final String key;
  private final String name;

  ProjectImpl(String uuid, String key, String name) {
    this.uuid = requireNonNull(uuid, "uuid can not be null");
    this.key = requireNonNull(key, "key can not be null");
    this.name = requireNonNull(name, "name can not be null");
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "ProjectImpl{" +
      "uuid='" + uuid + '\'' +
      ", key='" + key + '\'' +
      ", name='" + name + '\'' +
      '}';
  }
}
