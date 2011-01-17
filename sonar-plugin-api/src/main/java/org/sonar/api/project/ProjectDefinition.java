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
package org.sonar.api.project;

import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.util.List;

/**
 * Defines project in a form suitable for Sonar.
 * This is a part of bootstrap process, so we should take care about backward compatibility.
 * <p>
 * We assume that project is just a set of configuration properties and directories. And each project has unique key in format
 * "groupId:artifactId" (for example "org.codehaus.sonar:sonar").
 * </p>
 * 
 * @since 2.6
 */
public interface ProjectDefinition {

  /**
   * @return project key.
   */
  String getKey();

  /**
   * @return project properties.
   */
  Configuration getConfiguration();

  /**
   * @return Sonar working directory.
   *         It's "${project.build.directory}/sonar" ("${project.basedir}/target/sonar") for Maven projects.
   */
  File getSonarWorkingDirectory();

  /**
   * @return project root directory.
   *         It's "${project.basedir}" for Maven projects.
   */
  File getBasedir();

  /**
   * @return project directories.
   */
  List<ProjectDirectory> getDirs();

  /**
   * @return parent project.
   */
  ProjectDefinition getParent();

  /**
   * @return list of sub-projects.
   */
  List<ProjectDefinition> getModules();

}
