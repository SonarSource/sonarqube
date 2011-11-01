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
package org.sonar.plugins.jacoco;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.jacoco.itcoverage.*;

import java.util.Arrays;
import java.util.List;

@Properties({
    @Property(
        key = JacocoConfiguration.REPORT_PATH_PROPERTY,
        name = "File with execution data",
        defaultValue = JacocoConfiguration.REPORT_PATH_DEFAULT_VALUE,
        description = "Path (absolute or relative) to the file with execution data.",
        global = false,
        module = true,
        project = true
    ),
    @Property(
        key = JacocoConfiguration.INCLUDES_PROPERTY,
        name = "Includes",
        description = "A list of class names that should be included in execution analysis." +
            " The list entries are separated by a colon (:) and may use wildcard characters (* and ?)." +
            " Except for performance optimization or technical corner cases this option is normally not required.",
        global = true,
        project = true,
        module = true
    ),
    @Property(
        key = JacocoConfiguration.EXCLUDES_PROPERTY,
        name = "Excludes",
        description = "A list of class names that should be excluded from execution analysis." +
            " The list entries are separated by a colon (:) and may use wildcard characters (* and ?)." +
            " Except for performance optimization or technical corner cases this option is normally not required.",
        global = true,
        project = true,
        module = true
    ),
    @Property(
        key = JacocoConfiguration.EXCLCLASSLOADER_PROPERTY,
        name = "Excluded class loaders",
        description = "A list of class loader names that should be excluded from execution analysis." +
            " The list entries are separated by a colon (:) and may use wildcard characters (* and ?)." +
            " This option might be required in case of special frameworks that conflict with JaCoCo code" +
            " instrumentation, in particular class loaders that do not have access to the Java runtime classes.",
        global = true,
        project = true,
        module = true
    ),
    @Property(
        key = JacocoConfiguration.IT_REPORT_PATH_PROPERTY,
        name = "File with execution data for integration tests",
        defaultValue = JacocoConfiguration.IT_REPORT_PATH_DEFAULT_VALUE,
        description = "Path (absolute or relative) to the file with execution data.",
        global = false,
        module = true,
        project = true
    ),
    @Property(
        key = JacocoConfiguration.ANT_TARGETS_PROPERTY,
        name = "Ant targets",
        defaultValue = JacocoConfiguration.ANT_TARGETS_DEFAULT_VALUE,
        description = "Comma separated list of Ant targets for execution of tests.",
        global = true,
        module = true,
        project = true
    ) })
public class JaCoCoPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(
        JacocoConfiguration.class,
        JaCoCoAgentDownloader.class,
        // Ant
        JacocoAntInitializer.class,
        // Maven
        JacocoMavenInitializer.class,
        JaCoCoMavenPluginHandler.class,
        // Unit tests
        JaCoCoSensor.class,

        // Integration tests
        JaCoCoItSensor.class,
        ItCoverageWidget.class);
  }
}
