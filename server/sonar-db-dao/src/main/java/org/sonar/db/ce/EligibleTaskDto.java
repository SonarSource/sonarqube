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
package org.sonar.db.ce;

public class EligibleTaskDto {
  private String uuid;
  private int executionCount;

  public String getUuid() {
    return uuid;
  }

  public EligibleTaskDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public int getExecutionCount() {
    return executionCount;
  }

  public EligibleTaskDto setExecutionCount(int executionCount) {
    this.executionCount = executionCount;
    return this;
  }

  @Override
  public String toString() {
    return "EligibleTaskDto{" +
        "uuid='" + uuid + '\'' +
        ", executionCount=" + executionCount +
        '}';
  }
}
