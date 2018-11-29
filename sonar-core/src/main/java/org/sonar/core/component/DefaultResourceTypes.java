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
package org.sonar.core.component;

import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.server.ServerSide;

@ScannerSide
@ServerSide
@ComputeEngineSide
public final class DefaultResourceTypes {

  private static final String CONFIGURABLE = "configurable";
  private static final String UPDATABLE_KEY = "updatable_key";

  private DefaultResourceTypes() {
    // only static methods
  }

  public static ResourceTypeTree get() {
    return ResourceTypeTree.builder()
      .addType(ResourceType.builder(Qualifiers.PROJECT)
        .setProperty("deletable", true)
        .setProperty("modifiable_history", true)
        .setProperty("hasRolePolicy", true)
        .setProperty(UPDATABLE_KEY, true)
        .setProperty("comparable", true)
        .setProperty(CONFIGURABLE, true)
        .build())
      .addType(ResourceType.builder(Qualifiers.MODULE)
        .setProperty(UPDATABLE_KEY, true)
        .setProperty(CONFIGURABLE, true)
        .build())
      .addType(ResourceType.builder(Qualifiers.DIRECTORY)
        .build())
      .addType(ResourceType.builder(Qualifiers.FILE)
        .hasSourceCode()
        .build())
      .addType(ResourceType.builder(Qualifiers.UNIT_TEST_FILE)
        .hasSourceCode()
        .build())

      .addRelations(Qualifiers.PROJECT, Qualifiers.MODULE)
      .addRelations(Qualifiers.MODULE, Qualifiers.DIRECTORY)
      .addRelations(Qualifiers.DIRECTORY, Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE)

      .build();
  }
}
