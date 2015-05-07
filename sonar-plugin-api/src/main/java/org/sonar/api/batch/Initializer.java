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
package org.sonar.api.batch;

import org.sonar.api.BatchSide;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.resources.Project;

/**
 * <p>
 * Initializer can execute external tool (like a Maven plugin), change project configuration. For example CoberturaMavenInitializer invokes
 * the Codehaus Cobertura Mojo and sets path to Cobertura report according to Maven POM.
 * </p>
 * 
 * <p>
 * Initializers are executed first and once during project analysis.
 * </p>
 * 
 * @since 2.6
 */
@BatchSide
@ExtensionPoint
public abstract class Initializer implements CheckProject {

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public abstract void execute(Project project);

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
