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

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.component.persistence.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
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
  QProfileLoader qProfileLoader;

  @Mock
  RuleService ruleService;

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

    tester = new WsTester(new BatchWs(mock(BatchIndex.class), mock(GlobalReferentialsAction.class),
      new ProjectReferentialsAction(dbClient, propertiesDao, qProfileFactory, qProfileLoader, ruleService, languages)));
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

    when(qProfileFactory.getDefault(session, "java")).thenReturn(
      QualityProfileDto.createFor("abcd").setName("Default").setLanguage("java").setRulesUpdatedAt("2014-01-14T14:00:00+0200")
    );

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

    when(qProfileFactory.getDefault(session, "java")).thenReturn(
      QualityProfileDto.createFor("abcd").setName("Default").setLanguage("java").setRulesUpdatedAt("2014-01-14T14:00:00+0200")
    );

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
  public void fail_when_quality_profile_for_a_language() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.DRY_RUN_EXECUTION);

    WsTester.TestRequest request = tester.newGetRequest("batch", "project").setParam("key", "org.codehaus.sonar:sonar");

    try {
      request.execute();
    } catch (Exception e){
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("No quality profile can been found on language 'java' for project 'org.codehaus.sonar:sonar'");
    }
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

  @Test
  public void return_quality_profile_from_given_profile_name() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.DRY_RUN_EXECUTION);
    String projectKey = "org.codehaus.sonar:sonar";

    when(qProfileFactory.getByNameAndLanguage(session, "Default", "java")).thenReturn(
      QualityProfileDto.createFor("abcd").setName("Default").setLanguage("java").setRulesUpdatedAt("2014-01-14T14:00:00+0200")
    );

    WsTester.TestRequest request = tester.newGetRequest("batch", "project").setParam("key", projectKey).setParam("profile", "Default");
    request.execute().assertJson(getClass(), "return_quality_profile_from_given_profile_name.json");
  }

  @Test
  public void return_active_rules() throws Exception {
    MockUserSession.set().setLogin("john").setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.DRY_RUN_EXECUTION);
    String projectKey = "org.codehaus.sonar:sonar";

    when(qProfileFactory.getByProjectAndLanguage(session, projectKey, "java")).thenReturn(
      QualityProfileDto.createFor("abcd").setName("Default").setLanguage("java").setRulesUpdatedAt("2014-01-14T14:00:00+0200")
    );

    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    ActiveRule activeRule = mock(ActiveRule.class);
    when(activeRule.key()).thenReturn(ActiveRuleKey.of("abcd", ruleKey));
    when(activeRule.severity()).thenReturn(Severity.MINOR);
    when(activeRule.params()).thenReturn(ImmutableMap.of("max", "2"));
    when(qProfileLoader.findActiveRulesByProfile("abcd")).thenReturn(newArrayList(activeRule));

    Rule rule = mock(Rule.class);
    when(rule.name()).thenReturn("Avoid Cycle");
    when(rule.internalKey()).thenReturn("squid-1");
    when(ruleService.getByKey(ruleKey)).thenReturn(rule);

    WsTester.TestRequest request = tester.newGetRequest("batch", "project").setParam("key", projectKey);
    request.execute().assertJson(getClass(), "return_active_rules.json");
  }

}
