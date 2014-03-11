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
package org.sonar.batch.qualitygate;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.batch.rule.RulesProfileWrapper;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LegacyQualityGateLoaderTest {

  DefaultFileSystem fs = new DefaultFileSystem();
  RulesProfileWrapper qProfile = mock(RulesProfileWrapper.class);
  ProjectAlerts alerts = new ProjectAlerts();
  LegacyQualityGateLoader loader = new LegacyQualityGateLoader(fs, qProfile, alerts);

  @Test
  public void should_always_be_executed() throws Exception {
    assertThat(loader.shouldExecuteOnProject(new Project("struts"))).isTrue();
  }

  @Test
  public void test_toString() throws Exception {
    assertThat(loader.toString()).isEqualTo("Quality gate loader");
  }

  @Test
  public void register_project_alerts() throws Exception {
    fs.addLanguages("java", "php");

    RulesProfile javaProfile = new RulesProfile();
    javaProfile.setAlerts(Lists.newArrayList(new Alert()));
    when(qProfile.getProfileByLanguage("java")).thenReturn(javaProfile);

    loader.analyse(null, null);

    assertThat(alerts.all()).hasSize(1);
  }
}
