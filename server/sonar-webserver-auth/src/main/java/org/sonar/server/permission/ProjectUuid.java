/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.permission;

import javax.annotation.concurrent.Immutable;
import org.sonar.db.component.ComponentDto;

import static java.util.Objects.requireNonNull;

/**
 * Reference to a project by its db  uuid.
 *
 */
@Immutable
public class ProjectUuid {

  private final String uuid;
  private final boolean isPrivate;

  public ProjectUuid(ComponentDto project) {
    this.uuid = requireNonNull(project.uuid());
    this.isPrivate = project.isPrivate();
  }

  public String getUuid() {
    return uuid;
  }

  public boolean isPrivate() {
    return isPrivate;
  }
}
