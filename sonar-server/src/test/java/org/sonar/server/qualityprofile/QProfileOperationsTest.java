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
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.core.resource.ResourceDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.MockUserSession;

import java.io.Reader;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QProfileOperationsTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  QualityProfileDao dao;

  @Mock
  ActiveRuleDao activeRuleDao;

  @Mock
  ResourceDao resourceDao;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  PreviewCache dryRunCache;

  List<ProfileExporter> exporters = newArrayList();

  List<ProfileImporter> importers = newArrayList();

  QProfileOperations operations;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession()).thenReturn(session);
    operations = new QProfileOperations(myBatis, dao, activeRuleDao, resourceDao, propertiesDao, exporters, importers, dryRunCache);
  }

  @Test
  public void create_profile() throws Exception {
    NewProfileResult result = operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));
    assertThat(result.profile().name()).isEqualTo("Default");
    assertThat(result.profile().language()).isEqualTo("java");

    verify(dao).insert(any(QualityProfileDto.class), eq(session));

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(dao).insert(profileArgument.capture(), eq(session));
    assertThat(profileArgument.getValue().getName()).isEqualTo("Default");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");
    assertThat(profileArgument.getValue().getVersion()).isEqualTo(1);
    assertThat(profileArgument.getValue().isUsed()).isFalse();
  }

  @Test
  public void fail_to_create_profile_without_profile_admin_permission() throws Exception {
    try {
      operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), MockUserSession.create());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyNoMoreInteractions(dao);
    verify(session, never()).commit();
  }

  @Test
  public void fail_to_create_profile_without_name() throws Exception {
    try {
      operations.newProfile("", "java", Maps.<String, String>newHashMap(), MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
    verify(session, never()).commit();
  }

  @Test
  public void fail_to_create_profile_if_already_exists() throws Exception {
    try {
      when(dao.selectByNameAndLanguage(anyString(), anyString())).thenReturn(new QualityProfileDto());
      operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
    verify(session, never()).commit();
  }

  @Test
  public void create_profile_from_xml_plugin() throws Exception {
    RulesProfile profile = RulesProfile.create("Default", "java");
    Rule rule = Rule.create("pmd", "rule1");
    rule.createParameter("paramKey");
    rule.setId(10);
    ActiveRule activeRule = profile.activateRule(rule, RulePriority.BLOCKER);
    activeRule.setParameter("paramKey", "paramValue");

    Map<String, String> xmlProfilesByPlugin = newHashMap();
    xmlProfilesByPlugin.put("pmd", "<xml/>");
    ProfileImporter importer = mock(ProfileImporter.class);
    when(importer.getKey()).thenReturn("pmd");
    when(importer.importProfile(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);
    importers.add(importer);

    operations.newProfile("Default", "java", xmlProfilesByPlugin, MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));
    verify(session).commit();

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(dao).insert(profileArgument.capture(), eq(session));
    assertThat(profileArgument.getValue().getName()).isEqualTo("Default");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");

    ArgumentCaptor<ActiveRuleDto> activeRuleArgument = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).insert(activeRuleArgument.capture(), eq(session));
    assertThat(activeRuleArgument.getValue().getRulId()).isEqualTo(10);
    assertThat(activeRuleArgument.getValue().getSeverity()).isEqualTo(5);

    ArgumentCaptor<ActiveRuleParamDto> activeRuleParamArgument = ArgumentCaptor.forClass(ActiveRuleParamDto.class);
    verify(activeRuleDao).insert(activeRuleParamArgument.capture(), eq(session));
    assertThat(activeRuleParamArgument.getValue().getValue()).isEqualTo("paramValue");
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

      operations.newProfile("Default", "java", xmlProfilesByPlugin, MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
    verify(session, never()).commit();
  }

  @Test
  public void rename_profile() throws Exception {
    QualityProfileDto dto = new QualityProfileDto().setId(1).setName("Default").setLanguage("java");
    when(dao.selectById(1)).thenReturn(dto);
    operations.renameProfile(1, "Default profile", MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(dao).update(profileArgument.capture());

    assertThat(profileArgument.getValue().getName()).isEqualTo("Default profile");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");
  }

  @Test
  public void fail_to_rename_profile_on_unknown_profile() throws Exception {
    try {
      operations.renameProfile(1, "Default profile", MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
    verify(dao, never()).update(any(QualityProfileDto.class));
  }

  @Test
  public void fail_to_rename_profile_when_missing_new_name() throws Exception {
    try {
      operations.renameProfile(1, "", MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
    verify(dao, never()).update(any(QualityProfileDto.class));
  }

  @Test
  public void fail_to_rename_profile_if_already_exists() throws Exception {
    try {
      when(dao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
      when(dao.selectByNameAndLanguage(eq("New Default"), anyString())).thenReturn(new QualityProfileDto().setName("New Default").setLanguage("java"));
      operations.renameProfile(1, "New Default", MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
    verify(session, never()).commit();
  }

  @Test
  public void update_default_profile() throws Exception {
    when(dao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));

    operations.updateDefaultProfile(1, MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));

    ArgumentCaptor<PropertyDto> argumentCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).setProperty(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getKey()).isEqualTo("sonar.profile.java");
    assertThat(argumentCaptor.getValue().getValue()).isEqualTo("My profile");
  }

  @Test
  public void update_default_profile_from_name_and_language() throws Exception {
    when(dao.selectByNameAndLanguage("My profile", "java")).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));
    when(dao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));

    operations.updateDefaultProfile("My profile", "java", MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));

    ArgumentCaptor<PropertyDto> argumentCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).setProperty(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getKey()).isEqualTo("sonar.profile.java");
    assertThat(argumentCaptor.getValue().getValue()).isEqualTo("My profile");
  }

  @Test
  public void search_projects() throws Exception {
    when(dao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));
    when(dao.selectProjects("sonar.profile.java", "My profile")).thenReturn(newArrayList(new ComponentDto().setId(1L).setKey("org.codehaus.sonar:sonar").setName("SonarQube")));

    QProfileProjects result = operations.projects(1);
    assertThat(result.profile()).isNotNull();
    assertThat(result.projects()).hasSize(1);
  }

  @Test
  public void add_project() throws Exception {
    when(dao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));
    when(resourceDao.findById(10L)).thenReturn(new ComponentDto().setId(10L).setKey("org.codehaus.sonar:sonar").setName("SonarQube"));

    operations.addProject(1, 10L, MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));

    ArgumentCaptor<PropertyDto> argumentCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).setProperty(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getKey()).isEqualTo("sonar.profile.java");
    assertThat(argumentCaptor.getValue().getValue()).isEqualTo("My profile");
    assertThat(argumentCaptor.getValue().getResourceId()).isEqualTo(10);
  }

  @Test
  public void fail_to_add_project_if_project_not_found() throws Exception {
    try {
      when(dao.selectById(1)).thenReturn(new QualityProfileDto().setId(1).setName("My profile").setLanguage("java"));
      when(resourceDao.findById(10L)).thenReturn(null);

      operations.addProject(1, 10L, MockUserSession.create().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
    verify(propertiesDao, never()).setProperty(any(PropertyDto.class));
  }
}
