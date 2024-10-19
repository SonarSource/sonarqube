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
package org.sonar.server.project;

import java.util.Set;

public interface ProjectLifeCycleListeners {
  /**
   * This method is called after the specified projects have been deleted and will call method
   * {@link ProjectLifeCycleListener#onProjectsDeleted(Set) onProjectsDeleted(Set)} of all known
   * {@link ProjectLifeCycleListener} implementations.
   * <p>
   * This method ensures all {@link ProjectLifeCycleListener} implementations are called, even if one or more of
   * them fail with an exception.
   */
  void onProjectsDeleted(Set<DeletedProject> projects);

  /**
   * This method is called after the specified project have any king of change (branch deleted, change of main branch, ...)
   *  This method will call method {@link ProjectLifeCycleListener#onProjectBranchesChanged(Set,Set)} of all known
   * {@link ProjectLifeCycleListener} implementations.
   * <p>
   * This method ensures all {@link ProjectLifeCycleListener} implementations are called, even if one or more of
   * them fail with an exception.
   */
  void onProjectBranchesChanged(Set<Project> projects, Set<String> impactedBranches);

  /**
   * This method is called after the specified project's key has been changed and will call method
   * {@link ProjectLifeCycleListener#onProjectsRekeyed(Set) onProjectsRekeyed(Set)} of all known
   * {@link ProjectLifeCycleListener} implementations.
   * <p>
   * This method ensures all {@link ProjectLifeCycleListener} implementations are called, even if one or more of
   * them fail with an exception.
   */
  void onProjectsRekeyed(Set<RekeyedProject> rekeyedProjects);

}
