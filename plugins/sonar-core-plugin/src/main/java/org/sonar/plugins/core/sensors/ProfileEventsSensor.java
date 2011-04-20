/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import java.util.List;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;

public class ProfileEventsSensor implements Sensor {
    
  private final RulesProfile profile;
  private final TimeMachine timeMachine;
    
  public ProfileEventsSensor(RulesProfile profile, TimeMachine timeMachine) {
    this.profile = profile;
    this.timeMachine = timeMachine;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext context) {
    if (profile == null) {
      return;
    }
    String currentProfile = profile.getName();
    int currentProfileId = profile.getId();
    int currentProfileVersion = profile.getVersion();
    
    TimeMachineQuery query = new TimeMachineQuery(project).setOnlyLastAnalysis(true)
      .setMetrics(CoreMetrics.PROFILE, CoreMetrics.PROFILE_VERSION);
    List<Measure> measures = timeMachine.getMeasures(query);
    Measure pastProfileMeasure = (measures != null && measures.size() == 2 ? measures.get(0) : null);
    Measure pastProfileVersionMeasure = (measures != null && measures.size() == 2 ? measures.get(1) : null);

    int pastProfileId = (pastProfileMeasure != null ? pastProfileMeasure.getIntValue() : -1);
    int pastProfileVersion = (pastProfileVersionMeasure != null ? pastProfileVersionMeasure.getIntValue() : 1);

    if (pastProfileId != currentProfileId) {
      //A different profile is used for this project
      context.createEvent(project, currentProfile + " V" + currentProfileVersion, 
          "A different quality profile was used", Event.CATEGORY_PROFILE, null);
    }
    else if (pastProfileVersion != currentProfileVersion) {
      //Same profile but new version
      context.createEvent(project, currentProfile + " V" + currentProfileVersion, 
          "A new version of the quality profile was used", Event.CATEGORY_PROFILE, null);
    }
  }
  
  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
