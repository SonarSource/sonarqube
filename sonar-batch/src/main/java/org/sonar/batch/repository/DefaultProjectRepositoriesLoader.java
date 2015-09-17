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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.util.BatchUtils;

public class DefaultProjectRepositoriesLoader implements ProjectRepositoriesLoader {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectRepositoriesLoader.class);
  private static final String BATCH_PROJECT_URL = "/batch/project";

  private final WSLoader wsLoader;
  private final DefaultAnalysisMode analysisMode;

  public DefaultProjectRepositoriesLoader(WSLoader wsLoader, DefaultAnalysisMode analysisMode) {
    this.wsLoader = wsLoader;
    this.analysisMode = analysisMode;
  }

  @Override
  public ProjectRepositories load(String projectKeyWithBranch, @Nullable String sonarProfile, @Nullable MutableBoolean fromCache) {
    String url = BATCH_PROJECT_URL + "?key=" + BatchUtils.encodeForUrl(projectKeyWithBranch);
    if (sonarProfile != null) {
      LOG.warn("Ability to set quality profile from command line using '" + ModuleQProfiles.SONAR_PROFILE_PROP
        + "' is deprecated and will be dropped in a future SonarQube version. Please configure quality profile used by your project on SonarQube server.");
      url += "&profile=" + BatchUtils.encodeForUrl(sonarProfile);
    }
    url += "&preview=" + analysisMode.isIssues();

    ProjectRepositories projectRepositories = load(url, fromCache);
    return projectRepositories;
  }

  private ProjectRepositories load(String resource, @Nullable MutableBoolean fromCache) {
    WSLoaderResult<String> result = wsLoader.loadString(resource);
    if(fromCache != null) {
      fromCache.setValue(result.isFromCache());
    }
    return ProjectRepositories.fromJson(result.get());
  }
}
