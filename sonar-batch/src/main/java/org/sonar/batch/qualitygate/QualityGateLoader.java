/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.qualitygate;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.batch.rule.RulesProfileWrapper;

/**
 * Executed on every module to feed {@link org.sonar.batch.qualitygate.ProjectAlerts}
 */
public class QualityGateLoader implements Sensor {

  private final FileSystem fs;
  private final RulesProfileWrapper qProfile;
  private final ProjectAlerts projectAlerts;

  public QualityGateLoader(FileSystem fs, RulesProfileWrapper qProfile, ProjectAlerts projectAlerts) {
    this.fs = fs;
    this.qProfile = qProfile;
    this.projectAlerts = projectAlerts;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void analyse(Project module, SensorContext context) {
    for (String lang : fs.languages()) {
      RulesProfile profile = qProfile.getProfileByLanguage(lang);
      if (profile != null) {
        projectAlerts.addAll(profile.getAlerts());
      }
    }
  }

  @Override
  public String toString() {
    return "Quality gate loader";
  }
}
