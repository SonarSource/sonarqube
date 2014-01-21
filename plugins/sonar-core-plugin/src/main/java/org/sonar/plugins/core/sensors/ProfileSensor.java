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
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.batch.RulesProfileWrapper;
import org.sonar.batch.scan.language.ModuleLanguages;

public class ProfileSensor implements Sensor {

  private final RulesProfile profile;
  private final DatabaseSession session;
  private ModuleLanguages languages;

  public ProfileSensor(RulesProfile profile, DatabaseSession session, ModuleLanguages languages) {
    this.profile = profile;
    this.session = session;
    this.languages = languages;
  }

  public boolean shouldExecuteOnProject(Project project) {
    // Views will define a fake profile
    return profile instanceof RulesProfileWrapper;
  }

  public void analyse(Project project, SensorContext context) {
    RulesProfileWrapper wrapper = (RulesProfileWrapper) profile;
    for (String languageKey : languages.getModuleLanguageKeys()) {
      RulesProfile realProfile = wrapper.getProfileByLanguage(languageKey);
      Measure measure = new Measure(CoreMetrics.PROFILE, profile.getName());
      Measure measureVersion = new Measure(CoreMetrics.PROFILE_VERSION, Integer.valueOf(profile.getVersion()).doubleValue());
      if (realProfile.getId() != null) {
        measure.setValue(realProfile.getId().doubleValue());

        realProfile.setUsed(true);
        session.merge(realProfile);
      }
      context.saveMeasure(measure);
      context.saveMeasure(measureVersion);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
