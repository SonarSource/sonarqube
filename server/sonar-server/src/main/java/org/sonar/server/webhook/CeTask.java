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
package org.sonar.server.webhook;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class CeTask {
  private final String id;
  private final Status status;

  public CeTask(String id, Status status) {
    this.id = requireNonNull(id, "id can't be null");
    this.status = requireNonNull(status, "status can't be null");
  }

  public String getId() {
    return id;
  }

  public Status getStatus() {
    return status;
  }

  public enum Status {
    SUCCESS, FAILED
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CeTask task = (CeTask) o;
    return Objects.equals(id, task.id) &&
      status == task.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status);
  }

  @Override
  public String toString() {
    return "CeTask{" +
      "id='" + id + '\'' +
      ", status=" + status +
      '}';
  }
}
