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
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
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
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QProfileBackupTest {

  @Mock
  MyBatis myBatis;

  @Mock
  DbSession session;

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

    definitions = newArrayList();

    backup = new QProfileBackup(myBatis, qProfileOperations, qProfileActiveRuleOperations, ruleDao,
      definitions, defaultProfilesCache, dryRunCache);

    MockUserSession.set().setLogin("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
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

    when(ruleDao.getNullableByKey(session, RuleKey.of("pmd", "rule"))).thenReturn(new RuleDto().setId(10).setSeverity("INFO"));

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

    when(ruleDao.getNullableByKey(session, RuleKey.of("pmd", "rule"))).thenReturn(new RuleDto().setId(10).setSeverity("INFO"));
    when(ruleDao.getNullableByKey(session, RuleKey.of("checkstyle", "rule"))).thenReturn(new RuleDto().setId(11).setSeverity("INFO"));

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

    when(ruleDao.getNullableByKey(session, RuleKey.of("pmd", "rule"))).thenReturn(null);

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
