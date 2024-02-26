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
package org.sonar.scm.git;

import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GitScmConfiguration {

    public static final String INCLUDE_SCM_SUBMODULES = "sonar.scm.submodules.included";

    private static final List<PropertyDefinition> PROPERTY_DEFINITIONS = new ArrayList<>();

    static {
        PROPERTY_DEFINITIONS.add(PropertyDefinition.builder(INCLUDE_SCM_SUBMODULES)
                .name("Include submodules in SCM Sensor")
                .description("Enable the retrieval of blame information for submodules from Source Control Manager")
                .category(CoreProperties.CATEGORY_SCM)
                .subCategory("Git")
                .type(PropertyType.BOOLEAN)
                .onQualifiers(Qualifiers.PROJECT, Qualifiers.APP)
                .defaultValue("false")
                .build());
    }

    private GitScmConfiguration() {
        throw new IllegalStateException("Utility class");
    }

    public static List<PropertyDefinition> getConfiguration() {
        return PROPERTY_DEFINITIONS;
    }

    public static boolean subModuleAnalysisEnabled(Configuration configuration) {

        Optional<String> parameter = configuration.get(GitScmConfiguration.INCLUDE_SCM_SUBMODULES);
        return parameter.filter(Boolean::parseBoolean).isPresent();
    }
}
