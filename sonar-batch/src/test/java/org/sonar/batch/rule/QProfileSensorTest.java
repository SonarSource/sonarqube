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
package org.sonar.batch.rule;

import java.util.Collections;
import org.junit.Test;
import org.sonar.api.batch.ModuleLanguages;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.test.IsMeasure;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.qualityprofile.db.QualityProfileDao;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class QProfileSensorTest extends AbstractDaoTestCase {

  ModuleQProfiles moduleQProfiles = mock(ModuleQProfiles.class);
  ModuleLanguages moduleLanguages = mock(ModuleLanguages.class);
  Project project = mock(Project.class);
  SensorContext sensorContext = mock(SensorContext.class);

  @Test
  public void to_string() throws Exception {
    QualityProfileDao dao = mock(QualityProfileDao.class);
    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, moduleLanguages, dao);
    assertThat(sensor.toString()).isEqualTo("QProfileSensor");
  }

  @Test
  public void no_qprofiles() throws Exception {
    setupData("shared");
    QualityProfileDao dao = new QualityProfileDao(getMyBatis());
    when(moduleQProfiles.findAll()).thenReturn(Collections.<ModuleQProfiles.QProfile>emptyList());

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, moduleLanguages, dao);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    // measures are not saved
    verifyZeroInteractions(sensorContext);
  }

  @Test
  public void mark_profiles_as_used() throws Exception {
    setupData("shared");

    QualityProfileDao dao = new QualityProfileDao(getMyBatis());
    when(moduleQProfiles.findByLanguage("java")).thenReturn(new ModuleQProfiles.QProfile(dao.selectById(2)));
    when(moduleQProfiles.findByLanguage("php")).thenReturn(new ModuleQProfiles.QProfile(dao.selectById(3)));
    when(moduleQProfiles.findByLanguage("abap")).thenReturn(null);
    when(moduleLanguages.keys()).thenReturn(Arrays.asList("java", "php", "abap"));

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, moduleLanguages, dao);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    checkTable("mark_profiles_as_used", "rules_profiles");

    // no measures on multi-language modules
    verifyZeroInteractions(sensorContext);
  }

  @Test
  public void store_measures_on_single_lang_module() throws Exception {
    setupData("shared");

    QualityProfileDao dao = new QualityProfileDao(getMyBatis());
    when(moduleQProfiles.findByLanguage("java")).thenReturn(new ModuleQProfiles.QProfile(dao.selectById(2)));
    when(moduleQProfiles.findByLanguage("php")).thenReturn(new ModuleQProfiles.QProfile(dao.selectById(3)));
    when(moduleQProfiles.findByLanguage("abap")).thenReturn(null);
    when(moduleLanguages.keys()).thenReturn(Arrays.asList("java"));

    QProfileSensor sensor = new QProfileSensor(moduleQProfiles, moduleLanguages, dao);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    sensor.analyse(project, sensorContext);

    verify(sensorContext).saveMeasure(argThat(new IsMeasure(CoreMetrics.PROFILE, "Java Two")));
    verify(sensorContext).saveMeasure(argThat(new IsMeasure(CoreMetrics.PROFILE_VERSION, 20.0)));
  }
}
