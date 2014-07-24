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

package org.sonar.server.batch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.component.persistence.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectReferentialsActionTest {

  @Mock
  DbSession session;

  @Mock
  ComponentDao componentDao;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  QProfileFactory qProfileFactory;

  @Mock
  Languages languages;

  @Mock
  Language language;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = mock(DbClient.class);
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);

    when(language.getKey()).thenReturn("java");
    when(languages.all()).thenReturn(new Language[]{language});

    tester = new WsTester(new BatchWs(mock(BatchIndex.class), mock(GlobalReferentialsAction.class), new ProjectReferentialsAction(dbClient, propertiesDao, qProfileFactory, languages)));
  }

  @Test
  public void return_settings_by_modules() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.DRY_RUN_EXECUTION);
    String projectKey = "org.codehaus.sonar:sonar";
    String moduleKey = "org.codehaus.sonar:sonar-server";

    when(componentDao.findModulesByProject(projectKey, session)).thenReturn(newArrayList(
      new ComponentDto().setKey(projectKey),
      new ComponentDto().setKey(moduleKey)
      ));

    when(propertiesDao.selectProjectProperties(projectKey, session)).thenReturn(newArrayList(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR"),
      new PropertyDto().setKey("sonar.jira.login.secured").setValue("john")
      ));

    when(propertiesDao.selectProjectProperties(moduleKey, session)).thenReturn(newArrayList(
      new PropertyDto().setKey("sonar.coverage.exclusions").setValue("**/*.java")
    ));

    WsTester.TestRequest request = tester.newGetRequest("batch", "project").setParam("key", projectKey);
    request.execute().assertJson(getClass(), "return_settings_by_modules.json");
  }

  @Test
  public void return_settings_by_modules_without_empty_module_settings() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.DRY_RUN_EXECUTION);
    String projectKey = "org.codehaus.sonar:sonar";
    String moduleKey = "org.codehaus.sonar:sonar-server";

    when(componentDao.findModulesByProject(projectKey, session)).thenReturn(newArrayList(
      new ComponentDto().setKey(projectKey),
      new ComponentDto().setKey(moduleKey)
    ));

    when(propertiesDao.selectProjectProperties(projectKey, session)).thenReturn(newArrayList(
      new PropertyDto().setKey("sonar.jira.project.key").setValue("SONAR"),
      new PropertyDto().setKey("sonar.jira.login.secured").setValue("john")
    ));
    // No property on module

    WsTester.TestRequest request = tester.newGetRequest("batch", "project").setParam("key", projectKey);
    request.execute().assertJson(getClass(), "return_settings_by_modules_without_empty_module_settings.json");
  }

  @Test
  public void return_quality_profiles() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.DRY_RUN_EXECUTION);

    String projectKey = "org.codehaus.sonar:sonar";

    when(qProfileFactory.getByProjectAndLanguage(session, projectKey, "java")).thenReturn(
      QualityProfileDto.createFor("abcd").setName("Default").setLanguage("java").setRulesUpdatedAt("2014-01-14T14:00:00+0200")
    );

    WsTester.TestRequest request = tester.newGetRequest("batch", "project").setParam("key", projectKey);
    request.execute().assertJson(getClass(), "return_quality_profiles.json");
  }

  @Test
  public void return_quality_profile_from_default_profile() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.DRY_RUN_EXECUTION);

    String projectKey = "org.codehaus.sonar:sonar";

    when(qProfileFactory.getDefault(session, "java")).thenReturn(
      QualityProfileDto.createFor("abcd").setName("Default").setLanguage("java").setRulesUpdatedAt("2014-01-14T14:00:00+0200")
    );

    WsTester.TestRequest request = tester.newGetRequest("batch", "project").setParam("key", projectKey);
    request.execute().assertJson(getClass(), "return_quality_profile_from_default_profile.json");
  }

}
