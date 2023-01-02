/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.sensor;

import java.util.Objects;
import javax.annotation.Nullable;

import static org.sonar.api.utils.Preconditions.checkNotNull;

public class SensorId {
  private final String sensorName;
  @Nullable
  private final String pluginKey;

  public SensorId(@Nullable String pluginKey, String sensorName) {
    checkNotNull(sensorName);
    this.pluginKey = pluginKey;
    this.sensorName = sensorName;
  }

  @Nullable
  public String getPluginKey() {
    return pluginKey;
  }

  public String getSensorName() {
    return sensorName;
  }

  @Override
  public String toString() {
    if (pluginKey == null) {
      return sensorName;
    } else {
      return sensorName + " [" + pluginKey + "]";
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SensorId sensorId = (SensorId) o;
    return Objects.equals(sensorName, sensorId.sensorName) && Objects.equals(pluginKey, sensorId.pluginKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sensorName, pluginKey);
  }
}
