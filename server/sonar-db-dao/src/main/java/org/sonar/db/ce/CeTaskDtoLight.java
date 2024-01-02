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
package org.sonar.db.ce;

import java.util.Comparator;
import java.util.Objects;

public class CeTaskDtoLight implements Comparable<CeTaskDtoLight> {

  private String ceTaskUuid;
  private long createdAt;

  public void setCeTaskUuid(String ceTaskUuid) {
    this.ceTaskUuid = ceTaskUuid;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public String getCeTaskUuid() {
    return ceTaskUuid;
  }

  @Override
  public int compareTo(CeTaskDtoLight o) {
    return Comparator.comparingLong(CeTaskDtoLight::getCreatedAt).thenComparing(CeTaskDtoLight::getCeTaskUuid).compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CeTaskDtoLight that = (CeTaskDtoLight) o;
    return createdAt == that.createdAt && Objects.equals(ceTaskUuid, that.ceTaskUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ceTaskUuid, createdAt);
  }
}
