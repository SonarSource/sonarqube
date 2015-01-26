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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.batch.bootstrap.TaskProperties;
import org.sonar.batch.protocol.input.ProjectRepositories;

public class ProjectRepositoriesProvider extends ProviderAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectRepositoriesProvider.class);

  private ProjectRepositories projectReferentials;

  public ProjectRepositories provide(ProjectRepositoriesLoader loader, ProjectReactor reactor, TaskProperties taskProps) {
    if (projectReferentials == null) {
      TimeProfiler profiler = new TimeProfiler(LOG).start("Load project repositories");
      try {
        projectReferentials = loader.load(reactor, taskProps);
      } finally {
        profiler.stop();
      }
    }
    return projectReferentials;
  }
}
