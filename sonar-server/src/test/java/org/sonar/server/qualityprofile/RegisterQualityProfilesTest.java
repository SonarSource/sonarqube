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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.user.UserSession;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@Ignore
/* FIXME this test has the following errors:
java.lang.IllegalArgumentException: Name must be set
	at com.google.common.base.Preconditions.checkArgument(Preconditions.java:88)
	at org.sonar.core.qualityprofile.db.QualityProfileKey.of(QualityProfileKey.java:44)
	at org.sonar.server.qualityprofile.RegisterQualityProfiles.register(RegisterQualityProfiles.java:158)
	at org.sonar.server.qualityprofile.RegisterQualityProfiles.start(RegisterQualityProfiles.java:123)
	at org.sonar.server.qualityprofile.RegisterQualityProfilesTest.delete_existing_profile_if_template_is_empty(RegisterQualityProfilesTest.java:338)
  ...
 */
public class RegisterQualityProfilesTest {

  @Mock
  QualityProfileDao qualityProfileDao;

  @Mock
  LoadedTemplateDao loadedTemplateDao;

  @Mock
  QProfileBackup qProfileBackup;

  @Mock
  QProfileOperations qProfileOperations;

  @Mock
  QProfileLookup qProfileLookup;

  @Mock
  ESActiveRule esActiveRule;

  DefaultProfilesCache defaultProfilesCache = new DefaultProfilesCache();

  @Mock
  MyBatis myBatis;

  @Mock
  DbSession session;

  @Mock
  DatabaseSessionFactory sessionFactory;

  @Mock
  PersistentSettings settings;

  List<ProfileDefinition> definitions;

