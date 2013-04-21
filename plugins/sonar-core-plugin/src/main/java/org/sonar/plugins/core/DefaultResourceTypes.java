/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core;

import org.sonar.api.BatchExtension;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.ServerExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public final class DefaultResourceTypes extends ExtensionProvider implements BatchExtension, ServerExtension {
  @Override
  public ResourceTypeTree provide() {
    return ResourceTypeTree.builder()
        .addType(ResourceType.builder(Qualifiers.PROJECT)
            .setProperty("deletable", true)
            .setProperty("supportsGlobalDashboards", true)
            .setProperty("modifiable_history", true)
            .setProperty("hasRolePolicy", true)
            .setProperty("updatable_key", true)
            .setProperty("supportsMeasureFilters", true)
            .setProperty("comparable", true)
            .setProperty("configurable", true)
            .build())
        .addType(ResourceType.builder(Qualifiers.MODULE)
            .setProperty("updatable_key", true)
            .setProperty("supportsMeasureFilters", true)
            .setProperty("configurable", true)
            .build())
        .addType(ResourceType.builder(Qualifiers.DIRECTORY)
            .setProperty("supportsMeasureFilters", true)
            .build())
        .addType(ResourceType.builder(Qualifiers.PACKAGE)
            .build())
        .addType(ResourceType.builder(Qualifiers.FILE)
            .hasSourceCode()
            .setProperty("supportsMeasureFilters", true)
            .build())
        .addType(ResourceType.builder(Qualifiers.CLASS)
            .hasSourceCode()
            .build())
        .addType(ResourceType.builder(Qualifiers.UNIT_TEST_FILE)
            .hasSourceCode()
            .setProperty("supportsMeasureFilters", true)
            .build())

        .addRelations(Qualifiers.PROJECT, Qualifiers.MODULE)
        .addRelations(Qualifiers.MODULE, Qualifiers.DIRECTORY, Qualifiers.PACKAGE)
        .addRelations(Qualifiers.DIRECTORY, Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE)
        .addRelations(Qualifiers.PACKAGE, Qualifiers.CLASS, Qualifiers.UNIT_TEST_FILE)

        .build();
  }
}
