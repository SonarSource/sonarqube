/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrapper;

import org.sonar.api.batch.bootstrap.ProjectReactor;

import java.util.List;

/**
 * Describes order of projects.
 * Used by sonar-runner 1.x. Gradle 1.0 already uses {@link org.sonar.api.batch.bootstrap.ProjectReactor}.
 * 
 * @since 2.6
 * @deprecated replaced by {@link org.sonar.api.batch.bootstrap.ProjectReactor} in version 2.9
 */
@Deprecated
public class Reactor {

  private ProjectDefinition root;

  public Reactor(ProjectDefinition root) {
    this.root = root;
  }

  public Reactor(List<ProjectDefinition> sortedProjects) {//NOSONAR unused parameter is kept for backward-compatibility of API
    throw new IllegalArgumentException("This constructor is not supported anymore");
  }

  public List<ProjectDefinition> getSortedProjects() {
    throw new IllegalArgumentException("The method getSortedProjects() is not supported anymore");
  }

  public ProjectReactor toProjectReactor() {
    return new ProjectReactor(root.toNewProjectDefinition());
  }

}
