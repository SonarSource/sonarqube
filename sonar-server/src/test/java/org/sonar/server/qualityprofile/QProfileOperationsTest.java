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
import com.google.common.collect.Multimap;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.io.Reader;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QProfileOperationsTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  QualityProfileDao qualityProfileDao;

  @Mock
  ActiveRuleDao activeRuleDao;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  PreviewCache dryRunCache;

  @Mock
  RuleRegistry ruleRegistry;

  @Mock
  ProfilesManager profilesManager;

  List<ProfileImporter> importers = newArrayList();

  Integer currentId = 1;

  UserSession authorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  UserSession unauthorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas");

  QProfileOperations operations;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession()).thenReturn(session);

    // Associate an id when inserting an object to simulate the db id generator
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ActiveRuleDto dto = (ActiveRuleDto) args[0];
        dto.setId(currentId++);
        return null;
      }
    }).when(activeRuleDao).insert(any(ActiveRuleDto.class), any(SqlSession.class));

    operations = new QProfileOperations(myBatis, qualityProfileDao, activeRuleDao, propertiesDao, importers, dryRunCache, ruleRegistry, profilesManager);
  }

  @Test
  public void create_profile() throws Exception {
    QProfileOperations.NewProfileResult result = operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), authorizedUserSession);
    assertThat(result.profile().name()).isEqualTo("Default");
    assertThat(result.profile().language()).isEqualTo("java");

    verify(qualityProfileDao).insert(any(QualityProfileDto.class), eq(session));

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(qualityProfileDao).insert(profileArgument.capture(), eq(session));
    assertThat(profileArgument.getValue().getName()).isEqualTo("Default");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");
    assertThat(profileArgument.getValue().getVersion()).isEqualTo(1);
    assertThat(profileArgument.getValue().isUsed()).isFalse();
  }

  @Test
  public void fail_to_create_profile_without_profile_admin_permission() throws Exception {
    try {
      operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), unauthorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyNoMoreInteractions(qualityProfileDao);
    verify(session, never()).commit();
  }

  @Test
  public void fail_to_create_profile_if_already_exists() throws Exception {
    when(qualityProfileDao.selectByNameAndLanguage(anyString(), anyString(), eq(session))).thenReturn(new QualityProfileDto());
    try {
      operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void create_profile_from_xml_plugin() throws Exception {
    RulesProfile profile = RulesProfile.create("Default", "java");
    Rule rule = Rule.create("pmd", "rule1");
    rule.createParameter("max");
    rule.setId(10);
    ActiveRule activeRule = profile.activateRule(rule, RulePriority.BLOCKER);
    activeRule.setParameter("max", "10");

    Map<String, String> xmlProfilesByPlugin = newHashMap();
    xmlProfilesByPlugin.put("pmd", "<xml/>");
    ProfileImporter importer = mock(ProfileImporter.class);
    when(importer.getKey()).thenReturn("pmd");
    when(importer.importProfile(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);
    importers.add(importer);

    operations.newProfile("Default", "java", xmlProfilesByPlugin, authorizedUserSession);
    verify(session).commit();

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(qualityProfileDao).insert(profileArgument.capture(), eq(session));
    assertThat(profileArgument.getValue().getName()).isEqualTo("Default");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");

    ArgumentCaptor<ActiveRuleDto> activeRuleArgument = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).insert(activeRuleArgument.capture(), eq(session));
    assertThat(activeRuleArgument.getValue().getRulId()).isEqualTo(10);
    assertThat(activeRuleArgument.getValue().getSeverity()).isEqualTo(4);

    ArgumentCaptor<ActiveRuleParamDto> activeRuleParamArgument = ArgumentCaptor.forClass(ActiveRuleParamDto.class);
    verify(activeRuleDao).insert(activeRuleParamArgument.capture(), eq(session));
    assertThat(activeRuleParamArgument.getValue().getKey()).isEqualTo("max");
    assertThat(activeRuleParamArgument.getValue().getValue()).isEqualTo("10");

    verify(ruleRegistry).bulkIndexActiveRules(anyListOf(ActiveRuleDto.class), any(Multimap.class));
  }

  @Test
  public void fail_to_create_profile_from_xml_plugin_if_error() throws Exception {
    try {
      Map<String, String> xmlProfilesByPlugin = newHashMap();
      xmlProfilesByPlugin.put("pmd", "<xml/>");
      ProfileImporter importer = mock(ProfileImporter.class);
      when(importer.getKey()).thenReturn("pmd");
      importers.add(importer);

      doAnswer(new Answer() {
        public Object answer(InvocationOnMock invocation) {
          Object[] args = invocation.getArguments();
          ValidationMessages validationMessages = (ValidationMessages) args[1];
          validationMessages.addErrorText("error!");
          return null;
        }
      }).when(importer).importProfile(any(Reader.class), any(ValidationMessages.class));

      operations.newProfile("Default", "java", xmlProfilesByPlugin, authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
    verify(session, never()).commit();
  }

  @Test
  public void rename_profile() throws Exception {
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));

    operations.renameProfile(1, "Default profile", authorizedUserSession);

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(qualityProfileDao).update(profileArgument.capture(), eq(session));

    assertThat(profileArgument.getValue().getName()).isEqualTo("Default profile");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");
  }

  @Test
  public void fail_to_rename_profile_if_not_exists() throws Exception {
    when(qualityProfileDao.selectById(1, session)).thenReturn(null);
    try {
      operations.renameProfile(1, "New Default", authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void fail_to_rename_profile_if_already_exists() throws Exception {
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(qualityProfileDao.selectByNameAndLanguage(anyString(), anyString(), eq(session))).thenReturn(new QualityProfileDto());
    try {
      operations.renameProfile(1, "New Default", authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void update_default_profile() throws Exception {
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("My profile").setLanguage("java");

    operations.setDefaultProfile(qualityProfile, authorizedUserSession);

    ArgumentCaptor<PropertyDto> argumentCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).setProperty(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getKey()).isEqualTo("sonar.profile.java");
    assertThat(argumentCaptor.getValue().getValue()).isEqualTo("My profile");
  }

  @Test
  public void update_parent_profile() {
    QualityProfileDto oldParent = new QualityProfileDto().setId(2).setName("Old Parent").setLanguage("java");
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Child").setLanguage("java").setParent("Old Parent"));
    when(qualityProfileDao.selectById(3, session)).thenReturn(new QualityProfileDto().setId(3).setName("Parent").setLanguage("java"));

    when(qualityProfileDao.selectParent(2, session)).thenReturn(oldParent);
    when(profilesManager.profileParentChanged(anyInt(), anyString(), anyString())).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.updateParentProfile(1, 3, authorizedUserSession);
    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(qualityProfileDao).update(profileArgument.capture(), eq(session));
    assertThat(profileArgument.getValue().getParent()).isEqualTo("Parent");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");

    verify(profilesManager).profileParentChanged(1, "Parent", "Nicolas");
    verify(ruleRegistry).deleteActiveRules(anyListOf(Integer.class));
    verify(ruleRegistry).bulkIndexActiveRules(anyListOf(Integer.class), eq(session));
  }

  @Test
  public void set_parent_profile() {
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Child").setLanguage("java").setParent(null));
    when(qualityProfileDao.selectById(2, session)).thenReturn(new QualityProfileDto().setId(2).setName("Parent").setLanguage("java"));

    when(profilesManager.profileParentChanged(anyInt(), anyString(), anyString())).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.updateParentProfile(1, 2, authorizedUserSession);

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(qualityProfileDao).update(profileArgument.capture(), eq(session));
    assertThat(profileArgument.getValue().getParent()).isEqualTo("Parent");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");

    verify(profilesManager).profileParentChanged(1, "Parent", "Nicolas");
    verify(ruleRegistry).deleteActiveRules(anyListOf(Integer.class));
    verify(ruleRegistry).bulkIndexActiveRules(anyListOf(Integer.class), eq(session));
  }

  @Test
  public void remove_parent_profile() {
    QualityProfileDto parent = new QualityProfileDto().setId(2).setName("Old Parent").setLanguage("java");
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Child").setLanguage("java").setParent("Old Parent"));

    when(qualityProfileDao.selectParent(2, session)).thenReturn(parent);
    when(profilesManager.profileParentChanged(anyInt(), anyString(), anyString())).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.updateParentProfile(1, null, authorizedUserSession);

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(qualityProfileDao).update(profileArgument.capture(), eq(session));
    assertThat(profileArgument.getValue().getParent()).isNull();
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");

    verify(profilesManager).profileParentChanged(1, null, "Nicolas");
    verify(ruleRegistry).deleteActiveRules(anyListOf(Integer.class));
    verify(ruleRegistry).bulkIndexActiveRules(anyListOf(Integer.class), eq(session));
  }

  @Test
  public void fail_to_update_parent_if_not_exists() throws Exception {
    when(qualityProfileDao.selectById(1, session)).thenReturn(null);
    when(qualityProfileDao.selectById(3, session)).thenReturn(new QualityProfileDto().setId(3).setName("Parent").setLanguage("java"));

    try {
      operations.updateParentProfile(1, 3, authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void fail_to_update_parent_on_cycle() {
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Child").setLanguage("java").setParent("parent"));
    when(qualityProfileDao.selectById(2, session)).thenReturn(new QualityProfileDto().setId(2).setName("Parent").setLanguage("java"));

    QualityProfileDto parent = new QualityProfileDto().setId(2).setName("Parent").setLanguage("java");
    when(qualityProfileDao.selectParent(1, session)).thenReturn(parent);
    when(qualityProfileDao.selectParent(2, session)).thenReturn(null);
    try {
      operations.updateParentProfile(2, 1, authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Please do not select a child profile as parent.");
    }
  }

  @Test
  public void detect_cycle() {
    QualityProfileDto level1 = new QualityProfileDto().setId(1).setName("level1").setLanguage("java");
    QualityProfileDto level2 = new QualityProfileDto().setId(2).setName("level2").setLanguage("java").setParent("level1");
    QualityProfileDto level3 = new QualityProfileDto().setId(3).setName("level3").setLanguage("java").setParent("level2");

    when(qualityProfileDao.selectParent(1, session)).thenReturn(null);
    when(qualityProfileDao.selectParent(2, session)).thenReturn(level1);
    when(qualityProfileDao.selectParent(3, session)).thenReturn(level2);

    assertThat(operations.isCycle(level3, level1, session)).isFalse();
    assertThat(operations.isCycle(level1, level1, session)).isTrue();
    assertThat(operations.isCycle(level1, level1, session)).isTrue();
    assertThat(operations.isCycle(level1, level3, session)).isTrue();
    assertThat(operations.isCycle(level1, level2, session)).isTrue();
    assertThat(operations.isCycle(level2, level3, session)).isTrue();
  }

}
