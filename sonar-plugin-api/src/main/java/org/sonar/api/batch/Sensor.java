/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.batch;

import org.sonar.api.BatchExtension;
import org.sonar.api.resources.Project;

/**
 * <p>A Sensor is invoked once during the analysis of a project. The sensor can invoke a maven plugin,
 * parse a flat file, connect to a web server... For example the Cobertura Sensor invokes the Codehaus Cobertura MOJO.
 * Then the generated XML file is parsed and used to save the first-level of measures on resources
 * (project, package or class).</p>
 *
 * <p>Sensors are executed first during project analysis. Sensor are generally used to add measure at the
 * lowest level of the resource tree. A sensor can access and save measures on the whole tree of resources.</p>
 *
 * <p>A particular attention should be given to resource exclusion. Sonar already manages exclusions at file level : if
 * you try to save a measure on a resource that is excluded in the settings, then Sonar will not save the measure.
 * When handling a plugin or an external tool, you should make sure that exclusions are passed if you are going to get
 * back consolidated data.</p>
 *
 * @since 1.10
 */
public interface Sensor extends BatchExtension, CheckProject {

  /**
   * Sensors that depend upon Squid must declare the following method :
   * <code>
   *
   * @DependsUpon public String dependsUponSquidAnalysis() {
   * return Sensor.FLAG_SQUID_ANALYSIS;
   * }
   * </code>
   * }
   */
  String FLAG_SQUID_ANALYSIS = "squid";

  /**
   * The method that is going to be run when the sensor is called
   *
   * @param project the project the sensor runs on
   * @param context the context
   */
  void analyse(Project project, SensorContext context);

}
