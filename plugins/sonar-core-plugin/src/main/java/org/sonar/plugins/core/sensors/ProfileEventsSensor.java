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

import org.sonar.api.batch.Event;
import org.sonar.api.batch.ModuleLanguages;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.batch.rule.RulesProfileWrapper;

import java.util.List;

public class ProfileEventsSensor implements Sensor {

  private final RulesProfile profile;
  private final TimeMachine timeMachine;
  private final ModuleLanguages moduleLanguages;

  public ProfileEventsSensor(RulesProfile profile, TimeMachine timeMachine, ModuleLanguages moduleLanguages) {
    this.profile = profile;
    this.timeMachine = timeMachine;
    this.moduleLanguages = moduleLanguages;
  }

  public boolean shouldExecuteOnProject(Project project) {
    // Views will define a fake profile
    return profile instanceof RulesProfileWrapper;
  }

  public void analyse(Project project, SensorContext context) {
    RulesProfileWrapper profileWrapper = (RulesProfileWrapper) profile;
    for (String languageKey : moduleLanguages.keys()) {
      RulesProfile realProfile = profileWrapper.getProfileByLanguage(languageKey);
      Measure pastProfileMeasure = getPreviousMeasure(project, CoreMetrics.PROFILE);
      if (pastProfileMeasure == null) {
        // first analysis
        return;
      }
      int pastProfileId = pastProfileMeasure.getIntValue();
      Measure pastProfileVersionMeasure = getPreviousMeasure(project, CoreMetrics.PROFILE_VERSION);
      final int pastProfileVersion;
      // first analysis with versions
      if (pastProfileVersionMeasure == null) {
        pastProfileVersion = 1;
      } else {
        pastProfileVersion = pastProfileVersionMeasure.getIntValue();
      }
      String pastProfile = formatProfileDescription(pastProfileMeasure.getData(), pastProfileVersion);

      int currentProfileId = realProfile.getId();
      int currentProfileVersion = realProfile.getVersion();
      String currentProfile = formatProfileDescription(realProfile.getName(), currentProfileVersion);

      if ((pastProfileId != currentProfileId) || (pastProfileVersion != currentProfileVersion)) {
        // A different profile is used for this project or new version of same profile
        context.createEvent(project, currentProfile, currentProfile + " is used instead of " + pastProfile, Event.CATEGORY_PROFILE, null);
      }
    }
  }

  private static String formatProfileDescription(String name, int version) {
    return name + " version " + version;
  }

  private Measure getPreviousMeasure(Project project, Metric metric) {
    TimeMachineQuery query = new TimeMachineQuery(project)
      .setOnlyLastAnalysis(true)
      .setMetrics(metric);
    List<Measure> measures = timeMachine.getMeasures(query);
    if (measures.isEmpty()) {
      return null;
    }
    return measures.get(0);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
