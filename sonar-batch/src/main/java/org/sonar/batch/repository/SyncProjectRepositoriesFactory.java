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

import javax.annotation.Nullable;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.protocol.input.ProjectRepositories;

public class SyncProjectRepositoriesFactory implements ProjectRepositoriesFactory {
  private static final String LOG_MSG = "Load project repositories";
  private static final Logger LOG = Loggers.get(SyncProjectRepositoriesFactory.class);
  private static final String NON_EXISTING = "non1-existing2-project3-key";

  private final ProjectRepositoriesLoader loader;
  private final String projectKey;

  private ProjectRepositories projectRepositories;

  public SyncProjectRepositoriesFactory(@Nullable String projectKey, ProjectRepositoriesLoader loader) {
    this.projectKey = projectKey;
    this.loader = loader;
  }

  @Override
  public ProjectRepositories create() {
    if (projectRepositories == null) {
      projectRepositories = newInstance();
    }

    return projectRepositories;
  }

  public ProjectRepositories newInstance() {
    if (projectRepositories == null) {
      Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
      MutableBoolean fromCache = new MutableBoolean();
      projectRepositories = loader.load(getProjectKey(projectKey), null, fromCache);
      profiler.stopInfo(fromCache.booleanValue());

      if (projectRepositories.lastAnalysisDate() == null) {
        LOG.warn("No analysis has been found on the server for this project. All issues will be marked as 'new'.");
      }
    }
    return projectRepositories;
  }

  private static String getProjectKey(@Nullable String projectKey) {
    if (projectKey == null) {
      return NON_EXISTING;
    }
    return projectKey;
  }
}
