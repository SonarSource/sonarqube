/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.debt;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class Characteristic {
  private final int id;
  private final String key;
  private final Integer parentId;

  public Characteristic(int id, String key, @Nullable Integer parentId) {
    requireNonNull(key, "key cannot be null");
    this.id = id;
    this.key = key;
    this.parentId = parentId;
  }

  public int getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  @CheckForNull
  public Integer getParentId() {
    return parentId;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Characteristic that = (Characteristic) o;
    return id == that.id && key.equals(that.key);
  }

  @Override
  public int hashCode() {
    int result = id;
    result = 31 * result + key.hashCode();
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Characteristic{");
    sb.append("id=").append(id);
    sb.append(", key='").append(key).append('\'');
    sb.append(", parentId=").append(parentId);
    sb.append('}');
    return sb.toString();
  }
}