  RegisterQualityProfiles registerQualityProfiles;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession(false)).thenReturn(session);
    when(sessionFactory.getSession()).thenReturn(mock(DatabaseSession.class));

    definitions = newArrayList();
    registerQualityProfiles = new RegisterQualityProfiles(sessionFactory, myBatis, settings, esActiveRule, loadedTemplateDao, qProfileBackup, qProfileOperations, qProfileLookup,
      defaultProfilesCache, null, definitions);
  }

  @Test
  public void register_profile() throws Exception {
    RulesProfile rulesProfile = RulesProfile.create("Default", "java");
    ProfileDefinition profileDefinition = mock(ProfileDefinition.class);
    when(profileDefinition.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile);
    definitions.add(profileDefinition);

    QProfile profile = new QProfile();
    when(qProfileOperations.newProfile(eq("Default"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile);

    registerQualityProfiles.start();

    verify(qProfileBackup).restoreFromActiveRules(
      QualityProfileKey.of(eq(profile).name(),eq(profile).language()) , eq(rulesProfile), eq(session));

    ArgumentCaptor<LoadedTemplateDto> templateCaptor = ArgumentCaptor.forClass(LoadedTemplateDto.class);
    verify(loadedTemplateDao).insert(templateCaptor.capture(), eq(session));
    assertThat(templateCaptor.getValue().getKey()).isEqualTo("java:Default");
    assertThat(templateCaptor.getValue().getType()).isEqualTo("QUALITY_PROFILE");

    verify(settings).saveProperty("sonar.profile.java", "Default");

    assertThat(defaultProfilesCache.byLanguage("java")).containsOnly("Default");

    verify(session).commit();
    verify(esActiveRule).bulkRegisterActiveRules();
  }

  @Test
  public void register_profiles_with_different_languages() throws Exception {
    RulesProfile rulesProfile1 = RulesProfile.create("Default", "java");
    ProfileDefinition profileDefinition1 = mock(ProfileDefinition.class);
    when(profileDefinition1.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile1);
    definitions.add(profileDefinition1);

    RulesProfile rulesProfile2 = RulesProfile.create("Default", "js");
    ProfileDefinition profileDefinition2 = mock(ProfileDefinition.class);
    when(profileDefinition2.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile2);
    definitions.add(profileDefinition2);

    QProfile profile1 = new QProfile();
    when(qProfileOperations.newProfile(eq("Default"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile1);

    QProfile profile2 = new QProfile();
    when(qProfileOperations.newProfile(eq("Default"), eq("js"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile2);

    registerQualityProfiles.start();

    verify(qProfileBackup).restoreFromActiveRules(
      QualityProfileKey.of(eq(profile1).name(),eq(profile1).language()), eq(rulesProfile1), eq(session));
    verify(qProfileBackup).restoreFromActiveRules(
      QualityProfileKey.of(eq(profile2).name(),eq(profile2).language()), eq(rulesProfile2), eq(session));
    verify(loadedTemplateDao, times(2)).insert(any(LoadedTemplateDto.class), eq(session));
    verify(session).commit();

    verify(settings).saveProperty("sonar.profile.java", "Default");
    verify(settings).saveProperty("sonar.profile.js", "Default");

    assertThat(defaultProfilesCache.byLanguage("java")).containsOnly("Default");
    assertThat(defaultProfilesCache.byLanguage("js")).containsOnly("Default");
  }

  @Test
  public void register_two_profiles_with_one_rules_profile_being_default() throws Exception {
    RulesProfile rulesProfile1 = RulesProfile.create("Basic", "java");
    ProfileDefinition profileDefinition1 = mock(ProfileDefinition.class);
    when(profileDefinition1.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile1);
    definitions.add(profileDefinition1);

    // Default profile for java
    RulesProfile rulesProfile2 = RulesProfile.create("Default", "java");
    rulesProfile2.setDefaultProfile(true);
    ProfileDefinition profileDefinition2 = mock(ProfileDefinition.class);
    when(profileDefinition2.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile2);
    definitions.add(profileDefinition2);

    QProfile profile1 = new QProfile();
    when(qProfileOperations.newProfile(eq("Basic"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile1);

    QProfile profile2 = new QProfile();
    when(qProfileOperations.newProfile(eq("Default"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile2);

    registerQualityProfiles.start();

    verify(settings).saveProperty("sonar.profile.java", "Default");
    assertThat(defaultProfilesCache.byLanguage("java")).containsOnly("Default", "Basic");
  }

  @Test
  public void register_two_profiles_with_no_rules_profile_being_default() throws Exception {
    RulesProfile rulesProfile1 = RulesProfile.create("Basic", "java");
    ProfileDefinition profileDefinition1 = mock(ProfileDefinition.class);
    when(profileDefinition1.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile1);
    definitions.add(profileDefinition1);

    RulesProfile rulesProfile2 = RulesProfile.create("Default", "java");
    ProfileDefinition profileDefinition2 = mock(ProfileDefinition.class);
    when(profileDefinition2.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile2);
    definitions.add(profileDefinition2);

    QProfile profile1 = new QProfile();
    when(qProfileOperations.newProfile(eq("Basic"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile1);

    QProfile profile2 = new QProfile();
    when(qProfileOperations.newProfile(eq("Default"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile2);

    registerQualityProfiles.start();

    // No rules profile is defined as default, first one will be the default one
    verify(settings).saveProperty("sonar.profile.java", "Basic");
  }

  @Test
  public void register_two_profiles_with_one_rules_profile_name_using_sonar_way() throws Exception {
    RulesProfile rulesProfile1 = RulesProfile.create("Basic", "java");
    ProfileDefinition profileDefinition1 = mock(ProfileDefinition.class);
    when(profileDefinition1.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile1);
    definitions.add(profileDefinition1);

    // This profile is using 'Sonar way' name, it will be the default one
    RulesProfile rulesProfile2 = RulesProfile.create("Sonar way", "java");
    ProfileDefinition profileDefinition2 = mock(ProfileDefinition.class);
    when(profileDefinition2.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile2);
    definitions.add(profileDefinition2);

    QProfile profile1 = new QProfile();
    when(qProfileOperations.newProfile(eq("Basic"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile1);

    QProfile profile2 = new QProfile();
    when(qProfileOperations.newProfile(eq("Sonar way"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile2);

    registerQualityProfiles.start();

    verify(settings).saveProperty("sonar.profile.java", "Sonar way");
  }

  @Test
  public void fail_to_register_two_profiles_both_being_default() throws Exception {
    RulesProfile rulesProfile1 = RulesProfile.create("Basic", "java");
    rulesProfile1.setDefaultProfile(true);
    ProfileDefinition profileDefinition1 = mock(ProfileDefinition.class);
    when(profileDefinition1.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile1);
    definitions.add(profileDefinition1);

    RulesProfile rulesProfile2 = RulesProfile.create("Default", "java");
    rulesProfile2.setDefaultProfile(true);
    ProfileDefinition profileDefinition2 = mock(ProfileDefinition.class);
    when(profileDefinition2.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile2);
    definitions.add(profileDefinition2);

    try {
      registerQualityProfiles.start();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(SonarException.class);
    }

    verifyZeroInteractions(qProfileLookup);
    verifyZeroInteractions(qProfileBackup);
    verifyZeroInteractions(qProfileOperations);
    verifyZeroInteractions(settings);
  }

  @Test
  public void register_profile_from_multiple_rule_profiles_with_same_name_and_language() throws Exception {
    RulesProfile rulesProfile1 = RulesProfile.create("Default", "java");
    ProfileDefinition profileDefinition1 = mock(ProfileDefinition.class);
    when(profileDefinition1.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile1);
    definitions.add(profileDefinition1);

    RulesProfile rulesProfile2 = RulesProfile.create("Default", "java");
    ProfileDefinition profileDefinition2 = mock(ProfileDefinition.class);
    when(profileDefinition2.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile2);
    definitions.add(profileDefinition2);

    QProfile profile = new QProfile();
    when(qProfileOperations.newProfile(eq("Default"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile);

    registerQualityProfiles.start();

    ArgumentCaptor<RulesProfile> rulesProfileCaptor = ArgumentCaptor.forClass(RulesProfile.class);
    verify(qProfileBackup, times(2)).restoreFromActiveRules(
      QualityProfileKey.of(eq(profile).name(),eq(profile).language()), rulesProfileCaptor.capture(), eq(session));
    assertThat(rulesProfileCaptor.getAllValues().get(0)).isEqualTo(rulesProfile1);
    assertThat(rulesProfileCaptor.getAllValues().get(1)).isEqualTo(rulesProfile2);

    verify(session).commit();
  }

  @Test
  public void not_register_already_registered_profile() throws Exception {
    ProfileDefinition profileDefinition = mock(ProfileDefinition.class);
    when(profileDefinition.createProfile(any(ValidationMessages.class))).thenReturn(RulesProfile.create("Default", "java"));
    definitions.add(profileDefinition);

    when(loadedTemplateDao.countByTypeAndKey(anyString(), anyString(), eq(session))).thenReturn(1);

    registerQualityProfiles.start();

    verify(loadedTemplateDao, never()).insert(any(LoadedTemplateDto.class), eq(session));
    verifyZeroInteractions(qProfileBackup);
    verifyZeroInteractions(qProfileOperations);

    verify(settings).saveProperty("sonar.profile.java", "Default");
  }

  @Test
  public void not_set_profile_as_default_if_language_has_already_a_default_profile() throws Exception {
    ProfileDefinition profileDefinition = mock(ProfileDefinition.class);
    when(profileDefinition.createProfile(any(ValidationMessages.class))).thenReturn(RulesProfile.create("Default", "java"));
    definitions.add(profileDefinition);

    QProfile profile = new QProfile();
    when(qProfileOperations.newProfile(eq("Default"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile);

    when(settings.getString(eq("sonar.profile.java"))).thenReturn("Existing Default Profile");

    registerQualityProfiles.start();

    verify(settings, never()).saveProperty(anyString(), anyString());
  }

  @Test
  public void delete_existing_profile_if_template_is_empty() throws Exception {
    RulesProfile rulesProfile = RulesProfile.create("Default", "java");
    ProfileDefinition profileDefinition = mock(ProfileDefinition.class);
    when(profileDefinition.createProfile(any(ValidationMessages.class))).thenReturn(rulesProfile);
    definitions.add(profileDefinition);

    QProfile profile = new QProfile();
    when(qProfileLookup.profile(eq("Default"), eq("java"), eq(session))).thenReturn(new QProfile());
    when(qProfileOperations.newProfile(eq("Default"), eq("java"), eq(true), any(UserSession.class), eq(session))).thenReturn(profile);

    when(loadedTemplateDao.countByTypeAndKey(anyString(), anyString(), eq(session))).thenReturn(0);

    registerQualityProfiles.start();

    verify(qProfileOperations).deleteProfile(any(QProfile.class), eq(session));
    verify(qProfileBackup).restoreFromActiveRules(
      QualityProfileKey.of(eq(profile).name(),eq(profile).language()), eq(rulesProfile), eq(session));
    verify(session).commit();
  }

  @Test
  public void not_fail_if_no_profile() throws Exception {
    registerQualityProfiles.start();

    verifyZeroInteractions(qProfileLookup);
    verifyZeroInteractions(qProfileBackup);
    verifyZeroInteractions(qProfileOperations);
    verifyZeroInteractions(settings);
  }
}
