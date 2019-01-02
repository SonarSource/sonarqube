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
package org.sonar.server.platform.db.migration.version.v74;

import java.util.Objects;
import javax.annotation.Nullable;

final class Row {
  final String taskUuid;
  final String componentUuid;
  final String tmpComponentUuid;
  final String tmpMainComponentUuid;

  Row(String taskUuid, @Nullable String componentUuid, @Nullable String tmpComponentUuid, @Nullable String tmpMainComponentUuid) {
    this.taskUuid = taskUuid;
    this.componentUuid = componentUuid;
    this.tmpComponentUuid = tmpComponentUuid;
    this.tmpMainComponentUuid = tmpMainComponentUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Row row = (Row) o;
    return Objects.equals(taskUuid, row.taskUuid) &&
      Objects.equals(componentUuid, row.componentUuid) &&
      Objects.equals(tmpComponentUuid, row.tmpComponentUuid) &&
      Objects.equals(tmpMainComponentUuid, row.tmpMainComponentUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskUuid, componentUuid, tmpComponentUuid, tmpMainComponentUuid);
  }

  @Override
  public String toString() {
    return "Row{" +
      "uuid='" + taskUuid + '\'' +
      ", componentUuid='" + componentUuid + '\'' +
      ", tmpComponentUuid='" + tmpComponentUuid + '\'' +
      ", tmpMainComponentUuid='" + tmpMainComponentUuid + '\'' +
      '}';
  }
}
