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

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.notifications.Notification;
import org.sonar.server.qualityprofile.BuiltInQPChangeNotificationBuilder.Profile;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class BuiltInQPChangeNotificationTest {

  private static final Random RANDOM = new Random();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void serialize_and_parse_no_profile() {
    Notification notification = new BuiltInQPChangeNotificationBuilder().build();

    BuiltInQPChangeNotificationBuilder result = BuiltInQPChangeNotificationBuilder.parse(notification);

    assertThat(result.getProfiles()).isEmpty();
  }

  @Test
  public void serialize_and_parse_single_profile() {
    String profileName = randomAlphanumeric(20);
    String languageKey = randomAlphanumeric(20);
    String languageName = randomAlphanumeric(20);
    int newRules = RANDOM.nextInt(5000);
    int updatedRules = RANDOM.nextInt(5000);
    int removedRules = RANDOM.nextInt(5000);
    long startDate = RANDOM.nextInt(5000);
    long endDate = startDate + RANDOM.nextInt(5000);

    BuiltInQPChangeNotification notification = new BuiltInQPChangeNotificationBuilder()
      .addProfile(Profile.newBuilder()
        .setProfileName(profileName)
        .setLanguageKey(languageKey)
        .setLanguageName(languageName)
        .setNewRules(newRules)
        .setUpdatedRules(updatedRules)
        .setRemovedRules(removedRules)
        .setStartDate(startDate)
        .setEndDate(endDate)
        .build())
      .build();
    BuiltInQPChangeNotificationBuilder result = BuiltInQPChangeNotificationBuilder.parse(notification);

    assertThat(result.getProfiles())
      .extracting(Profile::getProfileName, Profile::getLanguageKey, Profile::getLanguageName, Profile::getNewRules, Profile::getUpdatedRules, Profile::getRemovedRules,
        Profile::getStartDate, Profile::getEndDate)
      .containsExactlyInAnyOrder(tuple(profileName, languageKey, languageName, newRules, updatedRules, removedRules, startDate, endDate));
  }

  @Test
  public void serialize_and_parse_multiple_profiles() {
    String profileName1 = randomAlphanumeric(20);
    String languageKey1 = randomAlphanumeric(20);
    String languageName1 = randomAlphanumeric(20);
    String profileName2 = randomAlphanumeric(20);
    String languageKey2 = randomAlphanumeric(20);
    String languageName2 = randomAlphanumeric(20);

    BuiltInQPChangeNotification notification = new BuiltInQPChangeNotificationBuilder()
      .addProfile(Profile.newBuilder()
        .setProfileName(profileName1)
        .setLanguageKey(languageKey1)
        .setLanguageName(languageName1)
        .build())
      .addProfile(Profile.newBuilder()
        .setProfileName(profileName2)
        .setLanguageKey(languageKey2)
        .setLanguageName(languageName2)
        .build())
      .build();
    BuiltInQPChangeNotificationBuilder result = BuiltInQPChangeNotificationBuilder.parse(notification);

    assertThat(result.getProfiles()).extracting(Profile::getProfileName, Profile::getLanguageKey, Profile::getLanguageName)
      .containsExactlyInAnyOrder(tuple(profileName1, languageKey1, languageName1), tuple(profileName2, languageKey2, languageName2));
  }

  @Test
  public void serialize_and_parse_max_values() {
    String profileName = randomAlphanumeric(20);
    String languageKey = randomAlphanumeric(20);
    String languageName = randomAlphanumeric(20);
    int newRules = Integer.MAX_VALUE;
    int updatedRules = Integer.MAX_VALUE;
    int removedRules = Integer.MAX_VALUE;
    long startDate = Long.MAX_VALUE;
    long endDate = Long.MAX_VALUE;

    BuiltInQPChangeNotification notification = new BuiltInQPChangeNotificationBuilder()
      .addProfile(Profile.newBuilder()
        .setProfileName(profileName)
        .setLanguageKey(languageKey)
        .setLanguageName(languageName)
        .setNewRules(newRules)
        .setUpdatedRules(updatedRules)
        .setRemovedRules(removedRules)
        .setStartDate(startDate)
        .setEndDate(endDate)
        .build())
      .build();
    BuiltInQPChangeNotificationBuilder result = BuiltInQPChangeNotificationBuilder.parse(notification);

    assertThat(result.getProfiles())
      .extracting(Profile::getProfileName, Profile::getLanguageKey, Profile::getLanguageName, Profile::getNewRules, Profile::getUpdatedRules, Profile::getRemovedRules,
        Profile::getStartDate, Profile::getEndDate)
      .containsExactlyInAnyOrder(tuple(profileName, languageKey, languageName, newRules, updatedRules, removedRules, startDate, endDate));
  }

  @Test
  public void fail_with_ISE_when_parsing_empty_notification() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Could not read the built-in quality profile notification");

    BuiltInQPChangeNotificationBuilder.parse(new Notification(BuiltInQPChangeNotification.TYPE));
  }
}
