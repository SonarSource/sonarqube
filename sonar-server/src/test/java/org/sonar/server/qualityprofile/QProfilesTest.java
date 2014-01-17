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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.Severity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.ProfileRuleQuery;
import org.sonar.server.rule.ProfileRules;
import org.sonar.server.user.UserSession;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyInt;
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
  QProfileProjectOperations projectOperations;

  @Mock
  QProfileProjectLookup projectLookup;

  @Mock
  QProfileLookup profileLookup;

  @Mock
  QProfileOperations profileOperations;

  @Mock
  QProfileActiveRuleOperations activeRuleOperations;

  @Mock
  QProfileRuleOperations ruleOperations;

  @Mock
  QProfileBackup backup;

  @Mock
  QProfileExporter exporter;

  @Mock
  ProfileRules rules;

  QProfiles qProfiles;

  @Before
  public void setUp() throws Exception {
    qProfiles = new QProfiles(qualityProfileDao, activeRuleDao, ruleDao, resourceDao, projectOperations, projectLookup, backup, exporter,
      profileLookup, profileOperations, activeRuleOperations, ruleOperations, rules);
  }

  @Test
  public void search_profile_by_id() throws Exception {
    qProfiles.profile(1);
    verify(profileLookup).profile(1);
  }

  @Test
  public void search_profile_by_name_and_language() throws Exception {
    qProfiles.profile("Default", "java");
    verify(profileLookup).profile("Default", "java");
  }

  @Test
  public void search_profiles() throws Exception {
    qProfiles.allProfiles();
    verify(profileLookup).allProfiles();
  }

  @Test
  public void search_profiles_by_language() throws Exception {
    qProfiles.profilesByLanguage("java");
    verify(profileLookup).profiles("java");
  }

  @Test
  public void search_default_profile_by_language() throws Exception {
    qProfiles.defaultProfile("java");
    verify(profileLookup).defaultProfile("java");
  }

  @Test
  public void search_parent_profile() throws Exception {
    QProfile profile = new QProfile().setId(1).setParent("Parent").setLanguage("java");
    qProfiles.parent(profile);
    verify(profileLookup).parent(profile);
  }

  @Test
  public void search_children() throws Exception {
    QProfile profile = new QProfile();
    qProfiles.children(profile);
    verify(profileLookup).children(profile);
  }

  @Test
  public void search_ancestors() throws Exception {
    QProfile profile = new QProfile();
    qProfiles.ancestors(profile);
    verify(profileLookup).ancestors(profile);
  }

  @Test
  public void count_children() throws Exception {
    QProfile profile = new QProfile();
    qProfiles.countChildren(profile);
    verify(profileLookup).countChildren(profile);
  }

  @Test
  public void create_new_profile() throws Exception {
    Map<String, String> xmlProfilesByPlugin = newHashMap();
    qProfiles.newProfile("Default", "java", xmlProfilesByPlugin);
    verify(profileOperations).newProfile(eq("Default"), eq("java"), eq(xmlProfilesByPlugin), any(UserSession.class));
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
  public void rename_profile() throws Exception {
    qProfiles.renameProfile(1, "Default profile");
    verify(profileOperations).renameProfile(eq(1), eq("Default profile"), any(UserSession.class));
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
  public void is_deletable() throws Exception {
    qProfiles.isDeletable(new QProfile());
    verify(profileLookup).isDeletable(any(QProfile.class));
  }

  @Test
  public void delete_profile() throws Exception {
    qProfiles.deleteProfile(1);
    verify(profileOperations).deleteProfile(eq(1), any(UserSession.class));
  }

  @Test
  public void update_default_profile() throws Exception {
    qProfiles.setDefaultProfile(1);
    verify(profileOperations).setDefaultProfile(eq(1), any(UserSession.class));
  }

  @Test
  public void update_parent_profile() throws Exception {
    qProfiles.updateParentProfile(1, 2);
    verify(profileOperations).updateParentProfile(eq(1), eq(2), any(UserSession.class));
  }

  @Test
  public void export_profile_to_xml_plugin() throws Exception {
    QProfile profile = new QProfile().setId(1);
    qProfiles.exportProfileToXml(profile, "pmd");
    verify(exporter).exportToXml(profile, "pmd");
  }

  @Test
  public void get_profile_exporter_mime_type() throws Exception {
    qProfiles.getProfileExporterMimeType("pmd");
    verify(exporter).getProfileExporterMimeType("pmd");
  }

  @Test
  public void projects() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    qProfiles.projects(1);
    verify(projectLookup).projects(qualityProfile);
  }

  @Test
  public void count_projects() throws Exception {
    QProfile profile = new QProfile();
    qProfiles.countProjects(profile);
    verify(projectLookup).countProjects(profile);
  }

  @Test
  public void get_profiles_from_project_id() throws Exception {
    qProfiles.profiles(1);
    verify(projectLookup).profiles(1);
  }

  @Test
  public void add_project() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    when(resourceDao.findById(10L)).thenReturn(project);

    qProfiles.addProject(1, 10L);
    verify(projectOperations).addProject(eq(qualityProfile), eq(project), any(UserSession.class));
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
    verifyZeroInteractions(projectOperations);
  }

  @Test
  public void remove_project_by_quality_profile_id() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);
    when(resourceDao.findById(10L)).thenReturn(project);

    qProfiles.removeProject(1, 10L);
    verify(projectOperations).removeProject(eq(qualityProfile), eq(project), any(UserSession.class));
  }

  @Test
  public void remove_project_by_language() throws Exception {
    ComponentDto project = new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube");
    when(resourceDao.findById(10L)).thenReturn(project);

    qProfiles.removeProjectByLanguage("java", 10L);
    verify(projectOperations).removeProject(eq("java"), eq(project), any(UserSession.class));
  }

  @Test
  public void remove_all_projects() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");
    when(qualityProfileDao.selectById(1)).thenReturn(qualityProfile);

    qProfiles.removeAllProjects(1);
    verify(projectOperations).removeAllProjects(eq(qualityProfile), any(UserSession.class));
  }

  @Test
  public void parent_active_rule() throws Exception {
    QProfileRule rule = mock(QProfileRule.class);
    when(rule.id()).thenReturn(10);
    when(rule.activeRuleParentId()).thenReturn(6);

    qProfiles.parentProfileRule(rule);
    verify(rules).findByActiveRuleId(6);
  }

  @Test
  public void parent_active_rule_return_null_when_no_parent() throws Exception {
    QProfileRule rule = mock(QProfileRule.class);
    when(rule.id()).thenReturn(10);
    when(rule.activeRuleParentId()).thenReturn(null);

    assertThat(qProfiles.parentProfileRule(rule)).isNull();
    verify(rules, never()).findByActiveRuleId(anyInt());
  }

  @Test
  public void find_by_rule() throws Exception {
    qProfiles.findByRule(1);
    verify(rules).findByRuleId(1);
  }

  @Test
  public void find_by_active_rule() throws Exception {
    qProfiles.findByActiveRuleId(1);
    verify(rules).findByActiveRuleId(1);
  }

  @Test
  public void find_by_profile_an_rule() throws Exception {
    qProfiles.findByProfileAndRule(1, 2);
    verify(rules).findByProfileIdAndRuleId(1, 2);
  }

  @Test
  public void search_active_rules() throws Exception {
    ProfileRuleQuery query = ProfileRuleQuery.create(42);
    Paging paging = Paging.create(20, 1);
    qProfiles.searchProfileRules(query, paging);
    verify(rules).search(query, paging);
  }

  @Test
  public void count_profile_rules() throws Exception {
    ProfileRuleQuery query = ProfileRuleQuery.create(1);
    qProfiles.countProfileRules(query);
    verify(rules).countProfileRules(query);
  }

  @Test
  public void count_profile_rules_from_profile() throws Exception {
    QProfile profile = new QProfile().setId(1);
    qProfiles.countProfileRules(profile);
    verify(rules).countProfileRules(any(ProfileRuleQuery.class));
  }

  @Test
  public void count_overriding_profile_rules() throws Exception {
    QProfile profile = new QProfile().setId(1);
    qProfiles.countOverridingProfileRules(profile);
    verify(rules).countProfileRules(any(ProfileRuleQuery.class));
  }

  @Test
  public void search_inactive_rules() throws Exception {
    final int profileId = 42;
    ProfileRuleQuery query = ProfileRuleQuery.create(profileId);
    Paging paging = Paging.create(20, 1);
    ProfileRules.QProfileRuleResult result = mock(ProfileRules.QProfileRuleResult.class);
    when(rules.searchInactives(query, paging)).thenReturn(result);
    assertThat(qProfiles.searchInactiveProfileRules(query, paging)).isEqualTo(result);
  }

  @Test
  public void count_inactive_rules() throws Exception {
    ProfileRuleQuery query = ProfileRuleQuery.create(1);
    qProfiles.countInactiveProfileRules(query);
    verify(rules).countInactiveProfileRules(query);
  }

  @Test
  public void activate_rule() throws Exception {
    qProfiles.activateRule(1, 10, Severity.BLOCKER);
    verify(activeRuleOperations).activateRule(eq(1), eq(10), eq(Severity.BLOCKER), any(UserSession.class));
  }

  @Test
  public void bulk_activate_rule() throws Exception {
    ProfileRuleQuery query = ProfileRuleQuery.create(1);
    when(rules.searchInactiveProfileRuleIds(query)).thenReturn(newArrayList(10));
    qProfiles.bulkActivateRule(query);
    verify(activeRuleOperations).activateRules(eq(1), eq(newArrayList(10)), any(UserSession.class));
  }

  @Test
  public void deactivate_rule() throws Exception {
    qProfiles.deactivateRule(1, 10);
    verify(activeRuleOperations).deactivateRule(eq(1), eq(10), any(UserSession.class));
  }

  @Test
  public void bulk_deactivate_rule() throws Exception {
    ProfileRuleQuery query = ProfileRuleQuery.create(1);
    when(rules.searchProfileRuleIds(query)).thenReturn(newArrayList(10));
    qProfiles.bulkDeactivateRule(query);
    verify(activeRuleOperations).deactivateRules(eq(1), eq(newArrayList(10)), any(UserSession.class));
  }

  @Test
  public void update_active_rule_param() throws Exception {
    qProfiles.updateActiveRuleParam(50, "max", "20");
    verify(activeRuleOperations).updateActiveRuleParam(eq(50), eq("max"), eq("20"), any(UserSession.class));
  }

  @Test
  public void revert_active_rule() throws Exception {
    qProfiles.revertActiveRule(50);
    verify(activeRuleOperations).revertActiveRule(eq(50), any(UserSession.class));
  }

  @Test
  public void create_active_rule_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    qProfiles.updateActiveRuleNote(50, "My note");

    verify(activeRuleOperations).updateActiveRuleNote(eq(activeRule), eq("My note"), any(UserSession.class));
  }

  @Test
  public void not_update_rule_note_when_empty_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    qProfiles.updateActiveRuleNote(50, "");

    verify(activeRuleOperations, never()).updateActiveRuleNote(eq(activeRule), anyString(), any(UserSession.class));
  }

  @Test
  public void delete_active_rule_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    qProfiles.deleteActiveRuleNote(50);

    verify(activeRuleOperations).deleteActiveRuleNote(eq(activeRule), any(UserSession.class));
  }

  @Test
  public void create_rule_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    qProfiles.updateRuleNote(50, 10, "My note");

    verify(ruleOperations).updateRuleNote(eq(rule), eq("My note"), any(UserSession.class));
  }

  @Test
  public void delete_rule_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(50);
    when(activeRuleDao.selectById(50)).thenReturn(activeRule);

    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    qProfiles.updateRuleNote(50, 10, "");

    verify(ruleOperations).deleteRuleNote(eq(rule), any(UserSession.class));
  }

  @Test
  public void get_rule_from_id() throws Exception {
    qProfiles.rule(10);

    verify(rules).findByRuleId(10);
  }

  @Test
  public void create_new_rule() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    RuleDto newRule = new RuleDto().setId(11);
    Map<String, String> paramsByKey = ImmutableMap.of("max", "20");
    when(ruleOperations.createRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("My note"), eq(paramsByKey), any(UserSession.class))).thenReturn(newRule);

    qProfiles.createRule(10, "Rule name", Severity.MAJOR, "My note", paramsByKey);

    verify(ruleOperations).createRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("My note"), eq(paramsByKey), any(UserSession.class));
    verify(rules).findByRuleId(11);
  }

  @Test
  public void fail_to_create_new_rule_on_empty_parameters() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    RuleDto newRule = new RuleDto().setId(11);
    Map<String, String> paramsByKey = ImmutableMap.of("max", "20");
    when(ruleOperations.createRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("My note"), eq(paramsByKey), any(UserSession.class))).thenReturn(newRule);

    try {
      qProfiles.createRule(10, "", "", "", paramsByKey);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      assertThat(((BadRequestException) e).errors()).hasSize(3);
    }
    verifyZeroInteractions(profileOperations);
    verifyZeroInteractions(rules);
  }

  @Test
  public void fail_to_create_new_rule_when_rule_name_already_exists() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");
    when(ruleDao.selectById(10)).thenReturn(rule);

    when(ruleDao.selectByName("Rule name")).thenReturn(new RuleDto());

    RuleDto newRule = new RuleDto().setId(11);
    Map<String, String> paramsByKey = ImmutableMap.of("max", "20");
    when(ruleOperations.createRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("My description"), eq(paramsByKey), any(UserSession.class))).thenReturn(newRule);

    try {
      qProfiles.createRule(10, "Rule name", Severity.MAJOR, "My description", paramsByKey);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      assertThat(((BadRequestException) e).errors()).hasSize(1);
    }
    verifyZeroInteractions(profileOperations);
    verifyZeroInteractions(rules);
  }

  @Test
  public void update_rule() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setParentId(10);
    when(ruleDao.selectById(11)).thenReturn(rule);
    when(ruleDao.selectByName("Updated name")).thenReturn(null);

    Map<String, String> paramsByKey = ImmutableMap.of("max", "21");

    qProfiles.updateRule(11, "Updated name", Severity.MAJOR, "Updated description", paramsByKey);

    verify(ruleOperations).updateRule(eq(rule), eq("Updated name"), eq(Severity.MAJOR), eq("Updated description"), eq(paramsByKey), any(UserSession.class));
    verify(rules).findByRuleId(11);
  }

  @Test
  public void update_rule_with_same_name() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setParentId(10);
    when(ruleDao.selectById(11)).thenReturn(rule);
    when(ruleDao.selectByName("Rule name")).thenReturn(rule);

    Map<String, String> paramsByKey = ImmutableMap.of("max", "21");

    qProfiles.updateRule(11, "Rule name", Severity.MAJOR, "Updated description", paramsByKey);

    verify(ruleOperations).updateRule(eq(rule), eq("Rule name"), eq(Severity.MAJOR), eq("Updated description"), eq(paramsByKey), any(UserSession.class));
    verify(rules).findByRuleId(11);
  }

  @Test
  public void fail_to_update_rule_when_no_parent() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254");
    when(ruleDao.selectById(11)).thenReturn(rule);
    when(ruleDao.selectByName("Rule name")).thenReturn(rule);

    Map<String, String> paramsByKey = ImmutableMap.of("max", "21");

    try {
      qProfiles.updateRule(11, "Rule name", Severity.MAJOR, "Updated description", paramsByKey);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
    verifyZeroInteractions(profileOperations);
    verifyZeroInteractions(rules);
  }

  @Test
  public void delete_rule() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setParentId(10);
    when(ruleDao.selectById(11)).thenReturn(rule);

    qProfiles.deleteRule(11);

    verify(ruleOperations).deleteRule(eq(rule), any(UserSession.class));
  }

  @Test
  public void count_active_rules() throws Exception {
    QProfileRule rule = mock(QProfileRule.class);
    when(rule.id()).thenReturn(10);

    when(activeRuleDao.selectByRuleId(10)).thenReturn(newArrayList(new ActiveRuleDto().setId(50), new ActiveRuleDto().setId(51)));

    assertThat(qProfiles.countActiveRules(rule)).isEqualTo(2);
  }

}
