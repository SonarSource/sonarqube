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
package org.sonar.core.config;

import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import static java.util.Arrays.asList;
import static org.sonar.api.PropertyType.BOOLEAN;

public class ScannerProperties {

  public static final String BRANCH_NAME = "sonar.branch.name";
  public static final String BRANCH_TARGET = "sonar.branch.target";
  public static final String ORGANIZATION = "sonar.organization";

  private ScannerProperties() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    return asList(
      PropertyDefinition.builder(CoreProperties.ANALYSIS_MODE)
        .name("Analysis mode")
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options(asList(CoreProperties.ANALYSIS_MODE_ANALYSIS, CoreProperties.ANALYSIS_MODE_PREVIEW, CoreProperties.ANALYSIS_MODE_INCREMENTAL))
        .category(CoreProperties.CATEGORY_GENERAL)
        .defaultValue(CoreProperties.ANALYSIS_MODE_ANALYSIS)
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.SCM_DISABLED_KEY)
        .name("Disable the SCM Sensor")
        .description("Disable the retrieval of blame information from Source Control Manager")
        .category(CoreProperties.CATEGORY_SCM)
        .type(BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue(String.valueOf(false))
        .build(),
      PropertyDefinition.builder(CoreProperties.SCM_PROVIDER_KEY)
        .name("Key of the SCM provider for this project")
        .description("Force the provider to be used to get SCM information for this project. By default auto-detection is done. Example: svn, git.")
        .category(CoreProperties.CATEGORY_SCM)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinition.builder(ORGANIZATION)
        .name("Organization key")
        .description("Key of the organization that contains the project being analyzed. If unset, then the organization marked as default is used.")
        .hidden()
        .build(),
      PropertyDefinition.builder(BRANCH_NAME)
        .name("Optional name of SonarQube/SCM branch")
        .description("TODO")
        .hidden()
        .build(),
      PropertyDefinition.builder(BRANCH_TARGET)
        .name("Optional name of target branch to merge into, and the base to determine changed files")
        .description("TODO")
        .hidden()
        .build()
      );
  }
}
