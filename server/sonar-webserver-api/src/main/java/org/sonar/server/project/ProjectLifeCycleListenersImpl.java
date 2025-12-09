/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static com.google.common.base.Preconditions.checkNotNull;

public class ProjectLifeCycleListenersImpl implements ProjectLifeCycleListeners {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectLifeCycleListenersImpl.class);

  private final ProjectLifeCycleListener[] listeners;

  /**
   * Used by the container when there is no ProjectLifeCycleListener implementation in container.
   */
  @Autowired(required = false)
  public ProjectLifeCycleListenersImpl() {
    this.listeners = new ProjectLifeCycleListener[0];
  }

  /**
   * Used by the container when there is at least one ProjectLifeCycleListener implementation in container.
   */
  @Autowired(required = false)
  public ProjectLifeCycleListenersImpl(ProjectLifeCycleListener[] listeners) {
    this.listeners = listeners;
  }

  @Override
  public void onProjectsDeleted(Set<DeletedProject> projects) {
    checkNotNull(projects, "projects can't be null");
    if (projects.isEmpty()) {
      return;
    }

    Arrays.stream(listeners)
      .forEach(safelyCallListener(listener -> listener.onProjectsDeleted(projects)));
  }

  @Override
  public void onProjectBranchesChanged(Set<Project> projects, Set<String> impactedBranches) {
    checkNotNull(projects, "projects can't be null");
    if (projects.isEmpty()) {
      return;
    }

    Arrays.stream(listeners)
      .forEach(safelyCallListener(listener -> listener.onProjectBranchesChanged(projects, impactedBranches)));
  }

  @Override
  public void onProjectsRekeyed(Set<RekeyedProject> rekeyedProjects) {
    checkNotNull(rekeyedProjects, "rekeyedProjects can't be null");
    if (rekeyedProjects.isEmpty()) {
      return;
    }

    Arrays.stream(listeners)
      .forEach(safelyCallListener(listener -> listener.onProjectsRekeyed(rekeyedProjects)));
  }

  private static Consumer<ProjectLifeCycleListener> safelyCallListener(Consumer<ProjectLifeCycleListener> task) {
    return listener -> {
      try {
        task.accept(listener);
      } catch (Error | Exception e) {
        LOG.error("Call on ProjectLifeCycleListener \"{}\" failed", listener.getClass(), e);
      }
    };
  }
}
