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

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.protocol.input.ProjectRepositories;

public class ProjectRepositoriesProvider extends ProviderAdapter {

  private static final Logger LOG = Loggers.get(ProjectRepositoriesProvider.class);

  private ProjectRepositories projectReferentials;

  public ProjectRepositories provide(ProjectRepositoriesLoader loader, ProjectReactor reactor, TaskProperties taskProps, AnalysisMode analysisMode) {
    if (projectReferentials == null) {
      Profiler profiler = Profiler.create(LOG).startInfo("Load project repositories");
      projectReferentials = loader.load(reactor, taskProps);
      profiler.stopInfo();
      if (analysisMode.isPreview() && projectReferentials.lastAnalysisDate() == null) {
        LOG.warn("No analysis has been found on the server for this project. All issues will be marked as 'new'.");
      }
    }
    return projectReferentials;
  }
}
