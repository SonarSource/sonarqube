/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.plugins.squid.decorators.*;

import java.util.Arrays;
import java.util.List;

@Properties({
    @Property(key = SquidPluginProperties.SQUID_ANALYSE_ACCESSORS_PROPERTY,
        defaultValue = SquidPluginProperties.SQUID_ANALYSE_ACCESSORS_DEFAULT_VALUE
            + "",
        name = "Separate accessors",
        description = "Flag whether Squid should separate accessors (getters/setters) from methods. " +
            "In that case, accessors are not counted in metrics such as complexity or API documentation.",
        project = true, global = true),
    @Property(key = SquidPluginProperties.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION,
        defaultValue = SquidPluginProperties.FIELDS_TO_EXCLUDE_FROM_LCOM4_COMPUTATION_DEFAULT_VALUE,
        name = "List of fields to exclude from LCOM4 computation",
        description = "Some fields should not be taken into account when computing LCOM4 measure as they " +
            "unexpectedly and artificially decrease the LCOM4 measure. "
            + "The best example is a logger used by all methods of a class. " +
            "All field names to exclude from LCOM4 computation must be separated by a comma.",
        project = true, global = true)})
public class SquidPlugin implements Plugin {

  public String getKey() {
    return CoreProperties.SQUID_PLUGIN;
  }

  public String getName() {
    return "Squid";
  }

  public String getDescription() {
    return "Squid collects standard metrics on source code, such as lines of code, cyclomatic complexity, documentation level...";
  }

  public List getExtensions() {
    return Arrays.asList(SquidSearchProxy.class, SquidSensor.class, SquidRuleRepository.class, JavaSourceImporter.class,
        ClassComplexityDistributionBuilder.class, FunctionComplexityDistributionBuilder.class, ClassesDecorator.class,
        ChidamberKemererDistributionBuilder.class, FunctionsDecorator.class);
  }

  @Override
  public String toString() {
    return getKey();
  }
}
