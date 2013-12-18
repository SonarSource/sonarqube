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

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.ProfileRuleQuery;
import org.sonar.server.rule.ProfileRules;
import org.sonar.server.user.UserSession;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QProfilesTest {

  @Mock
  QualityProfileDao qualityProfileDao;

  @Mock
  ResourceDao resourceDao;

  @Mock
  RuleFinder ruleFinder;

  @Mock
  QProfileProjectService projectService;

  @Mock
  QProfileSearch search;

  @Mock
  QProfileOperations service;

  @Mock
  ProfileRules rules;

  QProfiles qProfiles;

  @Before
  public void setUp() throws Exception {
    qProfiles = new QProfiles(qualityProfileDao, resourceDao, ruleFinder, projectService, search, service, rules);
  }

  @Test
  public void search_profile() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("Default").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);

    qProfiles.profile(1);
    verify(qualityProfileDao).selectById(1);
  }

  @Test
  public void search_profiles() throws Exception {
    qProfiles.allProfiles();
    verify(search).allProfiles();
  }

  @Test
  public void search_default_profile_by_language() throws Exception {
    qProfiles.defaultProfile("java");
    verify(search).defaultProfile("java");
  }

  @Test
  public void create_new_profile() throws Exception {
    Map<String, String> xmlProfilesByPlugin = newHashMap();
    qProfiles.newProfile("Default", "java", xmlProfilesByPlugin);
    verify(service).newProfile(eq("Default"), eq("java"), eq(xmlProfilesByPlugin), any(UserSession.class));
  }

  @Test
  public void fail_to_create_profile_without_name() throws Exception {
    try {
      qProfiles.newProfile("", "java", Maps.<String, String>newHashMap());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void fail_to_create_profile_if_already_exists() throws Exception {
    try {
      when(qualityProfileDao.selectByNameAndLanguage(anyString(), anyString())).thenReturn(new QualityProfileDto());
      qProfiles.newProfile("Default", "java", Maps.<String, String>newHashMap());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void rename_profile() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("Default").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);

    qProfiles.renameProfile(1, "Default profile");
    verify(service).renameProfile(eq(qualityProfile), eq("Default profile"), any(UserSession.class));
  }

  @Test
  public void fail_to_rename_profile_on_unknown_profile() throws Exception {
    try {
      when(qualityProfileDao.selectById(1)).thenReturn(null);
      qProfiles.renameProfile(1, "Default profile");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void fail_to_rename_profile_when_missing_new_name() throws Exception {
    try {
      qProfiles.renameProfile(1, "");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
    verify(qualityProfileDao, never()).update(any(QualityProfileDto.class));
  }

  @Test
  public void fail_to_rename_profile_if_already_exists() throws Exception {
    try {
      when(qualityProfileDao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
      when(qualityProfileDao.selectByNameAndLanguage(eq("New Default"), anyString())).thenReturn(new QualityProfileDto().setName("New Default").setLanguage("java"));
      qProfiles.renameProfile(1, "New Default");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void update_default_profile() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("Default").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);

    qProfiles.setDefaultProfile(1);
    verify(service).setDefaultProfile(eq(qualityProfile), any(UserSession.class));
  }

  @Test
  public void update_default_profile_from_name_and_language() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("Default").setLanguage("java");
    when(qualityProfileDao.selectByNameAndLanguage("Default", "java")).thenReturn(qualityProfile);

    qProfiles.setDefaultProfile("Default", "java");
    verify(service).setDefaultProfile(eq(qualityProfile), any(UserSession.class));
  }

  @Test
  public void projects() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    qProfiles.projects(1);
    verify(projectService).projects(qualityProfile);
  }

  @Test
  public void get_profiles_from_project_id() throws Exception {
    qProfiles.profiles(1);
    verify(projectService).profiles(1);
  }

  @Test
  public void add_project() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    when(resourceDao.findById(10L)).thenReturn(project);

    qProfiles.addProject(1, 10L);
    verify(projectService).addProject(eq(qualityProfile), eq(project), any(UserSession.class));
  }

  @Test
  public void fail_to_add_project_if_project_not_found() throws Exception {
    try {
      QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
      when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
      when(resourceDao.findById(10L)).thenReturn(null);

      qProfiles.addProject(1, 10L);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
    verifyZeroInteractions(projectService);
  }

  @Test
  public void remove_project_by_quality_profile_id() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    when(resourceDao.findById(10L)).thenReturn(project);

    qProfiles.removeProject(1, 10L);
    verify(projectService).removeProject(eq(qualityProfile), eq(project), any(UserSession.class));
  }

  @Test
  public void remove_project_by_language() throws Exception {
    ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");
    when(resourceDao.findById(10L)).thenReturn(project);

    qProfiles.removeProjectByLanguage("java", 10L);
    verify(projectService).removeProject(eq("java"), eq(project), any(UserSession.class));
  }

  @Test
  public void remove_all_projects() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);

    qProfiles.removeAllProjects(1);
    verify(projectService).removeAllProjects(eq(qualityProfile), any(UserSession.class));
  }

  @Test
  public void testSearchActiveRules() throws Exception {
    final int profileId = 42;
    ProfileRuleQuery query = ProfileRuleQuery.create(profileId);
    Paging paging = Paging.create(20, 1);
    QProfileRuleResult result = mock(QProfileRuleResult.class);
    when(rules.searchActiveRules(query, paging)).thenReturn(result);
    assertThat(qProfiles.searchActiveRules(query, paging)).isEqualTo(result);
  }

  @Test
  public void testSearchInactiveRules() throws Exception {
    final int profileId = 42;
    ProfileRuleQuery query = ProfileRuleQuery.create(profileId);
    Paging paging = Paging.create(20, 1);
    QProfileRuleResult result = mock(QProfileRuleResult.class);
    when(rules.searchInactiveRules(query, paging)).thenReturn(result);
    assertThat(qProfiles.searchInactiveRules(query, paging)).isEqualTo(result);
  }

  @Test
  public void activate_rule() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    Rule rule = Rule.create().setRepositoryKey("squid").setKey("AvoidCycle");
    rule.setId(10);
    when(ruleFinder.findById(10)).thenReturn(rule);

    qProfiles.activateRule(1, 10, Severity.BLOCKER);

    verify(service).activateRule(eq(qualityProfile), eq(rule), eq(Severity.BLOCKER), any(UserSession.class));
  }

  @Test
  public void fail_to_activate_rule_if_rule_not_found() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    Rule rule = Rule.create().setRepositoryKey("squid").setKey("AvoidCycle");
    rule.setId(10);
    when(ruleFinder.findById(10)).thenReturn(null);

    try {
      qProfiles.activateRule(1, 10, Severity.BLOCKER);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
    verifyZeroInteractions(service);
  }

  @Test
  public void deactivate_rule() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    Rule rule = Rule.create().setRepositoryKey("squid").setKey("AvoidCycle");
    rule.setId(10);
    when(ruleFinder.findById(10)).thenReturn(rule);

    qProfiles.deactivateRule(1, 10);

    verify(service).deactivateRule(eq(qualityProfile), eq(rule), any(UserSession.class));
  }

}
