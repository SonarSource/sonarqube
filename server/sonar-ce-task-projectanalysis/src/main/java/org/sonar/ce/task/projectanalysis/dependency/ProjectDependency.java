/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.dependency;

import javax.annotation.CheckForNull;

public interface ProjectDependency {

  /**
   * Returns the dependency uuid.
   */
  String getUuid();

  /**
   * Returns the dependency key.
   */
  String getKey();

  /**
   * The dependency fully qualified name, without version. For Maven this is the groupId:artifactId.
   */
  String getFullName();

  /**
   * The dependency name. For Maven this is only the artifactId.
   */
  String getName();

  /**
   * The optional description of the dependency.
   */
  @CheckForNull
  String getDescription();

  /**
   * The optional version of the dependency.
   */
  @CheckForNull
  String getVersion();

  /**
   * The optional package manager of the dependency (e.g. mvn, npm, nuget, ...).
   */
  @CheckForNull
  String getPackageManager();
}
