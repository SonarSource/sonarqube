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
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.core.rule.RuleDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
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
  RuleDao ruleDao;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  PreviewCache dryRunCache;

  @Mock
  RuleRegistry ruleRegistry;

  @Mock
  System2 system;

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

    operations = new QProfileOperations(myBatis, qualityProfileDao, activeRuleDao, ruleDao, propertiesDao, importers, dryRunCache, ruleRegistry, system);
  }

  @Test
  public void create_profile() throws Exception {
    NewProfileResult result = operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), authorizedUserSession);
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
    QualityProfileDto qualityProfile = new QualityProfileDto().setId(1).setName("Default").setLanguage("java");
    operations.renameProfile(qualityProfile, "Default profile", authorizedUserSession);

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(qualityProfileDao).update(profileArgument.capture());

    assertThat(profileArgument.getValue().getName()).isEqualTo("Default profile");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");
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

}
