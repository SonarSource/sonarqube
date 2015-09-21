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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonarqube.ws.QualityProfiles.WsSearchResponse.QualityProfile;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectKey;

import java.util.List;

import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.batch.rule.ModuleQProfiles;
import org.picocontainer.injectors.ProviderAdapter;

public class QualityProfileProvider extends ProviderAdapter {
  private static final Logger LOG = Loggers.get(QualityProfileProvider.class);
  private static final String LOG_MSG = "Load quality profiles";
  private ModuleQProfiles profiles = null;

  public ModuleQProfiles provide(ProjectKey projectKey, QualityProfileLoader loader, ProjectRepositories projectRepositories, AnalysisProperties props, DefaultAnalysisMode mode) {
    if (this.profiles == null) {
      List<QualityProfile> profileList;
      MutableBoolean fromCache = new MutableBoolean();
      
      Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
      if (mode.isNotAssociated() || !projectRepositories.exists()) {
        profileList = loader.loadDefault(fromCache);
      } else {
        profileList = loader.load(projectKey.get(), getSonarProfile(props, mode), fromCache);
      }
      profiler.stopInfo(fromCache.booleanValue());
      profiles = new ModuleQProfiles(profileList);
    }

    return profiles;
  }

  private static String getSonarProfile(AnalysisProperties props, DefaultAnalysisMode mode) {
    String profile = null;
    if (!mode.isIssues()) {
      profile = props.property(ModuleQProfiles.SONAR_PROFILE_PROP);
    }
    return profile;
  }

}
