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

package org.sonar.server.qualityprofile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QProfileProjectServiceTest {

  @Mock
  QualityProfileDao qualityProfileDao;

  @Mock
  PropertiesDao propertiesDao;

  QProfileProjectService service;

  UserSession authorizedSession = MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

  @Before
  public void setUp() throws Exception {
    service = new QProfileProjectService(qualityProfileDao, propertiesDao);
  }

  @Test
  public void search_projects() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectProjects("My profile", "sonar.profile.java")).thenReturn(newArrayList(new ComponentDto().setId(1L).setKey("org.codehaus.sonar:sonar").setName("SonarQube")));

    QProfileProjects result = service.projects(qualityProfile);
    assertThat(result.profile()).isNotNull();
    assertThat(result.projects()).hasSize(1);
  }

  @Test
  public void search_profiles_from_project() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectByProject(1L, "sonar.profile.%")).thenReturn(newArrayList(qualityProfile));

    List<QProfile> result = service.profiles(1L);
    assertThat(result).hasSize(1);
  }

  @Test
  public void add_project() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");

    service.addProject(qualityProfile, project, authorizedSession);

    ArgumentCaptor<PropertyDto> argumentCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).setProperty(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getKey()).isEqualTo("sonar.profile.java");
    assertThat(argumentCaptor.getValue().getValue()).isEqualTo("My profile");
    assertThat(argumentCaptor.getValue().getResourceId()).isEqualTo(10);
  }

  @Test
  public void fail_to_add_project_without_profile_admin_permission() throws Exception {
    try {
      QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
      ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");

      service.addProject(qualityProfile, project, MockUserSession.create());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyNoMoreInteractions(qualityProfileDao);
    verifyNoMoreInteractions(propertiesDao);
  }

  @Test
  public void remove_project_by_quality_profile() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");

    service.removeProject(qualityProfile, project, authorizedSession);

    verify(propertiesDao).deleteProjectProperty("sonar.profile.java", 10L);
  }

  @Test
  public void remove_project_by_language() throws Exception {
    ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");

    service.removeProject("java", project, authorizedSession);

    verify(propertiesDao).deleteProjectProperty("sonar.profile.java", 10L);
  }

  @Test
  public void remove_all_projects() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");

    service.removeAllProjects(qualityProfile, authorizedSession);

    verify(propertiesDao).deleteProjectProperties("sonar.profile.java", "My profile");
  }
}
