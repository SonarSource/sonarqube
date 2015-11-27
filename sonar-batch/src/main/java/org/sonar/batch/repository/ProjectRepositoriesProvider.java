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
package org.sonar.batch.repository;

import org.sonar.api.utils.log.Profiler;

import org.sonar.api.batch.bootstrap.ProjectKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.picocontainer.injectors.ProviderAdapter;

public class ProjectRepositoriesProvider extends ProviderAdapter {
  private static final Logger LOG = Loggers.get(ProjectRepositoriesProvider.class);
  private static final String LOG_MSG = "Load project repositories";
  private ProjectRepositories project = null;

  public ProjectRepositories provide(ProjectRepositoriesLoader loader, ProjectKey projectKey, DefaultAnalysisMode mode) {
    if (project == null) {
      MutableBoolean fromCache = new MutableBoolean(false);
      Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
      if (mode.isNotAssociated()) {
        project = createNonAssociatedProjectRepositories();
        profiler.stopInfo();
      } else {
        project = loader.load(projectKey.get(), mode.isIssues(), fromCache);
        checkProject(mode);
        profiler.stopInfo(fromCache.booleanValue());
      }

    }

    return project;
  }

  private void checkProject(DefaultAnalysisMode mode) {
    if (mode.isIssues()) {
      if (!project.exists()) {
        LOG.warn("Project doesn't exist on the server. All issues will be marked as 'new'.");
      } else if (project.lastAnalysisDate() == null && !mode.isNotAssociated()) {
        LOG.warn("No analysis has been found on the server for this project. All issues will be marked as 'new'.");
      }
    }
  }

  private static ProjectRepositories createNonAssociatedProjectRepositories() {
    return new ProjectRepositories();
  }
}
