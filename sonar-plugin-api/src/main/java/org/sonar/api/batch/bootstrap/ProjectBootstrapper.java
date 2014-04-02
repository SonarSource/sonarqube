/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.batch.bootstrap;

import org.sonar.api.task.TaskExtension;

/**
 * This extension point initializes the project structure. It is extended by batch bootstrappers
 * like sonar-runner or Maven plugin. It is not supposed to be used by standard plugins (.NET, ...).
 * Some use-cases :
 * <ul>
 *   <li>Maven Plugins defines project structure from pom.xml</li>
 *   <li>Sonar Runner defines project from sonar-runner.properties</li>
 * </ul>
 * Only one instance is allowed per environment.
 *
 * @since 3.7
 * @deprecated since 4.3 All bootstrappers should use SQ Runner API and provide a set of properties
 */
@Deprecated
public interface ProjectBootstrapper extends TaskExtension {

  /**
   * Implement this method to create project reactor
   */
  ProjectReactor bootstrap();

}
