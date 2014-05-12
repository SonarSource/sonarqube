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

package org.sonar.server.qualityprofile;

import org.elasticsearch.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.MockUserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QProfileProjectLookupTest {

  @Mock
  MyBatis myBatis;

  @Mock
  DbSession session;

  @Mock
  QualityProfileDao qualityProfileDao;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  AuthorizationDao authorizationDao;

  QProfileProjectLookup lookup;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession(false)).thenReturn(session);
    lookup = new QProfileProjectLookup(myBatis, qualityProfileDao, authorizationDao);
  }

  @Test
  public void search_projects() throws Exception {
    int userId = 42;
    MockUserSession.set().setUserId(userId);
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1, session)).thenReturn(qualityProfile);
    String key1 = "org.codehaus.sonar:sonar1";
    String key2 = "org.codehaus.sonar:sonar2";
    when(qualityProfileDao.selectProjects("My profile", "sonar.profile.java", session)).thenReturn(newArrayList(
      new ComponentDto().setId(1L).setKey(key1).setName("SonarQube One"),
      new ComponentDto().setId(1L).setKey(key2).setName("SonarQube Two")));

    when(authorizationDao.selectAuthorizedRootProjectsKeys(userId, UserRole.USER)).thenReturn(Sets.newHashSet(key1));

    assertThat(lookup.projects(1)).hasSize(1);
  }

  @Test
  public void fail_to_search_projects_if_profile_not_found() throws Exception {
    try {
      when(qualityProfileDao.selectById(1, session)).thenReturn(null);
      when(qualityProfileDao.selectProjects("My profile", "sonar.profile.java", session)).thenReturn(newArrayList(new ComponentDto().setId(1L).setKey("org.codehaus.sonar:sonar").setName("SonarQube")));
      lookup.projects(1);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void count_projects() throws Exception {
    lookup.countProjects(new QProfile().setId(1).setName("My profile").setLanguage("java"));
    verify(qualityProfileDao).countProjects("My profile", "sonar.profile.java");
  }

  @Test
  public void search_profiles_from_project() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectByProjectAndLanguage(1L, "java", "sonar.profile.java")).thenReturn(qualityProfile);

    QProfile result = lookup.findProfileByProjectAndLanguage(1L, "java");
    assertThat(result).isNotNull();
  }

  @Test
  public void return_null_when_no_profile_when_searching_for_profiles_from_project() throws Exception {
    when(qualityProfileDao.selectByProjectAndLanguage(1L, "java", "sonar.profile.java")).thenReturn(null);

    QProfile result = lookup.findProfileByProjectAndLanguage(1L, "java");
    assertThat(result).isNull();
  }

}
