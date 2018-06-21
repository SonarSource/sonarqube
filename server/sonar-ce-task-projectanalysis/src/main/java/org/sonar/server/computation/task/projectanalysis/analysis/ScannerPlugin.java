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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class ScannerPlugin {
  private final String key;
  private final String basePluginKey;
  private final long updatedAt;

  public ScannerPlugin(String key, @Nullable String basePluginKey, long updatedAt) {
    this.key = requireNonNull(key, "key can't be null");
    this.basePluginKey = basePluginKey;
    this.updatedAt = updatedAt;
  }

  public String getKey() {
    return key;
  }

  @CheckForNull
  public String getBasePluginKey() {
    return basePluginKey;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScannerPlugin that = (ScannerPlugin) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return "ScannerPlugin{" +
      "key='" + key + '\'' +
      ", basePluginKey='" + basePluginKey + '\'' +
      ", updatedAt='" + updatedAt + '\'' +
      '}';
  }

}
