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

import org.sonar.batch.protocol.input.QProfile;

import java.util.Collection;

import org.sonar.api.batch.AnalysisMode;
import org.sonar.batch.analysis.AnalysisProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.rule.ModuleQProfiles;
import org.picocontainer.injectors.ProviderAdapter;

public class QualityProfileProvider extends ProviderAdapter {
  private ModuleQProfiles profiles = null;

  public ModuleQProfiles provide(ProjectReactor projectReactor, QualityProfileLoader loader, AnalysisProperties props, AnalysisMode mode) {
    if (this.profiles == null) {
      String profile = null;
      if (!mode.isIssues()) {
        profile = props.property(ModuleQProfiles.SONAR_PROFILE_PROP);
      }
      Collection<QProfile> qps = loader.load(projectReactor.getRoot().getKeyWithBranch(), profile);
      profiles = new ModuleQProfiles(qps);
    }

    return profiles;
  }

}
