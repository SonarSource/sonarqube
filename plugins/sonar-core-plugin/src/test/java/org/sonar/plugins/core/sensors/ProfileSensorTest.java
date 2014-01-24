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

import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Languages;
import org.sonar.api.test.IsMeasure;
import org.sonar.batch.RulesProfileWrapper;
import org.sonar.batch.scan.language.DefaultModuleLanguages;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProfileSensorTest {

  @Test
  public void saveProfile() {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getId()).thenReturn(22);
    when(profile.getName()).thenReturn("fake");
    when(profile.getVersion()).thenReturn(2);

    DefaultModuleLanguages moduleLanguages = new DefaultModuleLanguages(new Settings(), new Languages(Java.INSTANCE));
    moduleLanguages.addLanguage("java");
    Map<String, RulesProfile> ruleProfilesPerLanguages = new HashMap<String, RulesProfile>();
    ruleProfilesPerLanguages.put("java", profile);
    RulesProfileWrapper wrapper = new RulesProfileWrapper(moduleLanguages, ruleProfilesPerLanguages);

    SensorContext context = mock(SensorContext.class);
    DatabaseSession session = mock(DatabaseSession.class);

    ProfileSensor sensor = new ProfileSensor(wrapper, session, moduleLanguages);
    sensor.analyse(null, context);

    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.PROFILE, 22d)));
    verify(context).saveMeasure(argThat(new IsMeasure(CoreMetrics.PROFILE_VERSION, 2d)));
  }
}
