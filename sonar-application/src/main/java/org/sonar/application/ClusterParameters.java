/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.application;

import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;

enum ClusterParameters {
  ENABLED("sonar.cluster.enabled", Boolean.FALSE.toString()),
  MEMBERS("sonar.cluster.members", ""),
  PORT("sonar.cluster.port", Integer.toString(9003)),
  PORT_AUTOINCREMENT("sonar.cluster.port_autoincrement", Boolean.FALSE.toString()),
  INTERFACES("sonar.cluster.interfaces", ""),
  NAME("sonar.cluster.name", ""),
  HAZELCAST_LOG_LEVEL("sonar.log.level.app.hazelcast", "WARN");

  private final String name;
  private final String defaultValue;

  ClusterParameters(@Nonnull String name, @Nonnull String defaultValue) {
    this.name = name;
    this.defaultValue = defaultValue;
  }

  String getName() {
    return name;
  }

  String getDefaultValue() {
    return defaultValue;
  }

  boolean getDefaultValueAsBoolean() {
    return "true".equalsIgnoreCase(defaultValue);
  }

  Integer getDefaultValueAsInt() {
    if (StringUtils.isNotEmpty(defaultValue)) {
      try {
        return Integer.parseInt(defaultValue);
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Default value of property " + name + " is not an integer: " + defaultValue, e);
      }
    }
    return null;
  }
}
