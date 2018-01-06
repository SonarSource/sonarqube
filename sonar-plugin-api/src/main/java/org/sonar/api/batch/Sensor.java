/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.batch;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.resources.Project;

/**
 * <p>
 * A Sensor is invoked once during the analysis of a project. The sensor can parse a flat file, connect to a web server... Sensor are
 * generally used to add measure at the lowest level of the resource tree. A sensor can access and save measures on the whole tree of
 * resources.
 * 
 *
 * <p>
 * For example the Cobertura Sensor parses Cobertura report and saves the first-level of measures on resources.
 * 
 *
 * <p>
 * A particular attention should be given to resource exclusion. Sonar already manages exclusions at file level : if you try to save a
 * measure on a resource that is excluded in the settings, then Sonar will not save the measure. When handling a plugin or an external tool,
 * you should make sure that exclusions are passed if you are going to get back consolidated data.
 * 
 *
 * @since 1.10
 * @deprecated since 5.6 use org.sonar.api.batch.sensor.Sensor
 */
@Deprecated
@ScannerSide
@ExtensionPoint
public interface Sensor extends CheckProject {

  /**
   * Sensors that depend upon Squid must declare the following method :
   *
   * <pre>
   * &#064;DependsUpon
   * public String dependsUponSquidAnalysis() {
   *   return Sensor.FLAG_SQUID_ANALYSIS;
   * }
   * </pre>
   */
  String FLAG_SQUID_ANALYSIS = "squid";

  /**
   * The method that is going to be run when the sensor is called
   *
   * @param module the module the sensor runs on
   * @param context the context
   */
  void analyse(Project module, SensorContext context);

}
