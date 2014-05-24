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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.profiles.XMLProfileSerializer;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QProfileBackupTest {

  @Mock
  DatabaseSessionFactory sessionFactory;

  @Mock
  DatabaseSession hibernateSession;

  @Mock
  MyBatis myBatis;

  @Mock
  DbSession session;

  @Mock
  XMLProfileParser xmlProfileParser;

  @Mock
  XMLProfileSerializer xmlProfileSerializer;

  @Mock
  QProfileLookup qProfileLookup;

  @Mock
  QProfileOperations qProfileOperations;

  @Mock
  QProfileActiveRuleOperations qProfileActiveRuleOperations;

  @Mock
  RuleDao ruleDao;

  DefaultProfilesCache defaultProfilesCache = new DefaultProfilesCache();

  @Mock
  PreviewCache dryRunCache;

  List<ProfileDefinition> definitions;

  QProfileBackup backup;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession(false)).thenReturn(session);
    when(sessionFactory.getSession()).thenReturn(hibernateSession);

    definitions = newArrayList();

    backup = new QProfileBackup(sessionFactory, xmlProfileParser, xmlProfileSerializer, myBatis, qProfileLookup, qProfileOperations, qProfileActiveRuleOperations, ruleDao,
      definitions, defaultProfilesCache, dryRunCache);

    MockUserSession.set().setLogin("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  @Test
  public void backup() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getId()).thenReturn(1);
    when(hibernateSession.getSingleResult(any(Class.class), eq("id"), eq(1))).thenReturn(profile);

    backup.backupProfile(new QProfile().setId(1));

    verify(xmlProfileSerializer).write(eq(profile), any(Writer.class));
  }

  @Test
  public void restore() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);
    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    QProfileResult result = backup.restore("<xml/>", false);

    assertThat(result.profile()).isNotNull();
    verify(hibernateSession).saveWithoutFlush(profile);
    verify(dryRunCache).reportGlobalModification(session);
    verify(session).commit();
  }

  @Test
  public void fail_to_restore_without_profile_admin_permission() throws Exception {
    try {
      MockUserSession.set().setLogin("nicolas").setName("Nicolas");
      backup.restore("<xml/>", false);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verify(hibernateSession, never()).saveWithoutFlush(any(RulesProfile.class));
    verifyZeroInteractions(dryRunCache);
  }

  @Test
  public void fail_to_restore_if_profile_already_exist() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);
    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(RulesProfile.create("Default", "java"));
    when(qProfileLookup.profile(anyInt())).thenReturn(new QProfile().setId(1));

    try {
      backup.restore("<xml/>", false);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("The profile [name=Default,language=java] already exists. Please delete it before restoring.");
    }

    verify(hibernateSession, never()).saveWithoutFlush(any(RulesProfile.class));
    verifyZeroInteractions(dryRunCache);
  }

  @Test
  public void restore_should_delete_existing_profile() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);

    RulesProfile existingProfile = mock(RulesProfile.class);
    when(existingProfile.getId()).thenReturn(1);
    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(existingProfile);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    QProfileResult result = backup.restore("<xml/>", true);

    assertThat(result.profile()).isNotNull();
    verify(hibernateSession).removeWithoutFlush(eq(existingProfile));
    verify(dryRunCache).reportGlobalModification(session);
    verify(session).commit();
  }

  @Test
  public void restore_should_add_warnings_and_infos_from_xml_parsing() throws Exception {
    final RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ValidationMessages validationMessages = (ValidationMessages) args[1];
        validationMessages.addInfoText("an info message");
        validationMessages.addWarningText("a warning message");
        return profile;
      }
    }).when(xmlProfileParser).parse(any(Reader.class), any(ValidationMessages.class));

    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    QProfileResult result = backup.restore("<xml/>", true);

    assertThat(result.profile()).isNotNull();
    assertThat(result.warnings()).isNotEmpty();
    assertThat(result.infos()).isNotEmpty();
  }

  @Test
  public void restore_should_fail_if_errors_when_parsing_xml() throws Exception {
    final RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ValidationMessages validationMessages = (ValidationMessages) args[1];
        validationMessages.addErrorText("error!");
        return profile;
      }
    }).when(xmlProfileParser).parse(any(Reader.class), any(ValidationMessages.class));

    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    try {
      backup.restore("<xml/>", false);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Fail to restore profile");
      BadRequestException badRequestException = (BadRequestException) e;
      assertThat(badRequestException.errors()).hasSize(1);
      assertThat(badRequestException.errors().get(0).text()).isEqualTo("error!");
    }

    verify(hibernateSession, never()).saveWithoutFlush(any(RulesProfile.class));
    verifyZeroInteractions(dryRunCache);
  }

  @Test
  public void do_not_restore_if_xml_is_empty() throws Exception {
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(null);
    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    QProfileResult result = backup.restore("<xml/>", false);

    assertThat(result.profile()).isNull();
    verify(hibernateSession, never()).saveWithoutFlush(any(RulesProfile.class));
    verifyZeroInteractions(dryRunCache);
  }

  @Test
  public void do_not_restore_if_new_profile_is_null_after_import() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);

    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(null);

    try {
      backup.restore("<xml/>", false);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Restore of the profile has failed.");
    }

    verifyZeroInteractions(dryRunCache);
  }

  @Test
  public void recreate_built_in_profiles_from_language() throws Exception {
    String name = "Default";
    String language = "java";

    RulesProfile profile = RulesProfile.create(name, language);
    Rule rule = Rule.create("pmd", "rule");
    rule.createParameter("max");
    ActiveRule activeRule = profile.activateRule(rule, RulePriority.BLOCKER);
    activeRule.setParameter("max", "10");

    ProfileDefinition profileDefinition = mock(ProfileDefinition.class);
    when(profileDefinition.createProfile(any(ValidationMessages.class))).thenReturn(profile);
    definitions.add(profileDefinition);

    when(ruleDao.getByKey(session, RuleKey.of("pmd", "rule"))).thenReturn(new RuleDto().setId(10).setSeverity("INFO"));

    when(qProfileOperations.newProfile(eq(name), eq(language), eq(true), any(UserSession.class), eq(session))).thenReturn(new QProfile().setId(1));

    backup.recreateBuiltInProfilesByLanguage(language);

    verify(qProfileActiveRuleOperations).createActiveRule(
      eq(QualityProfileKey.of(name, language)), eq(RuleKey.of("pmd", "rule"))
      , eq("BLOCKER"), eq(session));
    verify(qProfileActiveRuleOperations).updateActiveRuleParam(any(ActiveRuleDto.class), eq("max"), eq("10"), eq(session));
    verifyNoMoreInteractions(qProfileActiveRuleOperations);

    verify(dryRunCache).reportGlobalModification(session);
    verify(session).commit();
  }

  @Test
  public void recreate_built_in_profiles_from_language_with_multiple_profiles_with_same_name_and_same_language() throws Exception {
    RulesProfile profile1 = RulesProfile.create("Default", "java");
    profile1.activateRule(Rule.create("pmd", "rule").setSeverity(RulePriority.BLOCKER), null);
    ProfileDefinition profileDefinition1 = mock(ProfileDefinition.class);
    when(profileDefinition1.createProfile(any(ValidationMessages.class))).thenReturn(profile1);
    definitions.add(profileDefinition1);

    RulesProfile profile2 = RulesProfile.create("Default", "java");
    profile2.activateRule(Rule.create("checkstyle", "rule").setSeverity(RulePriority.MAJOR), null);
    ProfileDefinition profileDefinition2 = mock(ProfileDefinition.class);
    when(profileDefinition2.createProfile(any(ValidationMessages.class))).thenReturn(profile2);
    definitions.add(profileDefinition2);

    when(ruleDao.getByKey(session, RuleKey.of("pmd", "rule"))).thenReturn(new RuleDto().setId(10).setSeverity("INFO"));
    when(ruleDao.getByKey(session, RuleKey.of("checkstyle", "rule"))).thenReturn(new RuleDto().setId(11).setSeverity("INFO"));

    when(qProfileOperations.newProfile(eq("Default"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(new QProfile().setId(1));

    backup.recreateBuiltInProfilesByLanguage("java");

    verify(qProfileActiveRuleOperations).createActiveRule(
      eq(QualityProfileKey.of("Default", "java")),
      eq(RuleKey.of("pmd", "rule")), eq("BLOCKER"), eq(session));
    verify(qProfileActiveRuleOperations).createActiveRule(
      eq(QualityProfileKey.of("Default", "java")),
      eq(RuleKey.of("checkstyle", "rule")), eq("MAJOR"), eq(session));
    verifyNoMoreInteractions(qProfileActiveRuleOperations);

    verify(dryRunCache).reportGlobalModification(session);
    verify(session).commit();
  }

  @Test
  public void fail_to_recreate_built_in_profile_when_rule_not_found() throws Exception {
    String name = "Default";
    String language = "java";

    RulesProfile profile = RulesProfile.create(name, language);
    Rule rule = Rule.create("pmd", "rule");
    profile.activateRule(rule, null);

    ProfileDefinition profileDefinition = mock(ProfileDefinition.class);
    when(profileDefinition.createProfile(any(ValidationMessages.class))).thenReturn(profile);
    definitions.add(profileDefinition);

    when(ruleDao.getByKey(session, RuleKey.of("pmd", "rule"))).thenReturn(null);

    when(qProfileOperations.newProfile(eq(name), eq(language), eq(true), any(UserSession.class), eq(session))).thenReturn(new QProfile().setId(1));

    try {
      backup.recreateBuiltInProfilesByLanguage(language);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
    verifyZeroInteractions(qProfileActiveRuleOperations);
  }

  @Test
  public void not_recreate_built_in_profiles_from_another_language() throws Exception {
    RulesProfile profile = RulesProfile.create("Default", "java");
    profile.activateRule(Rule.create("pmd", "rule").setSeverity(RulePriority.BLOCKER), null);
    ProfileDefinition profileDefinition = mock(ProfileDefinition.class);
    when(profileDefinition.createProfile(any(ValidationMessages.class))).thenReturn(profile);
    definitions.add(profileDefinition);

    backup.recreateBuiltInProfilesByLanguage("js");

    verifyZeroInteractions(qProfileOperations);
    verifyZeroInteractions(qProfileActiveRuleOperations);
  }

  @Test
  public void find_default_profile_names_by_language() throws Exception {
    defaultProfilesCache.put("java", "Basic");
    defaultProfilesCache.put("java", "Default");
    defaultProfilesCache.put("java", "Default");

    Collection<String> result = backup.findDefaultProfileNamesByLanguage("java");
    assertThat(result).containsOnly("Basic", "Default");
  }
}
