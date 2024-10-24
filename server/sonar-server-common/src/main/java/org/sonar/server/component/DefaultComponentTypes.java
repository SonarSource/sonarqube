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
package org.sonar.server.component;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.component.ComponentQualifiers;

@ServerSide
@ComputeEngineSide
public final class DefaultComponentTypes {

  private static final String CONFIGURABLE = "configurable";
  private static final String UPDATABLE_KEY = "updatable_key";
  public static final String IGNORED = "ignored";

  private DefaultComponentTypes() {
    // only static methods
  }

  public static ComponentTypeTree get() {
    return ComponentTypeTree.builder()
      .addType(ComponentType.builder(ComponentQualifiers.PROJECT)
        .setProperty("deletable", true)
        .setProperty("modifiable_history", true)
        .setProperty("hasRolePolicy", true)
        .setProperty(UPDATABLE_KEY, true)
        .setProperty("comparable", true)
        .setProperty(CONFIGURABLE, true)
        .build())
      .addType(ComponentType.builder(ComponentQualifiers.MODULE)
        .setProperty(UPDATABLE_KEY, true)
        .setProperty(CONFIGURABLE, true)
        .setProperty(IGNORED, true)
        .build())
      .addType(ComponentType.builder(ComponentQualifiers.DIRECTORY)
        .build())
      .addType(ComponentType.builder(ComponentQualifiers.FILE)
        .hasSourceCode()
        .build())
      .addType(ComponentType.builder(ComponentQualifiers.UNIT_TEST_FILE)
        .hasSourceCode()
        .build())

      .addRelations(ComponentQualifiers.PROJECT, ComponentQualifiers.DIRECTORY)
      .addRelations(ComponentQualifiers.DIRECTORY, ComponentQualifiers.FILE, ComponentQualifiers.UNIT_TEST_FILE)

      .build();
  }
}
