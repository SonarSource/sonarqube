/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.cluster;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

public class ClusterProperties {

  public static final String ENABLED = "sonar.cluster.enabled";
  public static final String STARTUP_LEADER = "sonar.cluster.web.startupLeader";

  private ClusterProperties() {
    // only statics
  }

  public static List<PropertyDefinition> definitions() {
    return ImmutableList.of(
      PropertyDefinition.builder(ENABLED)
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .hidden()
        .build(),

      PropertyDefinition.builder(STARTUP_LEADER)
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .hidden()
        .build());
  }
}
