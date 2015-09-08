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

import org.sonar.api.batch.bootstrap.ProjectReactor;

import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.protocol.input.ProjectRepositories;

public class DefaultProjectRepositoriesFactory implements ProjectRepositoriesFactory {
  private static final String LOG_MSG = "Load project repositories";
  private static final Logger LOG = Loggers.get(DefaultProjectRepositoriesFactory.class);
  private static final String NON_EXISTING = "non1-existing2-project3-key";

  private final DefaultAnalysisMode analysisMode;
  private final ProjectRepositoriesLoader loader;
  private final AnalysisProperties props;
  private final ProjectReactor projectReactor;

  private ProjectRepositories projectReferentials;

  public DefaultProjectRepositoriesFactory(ProjectReactor projectReactor, DefaultAnalysisMode analysisMode, ProjectRepositoriesLoader loader, AnalysisProperties props) {
    this.projectReactor = projectReactor;
    this.analysisMode = analysisMode;
    this.loader = loader;
    this.props = props;
  }

  @Override
  public ProjectRepositories create() {
    if (projectReferentials == null) {
      Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
      MutableBoolean fromCache = new MutableBoolean();
      projectReferentials = loader.load(getProjectKey(), getSonarProfile(), fromCache);
      profiler.stopInfo(fromCache.booleanValue());

      if (analysisMode.isIssues() && projectReferentials.lastAnalysisDate() == null) {
        LOG.warn("No analysis has been found on the server for this project. All issues will be marked as 'new'.");
      }
    }
    return projectReferentials;
  }

  private String getProjectKey() {
    if (analysisMode.isNotAssociated()) {
      return NON_EXISTING;
    }
    return projectReactor.getRoot().getKeyWithBranch();
  }

  private String getSonarProfile() {
    String profile = null;
    if (!analysisMode.isIssues()) {
      profile = props.property(ModuleQProfiles.SONAR_PROFILE_PROP);
    }
    return profile;
  }
}
