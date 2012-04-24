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
package org.sonar.plugins.findbugs;

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.Extension;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;

import java.util.List;

@Properties({
  @Property(
    key = CoreProperties.FINDBUGS_EFFORT_PROPERTY,
    defaultValue = CoreProperties.FINDBUGS_EFFORT_DEFAULT_VALUE,
    name = "Effort",
    description = "Effort of the bug finders. Valid values are Min, Default and Max. Setting 'Max' increases precision but also increases " +
      "memory consumption.",
    project = true,
    module = true,
    global = true),
  @Property(
    key = CoreProperties.FINDBUGS_TIMEOUT_PROPERTY,
    defaultValue = CoreProperties.FINDBUGS_TIMEOUT_DEFAULT_VALUE + "",
    name = "Timeout",
    description = "Specifies the amount of time, in milliseconds, that FindBugs may run before it is assumed to be hung and is terminated. " +
      "The default is 600,000 milliseconds, which is ten minutes.",
    project = true,
    module = true,
    global = true,
    type = PropertyType.INTEGER),
  @Property(
    key = FindbugsConstants.EXCLUDES_FILTERS_PROPERTY,
    name = "Excludes Filters",
    description = "Paths to findbugs filter-files with exclusions (comma-separated).",
    project = true, module = true, global = true)})
public class FindbugsPlugin extends SonarPlugin {

  public List<Class<? extends Extension>> getExtensions() {
    return ImmutableList.of(
        FindbugsSensor.class,
        FindbugsConfiguration.class,
        FindbugsExecutor.class,
        FindbugsRuleRepository.class,
        FindbugsProfileExporter.class,
        FindbugsProfileImporter.class,
        SonarWayWithFindbugsProfile.class,
        FindbugsMavenInitializer.class);
  }
}
