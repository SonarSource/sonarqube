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
import org.sonar.core.component.ComponentDto;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
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
  ActiveRuleDao activeRuleDao;

  @Mock
  RuleDao ruleDao;

  @Mock
  ResourceDao resourceDao;

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
    qProfiles = new QProfiles(qualityProfileDao, activeRuleDao, ruleDao, resourceDao, projectService, search, service, rules);
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
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    when(service.createActiveRule(eq(qualityProfile), eq(rule), eq(Severity.BLOCKER), any(UserSession.class)))
      .thenReturn(new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(1));

    qProfiles.activateRule(1, 10, Severity.BLOCKER);

    verify(service).createActiveRule(eq(qualityProfile), eq(rule), eq(Severity.BLOCKER), any(UserSession.class));
  }

  @Test
  public void fail_to_activate_rule_if_rule_not_found() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    QProfileRule rule = mock(QProfileRule.class);
    when(rule.id()).thenReturn(10);
    when(rules.getFromRuleId(10)).thenReturn(null);

    try {
      qProfiles.activateRule(1, 10, Severity.BLOCKER);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
    verifyZeroInteractions(service);
  }

  @Test
  public void update_severity() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);

    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(1);
    when(activeRuleDao.selectByProfileAndRule(1, 10)).thenReturn(activeRule);

    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    qProfiles.activateRule(1, 10, Severity.BLOCKER);

    verify(service).updateSeverity(eq(qualityProfile), eq(activeRule), eq(Severity.BLOCKER), any(UserSession.class));
  }

  @Test
  public void deactivate_rule() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(1);
    when(activeRuleDao.selectByProfileAndRule(1, 10)).thenReturn(activeRule);

    qProfiles.deactivateRule(1, 10);

    verify(service).deactivateRule(eq(activeRule), any(UserSession.class));
  }

  @Test
  public void fail_to_deactivate_rule_if_no_active_rule_on_profile() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);
    when(activeRuleDao.selectByProfileAndRule(1, 10)).thenReturn(null);

    try {
      qProfiles.deactivateRule(1, 10);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void update_active_rule_param() throws Exception {
    when(qualityProfileDao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));

    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setId(100).setActiveRuleId(5).setKey("max").setValue("20");
    when(activeRuleDao.selectParamByActiveRuleAndKey(50, "max")).thenReturn(activeRuleParam);

    qProfiles.updateActiveRuleParam(1, 50, "max", "20");

    verify(service).updateActiveRuleParam(eq(activeRule), eq(activeRuleParam), eq("20"), any(UserSession.class));
  }

  @Test
  public void fail_to_update_active_rule_param_if_active_rule_not_found() throws Exception {
    when(qualityProfileDao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));
    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setId(100).setActiveRuleId(5).setKey("max").setValue("20");
    when(activeRuleDao.selectParamByActiveRuleAndKey(50, "max")).thenReturn(activeRuleParam);

    when(activeRuleDao.selectById(50)).thenReturn(null);

    try {
      qProfiles.updateActiveRuleParam(1, 50, "max", "20");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
    verifyZeroInteractions(service);
  }

  @Test
  public void create_active_rule_param() throws Exception {
    when(qualityProfileDao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);
    when(activeRuleDao.selectParamByActiveRuleAndKey(50, "max")).thenReturn(null);

    qProfiles.updateActiveRuleParam(1, 50, "max", "20");

    verify(service).createActiveRuleParam(eq(activeRule), eq("max"), eq("20"), any(UserSession.class));
  }

  @Test
  public void delete_active_rule_param() throws Exception {
    when(qualityProfileDao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setId(100).setActiveRuleId(5).setKey("max").setValue("20");
    when(activeRuleDao.selectParamByActiveRuleAndKey(50, "max")).thenReturn(activeRuleParam);

    qProfiles.updateActiveRuleParam(1, 50, "max", "");

    verify(service).deleteActiveRuleParam(eq(activeRule), eq(activeRuleParam), any(UserSession.class));
  }

  @Test
  public void do_nothing_when_updating_active_rule_param_with_no_param_and_empty_value() throws Exception {
    when(qualityProfileDao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);
    when(activeRuleDao.selectParamByActiveRuleAndKey(50, "max")).thenReturn(null);

    qProfiles.updateActiveRuleParam(1, 50, "max", "");

    verifyZeroInteractions(service);
  }

  @Test
  public void create_active_rule_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    qProfiles.updateActiveRuleNote(50, "My note");

    verify(service).updateActiveRuleNote(eq(activeRule), eq("My note"), any(UserSession.class));
  }

  @Test
  public void not_update_rule_note_when_empty_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    qProfiles.updateActiveRuleNote(50, "");

    verify(service, never()).updateActiveRuleNote(eq(activeRule), anyString(), any(UserSession.class));
  }

  @Test
  public void delete_active_rule_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    qProfiles.deleteActiveRuleNote(50);

    verify(service).deleteActiveRuleNote(eq(activeRule), any(UserSession.class));
  }

  @Test
  public void create_rule_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    qProfiles.updateRuleNote(50, 10, "My note");

    verify(service).updateRuleNote(eq(activeRule), eq(rule), eq("My note"), any(UserSession.class));
  }

  @Test
  public void delete_rule_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    qProfiles.updateRuleNote(50, 10, "");

    verify(service).deleteRuleNote(eq(activeRule), eq(rule), any(UserSession.class));
  }

}
