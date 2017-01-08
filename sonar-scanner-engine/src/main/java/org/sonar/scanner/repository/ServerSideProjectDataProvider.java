/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.repository;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.analysis.DefaultAnalysisMode;

public class ServerSideProjectDataProvider extends ProviderAdapter {
  private static final Logger LOG = Loggers.get(ServerSideProjectDataProvider.class);
  private static final String LOG_MSG = "Load server side project data";
  private ServerSideProjectData serverSideProjectDef = null;

  public ServerSideProjectData provide(ServerSideProjectDataLoader loader, ProjectKey projectKey, DefaultAnalysisMode mode) {
    if (serverSideProjectDef == null) {
      Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
      serverSideProjectDef = loader.load(projectKey.get(), mode.isIssues());
      checkProject(mode);
      profiler.stopInfo();
    }

    return serverSideProjectDef;
  }

  private void checkProject(DefaultAnalysisMode mode) {
    if (mode.isIssues()) {
      if (!serverSideProjectDef.exists()) {
        LOG.warn("Project doesn't exist on the server. All issues will be marked as 'new'.");
      } else if (serverSideProjectDef.lastAnalysisDate() == null) {
        LOG.warn("No analysis has been found on the server for this project. All issues will be marked as 'new'.");
      }
    }
  }
}
