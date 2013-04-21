/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;

public class ProfileSensor implements Sensor {

  private final RulesProfile profile;
  private final DatabaseSession session;

  public ProfileSensor(RulesProfile profile, DatabaseSession session) {
    this.profile = profile;
    this.session = session;
  }

  public boolean shouldExecuteOnProject(Project project) {
    // Views will define a fake profile with a null id
    return profile.getId() != null;
  }

  public void analyse(Project project, SensorContext context) {
    Measure measure = new Measure(CoreMetrics.PROFILE, profile.getName());
    Measure measureVersion = new Measure(CoreMetrics.PROFILE_VERSION, Integer.valueOf(profile.getVersion()).doubleValue());
    if (profile.getId() != null) {
      measure.setValue(profile.getId().doubleValue());

      profile.setUsed(true);
      session.merge(profile);
    }
    context.saveMeasure(measure);
    context.saveMeasure(measureVersion);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
