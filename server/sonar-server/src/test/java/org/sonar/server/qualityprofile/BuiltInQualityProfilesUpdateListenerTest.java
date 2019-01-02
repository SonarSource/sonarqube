/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Random;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.qualityprofile.BuiltInQualityProfilesNotification.Profile;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sonar.core.config.CorePropertyDefinitions.DISABLE_NOTIFICATION_ON_BUILT_IN_QPROFILES;
import static org.sonar.server.language.LanguageTesting.newLanguage;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.UPDATED;

public class BuiltInQualityProfilesUpdateListenerTest {

  private NotificationManager notificationManager = mock(NotificationManager.class);
  private MapSettings settings = new MapSettings();

  @Test
  public void add_profile_to_notification_for_added_rules() {
    enableNotificationInGlobalSettings();
    Multimap<QProfileName, ActiveRuleChange> profiles = ArrayListMultimap.create();
    Languages languages = new Languages();
    Tuple expectedTuple = addProfile(profiles, languages, ACTIVATED);

    BuiltInQualityProfilesUpdateListener underTest = new BuiltInQualityProfilesUpdateListener(notificationManager, languages, settings.asConfig());
    underTest.onChange(profiles, 0, 1);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    verifyNoMoreInteractions(notificationManager);
    assertThat(BuiltInQualityProfilesNotification.parse(notificationArgumentCaptor.getValue()).getProfiles())
      .extracting(Profile::getProfileName, Profile::getLanguageKey, Profile::getLanguageName, Profile::getNewRules)
      .containsExactlyInAnyOrder(expectedTuple);
  }

  @Test
  public void add_profile_to_notification_for_updated_rules() {
    enableNotificationInGlobalSettings();
    Multimap<QProfileName, ActiveRuleChange> profiles = ArrayListMultimap.create();
    Languages languages = new Languages();
    Tuple expectedTuple = addProfile(profiles, languages, UPDATED);

    BuiltInQualityProfilesUpdateListener underTest = new BuiltInQualityProfilesUpdateListener(notificationManager, languages, settings.asConfig());
    underTest.onChange(profiles, 0, 1);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    verifyNoMoreInteractions(notificationManager);
    assertThat(BuiltInQualityProfilesNotification.parse(notificationArgumentCaptor.getValue()).getProfiles())
      .extracting(Profile::getProfileName, Profile::getLanguageKey, Profile::getLanguageName, Profile::getUpdatedRules)
      .containsExactlyInAnyOrder(expectedTuple);
  }

  @Test
  public void add_profile_to_notification_for_removed_rules() {
    enableNotificationInGlobalSettings();
    Multimap<QProfileName, ActiveRuleChange> profiles = ArrayListMultimap.create();
    Languages languages = new Languages();
    Tuple expectedTuple = addProfile(profiles, languages, DEACTIVATED);

    BuiltInQualityProfilesUpdateListener underTest = new BuiltInQualityProfilesUpdateListener(notificationManager, languages, settings.asConfig());
    underTest.onChange(profiles, 0, 1);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    verifyNoMoreInteractions(notificationManager);
    assertThat(BuiltInQualityProfilesNotification.parse(notificationArgumentCaptor.getValue()).getProfiles())
      .extracting(Profile::getProfileName, Profile::getLanguageKey, Profile::getLanguageName, Profile::getRemovedRules)
      .containsExactlyInAnyOrder(expectedTuple);
  }

  @Test
  public void add_multiple_profiles_to_notification() {
    enableNotificationInGlobalSettings();
    Multimap<QProfileName, ActiveRuleChange> profiles = ArrayListMultimap.create();
    Languages languages = new Languages();
    Tuple expectedTuple1 = addProfile(profiles, languages, ACTIVATED);
    Tuple expectedTuple2 = addProfile(profiles, languages, ACTIVATED);

    BuiltInQualityProfilesUpdateListener underTest = new BuiltInQualityProfilesUpdateListener(notificationManager, languages, settings.asConfig());
    underTest.onChange(profiles, 0, 1);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    verifyNoMoreInteractions(notificationManager);
    assertThat(BuiltInQualityProfilesNotification.parse(notificationArgumentCaptor.getValue()).getProfiles())
      .extracting(Profile::getProfileName, Profile::getLanguageKey, Profile::getLanguageName, Profile::getNewRules)
      .containsExactlyInAnyOrder(expectedTuple1, expectedTuple2);
  }

  @Test
  public void add_start_and_end_dates_to_notification() {
    enableNotificationInGlobalSettings();
    Multimap<QProfileName, ActiveRuleChange> profiles = ArrayListMultimap.create();
    Languages languages = new Languages();
    addProfile(profiles, languages, ACTIVATED);
    long startDate = 10_000_000_000L;
    long endDate = 15_000_000_000L;

    BuiltInQualityProfilesUpdateListener underTest = new BuiltInQualityProfilesUpdateListener(notificationManager, languages, settings.asConfig());
    underTest.onChange(profiles, startDate, endDate);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager).scheduleForSending(notificationArgumentCaptor.capture());
    verifyNoMoreInteractions(notificationManager);
    assertThat(BuiltInQualityProfilesNotification.parse(notificationArgumentCaptor.getValue()).getProfiles())
      .extracting(Profile::getStartDate, Profile::getEndDate)
      .containsExactlyInAnyOrder(tuple(startDate, endDate));
  }

  @Test
  public void avoid_notification_if_configured_in_settings() {
    settings.setProperty(DISABLE_NOTIFICATION_ON_BUILT_IN_QPROFILES, true);
    Multimap<QProfileName, ActiveRuleChange> profiles = ArrayListMultimap.create();
    Languages languages = new Languages();
    addProfile(profiles, languages, ACTIVATED);

    BuiltInQualityProfilesUpdateListener underTest = new BuiltInQualityProfilesUpdateListener(notificationManager, languages, settings.asConfig());
    underTest.onChange(profiles, 0, 1);

    verifyZeroInteractions(notificationManager);
  }

  private Tuple addProfile(Multimap<QProfileName, ActiveRuleChange> profiles, Languages languages, ActiveRuleChange.Type type) {
    String profileName = randomLowerCaseText();
    int ruleId1 = new Random().nextInt(952);
    int ruleId2 = new Random().nextInt(952);
    Language language = newLanguage(randomLowerCaseText(), randomLowerCaseText());
    languages.add(language);
    profiles.putAll(new QProfileName(language.getKey(), profileName),
      asList(new ActiveRuleChange(
        type,
        ActiveRuleKey.parse("qp:repo:rule1"), new RuleDefinitionDto().setId(ruleId1)),
        new ActiveRuleChange(type, ActiveRuleKey.parse("qp:repo:rule2"), new RuleDefinitionDto().setId(ruleId2))));
    return tuple(profileName, language.getKey(), language.getName(), 2);
  }

  private static String randomLowerCaseText() {
    return randomAlphanumeric(20).toLowerCase();
  }

  private void enableNotificationInGlobalSettings() {
    settings.setProperty(DISABLE_NOTIFICATION_ON_BUILT_IN_QPROFILES, false);
  }
}
