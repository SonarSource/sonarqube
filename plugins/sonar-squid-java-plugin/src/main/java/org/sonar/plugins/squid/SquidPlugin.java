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
package org.sonar.plugins.squid;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.Extension;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.squid.decorators.ChidamberKemererDistributionBuilder;
import org.sonar.plugins.squid.decorators.ClassesDecorator;
import org.sonar.plugins.squid.decorators.FileComplexityDistributionDecorator;
import org.sonar.plugins.squid.decorators.FunctionComplexityDistributionBuilder;
import org.sonar.plugins.squid.decorators.FunctionsDecorator;

import java.util.List;

@Properties({
  @Property(key = SquidPluginProperties.SQUID_ANALYSE_ACCESSORS_PROPERTY,
    defaultValue = SquidPluginProperties.SQUID_ANALYSE_ACCESSORS_DEFAULT_VALUE + "",
    name = "Separate accessors",
    description = "Flag whether Squid should separate accessors (getters/setters) from methods. " +
      "In that case, accessors are not counted in metrics such as complexity or API documentation.",
    project = true,
    global = true,
    category = CoreProperties.CATEGORY_JAVA,
    type = PropertyType.BOOLEAN),
  @Property(key = SquidPluginProperties.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION,
    defaultValue = SquidPluginProperties.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION_DEFAULT_VALUE,
    name = "List of fields to exclude from LCOM4 computation",
    description = "Some fields should not be taken into account when computing LCOM4 measure as they " +
      "unexpectedly and artificially decrease the LCOM4 measure. "
      + "The best example is a logger used by all methods of a class. " +
      "All field names to exclude from LCOM4 computation must be separated by a comma.",
    project = true,
    global = true,
    category = CoreProperties.CATEGORY_JAVA),
  @Property(
    key = CoreProperties.DESIGN_SKIP_DESIGN_PROPERTY,
    defaultValue = "" + CoreProperties.DESIGN_SKIP_DESIGN_DEFAULT_VALUE,
    name = "Skip design analysis",
    project = true,
    global = true,
    category = CoreProperties.CATEGORY_JAVA,
    type = PropertyType.BOOLEAN)
})
public final class SquidPlugin extends SonarPlugin {

  public List<Class<? extends Extension>> getExtensions() {
    return ImmutableList.of(
        ChidamberKemererDistributionBuilder.class,
        ClassesDecorator.class,
        FileComplexityDistributionDecorator.class,
        FunctionComplexityDistributionBuilder.class,
        FunctionsDecorator.class,
        JavaSourceImporter.class,
        SonarWayProfile.class,
        SonarWayWithFindbugsProfile.class,
        SquidRuleRepository.class,
        SquidSensor.class);
  }
}
