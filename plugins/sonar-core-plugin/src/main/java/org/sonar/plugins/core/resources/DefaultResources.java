/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.resources;

import org.sonar.api.ExtensionProvider;
import org.sonar.api.ServerExtension;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceDefinition;

import java.util.Arrays;
import java.util.List;

public class DefaultResources extends ExtensionProvider implements ServerExtension {

  @Override
  public List<ResourceDefinition> provide() {
    return Arrays.asList(
        ResourceDefinition.builder(Qualifiers.VIEW).setName("view").build(),
        ResourceDefinition.builder(Qualifiers.SUBVIEW).setName("sub_view").build(),
        ResourceDefinition.builder(Qualifiers.PROJECT).setName("project").build(),
        ResourceDefinition.builder(Qualifiers.MODULE).setName("sub_project").build(),
        ResourceDefinition.builder(Qualifiers.DIRECTORY).setName("directory").build(),
        ResourceDefinition.builder(Qualifiers.PACKAGE).setName("package").build(),
        ResourceDefinition.builder(Qualifiers.FILE).setName("file").build(),
        ResourceDefinition.builder(Qualifiers.CLASS).setName("class").build(),
        ResourceDefinition.builder(Qualifiers.UNIT_TEST_FILE).setName("unit_test").build(),
        ResourceDefinition.builder(Qualifiers.LIBRARY).setName("library").build());
  }

}
