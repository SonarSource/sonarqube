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
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.RuleRegistry;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QProfilePluginExporterTest {

  @Mock
  SqlSession session;

  @Mock
  DatabaseSessionFactory sessionFactory;

  @Mock
  DatabaseSession hibernateSession;

  @Mock
  ActiveRuleDao activeRuleDao;

  @Mock
  RuleRegistry ruleRegistry;

  List<ProfileImporter> importers = newArrayList();
  List<ProfileExporter> exporters = newArrayList();

  Integer currentId = 1;

  QProfilePluginExporter operations;

  @Before
  public void setUp() throws Exception {
    when(sessionFactory.getSession()).thenReturn(hibernateSession);

    // Associate an id when inserting an object to simulate the db id generator
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ActiveRuleDto dto = (ActiveRuleDto) args[0];
        dto.setId(currentId++);
        return null;
      }
    }).when(activeRuleDao).insert(any(ActiveRuleDto.class), any(SqlSession.class));

    operations = new QProfilePluginExporter(sessionFactory, activeRuleDao, ruleRegistry, importers, exporters);
  }

  @Test
  public void import_from_xml_plugin() throws Exception {
    RulesProfile profile = RulesProfile.create("Default", "java");
    Rule rule = Rule.create("pmd", "rule1");
    rule.createParameter("max");
    rule.setId(10);
    ActiveRule activeRule = profile.activateRule(rule, RulePriority.BLOCKER);
    activeRule.setParameter("max", "10");

    ProfileImporter importer = mock(ProfileImporter.class);
    when(importer.getKey()).thenReturn("pmd");
    when(importer.importProfile(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);
    importers.add(importer);

    operations.importXml(new QProfile().setId(1), "pmd", "<xml/>", session);

    verify(importer).importProfile(any(Reader.class), any(ValidationMessages.class));

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
  public void import_from_xml_plugin_add_infos_and_warnings() throws Exception {
    final RulesProfile profile = RulesProfile.create("Default", "java");
    Rule rule = Rule.create("pmd", "rule1");
    rule.createParameter("max");
    rule.setId(10);
    ActiveRule activeRule = profile.activateRule(rule, RulePriority.BLOCKER);
    activeRule.setParameter("max", "10");

    ProfileImporter importer = mock(ProfileImporter.class);
    when(importer.getKey()).thenReturn("pmd");
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ValidationMessages validationMessages = (ValidationMessages) args[1];
        validationMessages.addInfoText("an info message");
        validationMessages.addWarningText("a warning message");
        return profile;
      }
    }).when(importer).importProfile(any(Reader.class), any(ValidationMessages.class));
    importers.add(importer);

    QProfileResult result = operations.importXml(new QProfile().setId(1), "pmd", "<xml/>", session);;
    assertThat(result.infos()).hasSize(1);
    assertThat(result.warnings()).hasSize(1);
  }

  @Test
  public void fail_to_import_profile_from_xml_plugin_if_error() throws Exception {
    final RulesProfile profile = RulesProfile.create("Default", "java");
    Rule rule = Rule.create("pmd", "rule1");
    rule.createParameter("max");
    rule.setId(10);
    ActiveRule activeRule = profile.activateRule(rule, RulePriority.BLOCKER);
    activeRule.setParameter("max", "10");

    ProfileImporter importer = mock(ProfileImporter.class);
    when(importer.getKey()).thenReturn("pmd");
    importers.add(importer);

    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ValidationMessages validationMessages = (ValidationMessages) args[1];
        validationMessages.addErrorText("error!");
        return profile;
      }
    }).when(importer).importProfile(any(Reader.class), any(ValidationMessages.class));

    try {
      operations.importXml(new QProfile().setId(1), "pmd", "<xml/>", session);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void fail_to_import_profile_when_missing_importer() throws Exception {
    final RulesProfile profile = RulesProfile.create("Default", "java");
    Rule rule = Rule.create("pmd", "rule1");
    rule.createParameter("max");
    rule.setId(10);
    ActiveRule activeRule = profile.activateRule(rule, RulePriority.BLOCKER);
    activeRule.setParameter("max", "10");

    ProfileImporter importer = mock(ProfileImporter.class);
    when(importer.getKey()).thenReturn("pmd");
    importers.add(importer);

    when(importer.importProfile(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);

    try {
      operations.importXml(new QProfile().setId(1), "unknown", "<xml/>", session);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("No such importer : unknown");
    }
    verify(importer, never()).importProfile(any(Reader.class), any(ValidationMessages.class));
  }

  @Test
  public void export_to_plugin_xml() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getId()).thenReturn(1);
    when(hibernateSession.getSingleResult(any(Class.class), eq("id"), eq(1))).thenReturn(profile);

    ProfileExporter exporter = mock(ProfileExporter.class);
    when(exporter.getKey()).thenReturn("pmd");
    exporters.add(exporter);

    operations.exportToXml(new QProfile().setId(1), "pmd");

    verify(exporter).exportProfile(eq(profile), any(Writer.class));
  }

  @Test
  public void fail_to_export_profile_when_missing_exporter() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getId()).thenReturn(1);
    when(hibernateSession.getSingleResult(any(Class.class), eq("id"), eq(1))).thenReturn(profile);

    ProfileExporter exporter = mock(ProfileExporter.class);
    when(exporter.getKey()).thenReturn("pmd");
    exporters.add(exporter);

    try {
      operations.exportToXml(new QProfile().setId(1), "unknown");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("No such exporter : unknown");
    }

    verify(exporter, never()).exportProfile(any(RulesProfile.class), any(Writer.class));
  }

  @Test
  public void get_profile_exporter_mime_type() throws Exception {
    ProfileExporter exporter = mock(ProfileExporter.class);
    when(exporter.getKey()).thenReturn("pmd");
    when(exporter.getMimeType()).thenReturn("mime");
    exporters.add(exporter);

    assertThat(operations.getProfileExporterMimeType("pmd")).isEqualTo("mime");
  }

  @Test
  public void get_profile_exporters_for_language() throws Exception {
    // 2 exporters not declaring supported languages -> match all languages -> to be include in result
    ProfileExporter exporterWithEmptySupportedLanguagesList = mock(ProfileExporter.class);
    when(exporterWithEmptySupportedLanguagesList.getSupportedLanguages()).thenReturn(new String[]{});
    exporters.add(exporterWithEmptySupportedLanguagesList);
    exporters.add(mock(ProfileExporter.class));

    // 1 exporter supporting the java language -> to be include in result
    ProfileExporter exporterSupportingJava = mock(ProfileExporter.class);
    when(exporterSupportingJava.getSupportedLanguages()).thenReturn(new String[]{"java"});
    exporters.add(exporterSupportingJava);

    // 1 exporter supporting another language -> not to be include in result
    ProfileExporter exporterSupportingAnotherLanguage = mock(ProfileExporter.class);
    when(exporterSupportingAnotherLanguage.getSupportedLanguages()).thenReturn(new String[]{"js"});
    exporters.add(exporterSupportingAnotherLanguage);

    assertThat(operations.getProfileExportersForLanguage("java")).hasSize(3);
  }

  @Test
  public void get_profile_importers_for_language() throws Exception {
    // 2 importers not declaring supported languages -> match all languages -> to be include in result
    ProfileImporter importersWithEmptySupportedLanguagesList = mock(ProfileImporter.class);
    when(importersWithEmptySupportedLanguagesList.getSupportedLanguages()).thenReturn(new String[]{});
    importers.add(importersWithEmptySupportedLanguagesList);
    importers.add(mock(ProfileImporter.class));

    // 1 importers supporting the java language -> to be include in result
    ProfileImporter importerSupportingJava = mock(ProfileImporter.class);
    when(importerSupportingJava.getSupportedLanguages()).thenReturn(new String[]{"java"});
    importers.add(importerSupportingJava);

    // 1 importers supporting another language -> not to be include in result
    ProfileImporter importerSupportingAnotherLanguage = mock(ProfileImporter.class);
    when(importerSupportingAnotherLanguage.getSupportedLanguages()).thenReturn(new String[]{"js"});
    importers.add(importerSupportingAnotherLanguage);

    assertThat(operations.getProfileImportersForLanguage("java")).hasSize(3);
  }

}
