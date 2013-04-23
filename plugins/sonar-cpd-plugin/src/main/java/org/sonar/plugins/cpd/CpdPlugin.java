/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.cpd;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.plugins.cpd.decorators.DuplicationDensityDecorator;
import org.sonar.plugins.cpd.decorators.SumDuplicationsDecorator;
import org.sonar.plugins.cpd.index.IndexFactory;

import java.util.List;

public final class CpdPlugin extends SonarPlugin {

  public List getExtensions() {
    return ImmutableList.of(
        PropertyDefinition.builder(CoreProperties.CPD_CROSS_RPOJECT)
            .defaultValue(CoreProperties.CPD_CROSS_RPOJECT_DEFAULT_VALUE + "")
            .name("Cross project duplication detection")
            .description("SonarQube supports the detection of cross project duplications. Activating this property will slightly increase each Sonar analysis time.")
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .category(CoreProperties.CATEGORY_DUPLICATIONS)
            .type(PropertyType.BOOLEAN)
            .build(),
        PropertyDefinition.builder(CoreProperties.CPD_SKIP_PROPERTY)
            .defaultValue("false")
            .name("Skip")
            .description("Disable detection of duplications")
            .hidden()
            .category(CoreProperties.CATEGORY_DUPLICATIONS)
            .type(PropertyType.BOOLEAN)
            .build(),
        PropertyDefinition.builder(CoreProperties.CPD_EXCLUSIONS)
            .defaultValue("")
            .name("Duplication exclusions")
            .description("Patterns used to exclude some source files from the duplication detection mechanism. " +
                "See the \"Exclusions\" category to know how to use wildcards to specify this property.")
            .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
            .category(CoreProperties.CATEGORY_DUPLICATIONS)
            .multiValues(true)
            .build(),

        CpdSensor.class,
        SumDuplicationsDecorator.class,
        DuplicationDensityDecorator.class,
        IndexFactory.class,
        SonarEngine.class,
        SonarBridgeEngine.class);
  }

}
