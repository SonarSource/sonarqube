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
package org.sonar.api.batch.bootstrap;

import org.sonar.api.task.TaskExtension;

/**
 * This extension point allows to define project structure at runtime. It is executed once during task startup.
 * Some use-cases :
 * <ul>
 *   <li>Maven bootstraper create project structure from pom.xml</li>
 *   <li>Sonar Runner bootstraper create project structure from sonar-runner.properties</li>
 * </ul>
 * Only one ProjectBootstrapper is allowed per environement.
 *
 * @since 3.7
 */
public abstract class ProjectBootstrapper implements TaskExtension {

  protected ProjectBootstrapper() {
  }

  /**
   * Implement this method to create project reactor
   */
  public abstract ProjectReactor bootstrap();

}
