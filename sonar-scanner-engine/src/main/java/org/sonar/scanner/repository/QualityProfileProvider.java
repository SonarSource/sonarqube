/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.repository;

import java.util.List;
import javax.annotation.CheckForNull;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.analysis.AnalysisProperties;
import org.sonar.scanner.rule.ModuleQProfiles;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;

public class QualityProfileProvider extends ProviderAdapter {
  private static final Logger LOG = Loggers.get(QualityProfileProvider.class);
  private static final String LOG_MSG = "Load quality profiles";
  private ModuleQProfiles profiles = null;

  public ModuleQProfiles provide(ProjectKey projectKey, QualityProfileLoader loader, ProjectRepositories projectRepositories, AnalysisProperties props) {
    if (this.profiles == null) {
      List<QualityProfile> profileList;
      Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
      if (!projectRepositories.exists()) {
        profileList = loader.loadDefault(getSonarProfile(props));
      } else {
        profileList = loader.load(projectKey.get(), getSonarProfile(props));
      }
      profiler.stopInfo();
      profiles = new ModuleQProfiles(profileList);
    }

    return profiles;
  }

  @CheckForNull
  private static String getSonarProfile(AnalysisProperties props) {
    String profile = props.property(ModuleQProfiles.SONAR_PROFILE_PROP);
    if (profile != null) {
      LOG.warn("Ability to set quality profile from command line using '" + ModuleQProfiles.SONAR_PROFILE_PROP
        + "' is deprecated and will be dropped in a future SonarQube version. Please configure quality profile used by your project on SonarQube server.");
    }
    return profile;
  }

}
