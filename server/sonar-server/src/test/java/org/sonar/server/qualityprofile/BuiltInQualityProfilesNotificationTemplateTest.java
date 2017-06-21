/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.junit.Test;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.server.qualityprofile.BuiltInQualityProfilesNotification.Profile;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class BuiltInQualityProfilesNotificationTemplateTest {

  private BuiltInQualityProfilesNotificationTemplate underTest = new BuiltInQualityProfilesNotificationTemplate();

  @Test
  public void notification_contains_list_of_new_rules() {
    String profileName = randomAlphanumeric(20);
    String language = randomAlphanumeric(20);
    BuiltInQualityProfilesNotification notification = new BuiltInQualityProfilesNotification()
      .addProfile(Profile.newBuilder(profileName, language)
        .setNewRules(2)
        .build());

    EmailMessage emailMessage = underTest.format(notification.serialize());

    assertThat(emailMessage.getMessage()).isEqualTo("Built-in quality profiles have been updated:\n" +
      "\"" + profileName + "\" - " + language + "\n" +
      " 2 new rules\n" +
      "This is a good time to review your quality profiles and update them to benefit from the latest evolutions.");
  }

  @Test
  public void notification_contains_list_of_updated_rules() {
    String profileName = randomAlphanumeric(20);
    String language = randomAlphanumeric(20);
    BuiltInQualityProfilesNotification notification = new BuiltInQualityProfilesNotification()
      .addProfile(Profile.newBuilder(profileName, language)
        .setUpdatedRules(2)
        .build());

    EmailMessage emailMessage = underTest.format(notification.serialize());

    assertThat(emailMessage.getMessage()).isEqualTo("Built-in quality profiles have been updated:\n" +
      "\"" + profileName + "\" - " + language + "\n" +
      " 2 rules have been updated\n" +
      "This is a good time to review your quality profiles and update them to benefit from the latest evolutions.");
  }

  @Test
  public void notification_contains_list_of_removed_rules() {
    String profileName = randomAlphanumeric(20);
    String language = randomAlphanumeric(20);
    BuiltInQualityProfilesNotification notification = new BuiltInQualityProfilesNotification()
      .addProfile(Profile.newBuilder(profileName, language)
        .setRemovedRules(2)
        .build());

    EmailMessage emailMessage = underTest.format(notification.serialize());

    assertThat(emailMessage.getMessage()).isEqualTo("Built-in quality profiles have been updated:\n" +
      "\"" + profileName + "\" - " + language + "\n" +
      " 2 rules removed\n" +
      "This is a good time to review your quality profiles and update them to benefit from the latest evolutions.");
  }

  @Test
  public void notification_contains_list_of_new_updated_and_removed_rules() {
    String profileName = randomAlphanumeric(20);
    String language = randomAlphanumeric(20);
    BuiltInQualityProfilesNotification notification = new BuiltInQualityProfilesNotification()
      .addProfile(Profile.newBuilder(profileName, language)
        .setNewRules(2)
        .setUpdatedRules(3)
        .setRemovedRules(4)
        .build());

    EmailMessage emailMessage = underTest.format(notification.serialize());

    assertThat(emailMessage.getMessage()).isEqualTo("Built-in quality profiles have been updated:\n" +
      "\"" + profileName + "\" - " + language + "\n" +
      " 2 new rules\n" +
      " 3 rules have been updated\n" +
      " 4 rules removed\n" +
      "This is a good time to review your quality profiles and update them to benefit from the latest evolutions.");
  }

  @Test
  public void notification_contains_many_profiles() {
    String profileName1 = "profile1_" + randomAlphanumeric(20);
    String language1 = "lang1_" + randomAlphanumeric(20);
    String profileName2 = "profile1_" + randomAlphanumeric(20);
    String language2 = "lang2_" + randomAlphanumeric(20);
    BuiltInQualityProfilesNotification notification = new BuiltInQualityProfilesNotification()
      .addProfile(Profile.newBuilder(profileName1, language1)
        .setNewRules(2)
        .build())
      .addProfile(Profile.newBuilder(profileName2, language2)
        .setNewRules(13)
        .build());

    EmailMessage emailMessage = underTest.format(notification.serialize());

    assertThat(emailMessage.getMessage()).isEqualTo("Built-in quality profiles have been updated:\n" +
      "\"" + profileName1 + "\" - " + language1 + "\n" +
      " 2 new rules\n" +
      "\"" + profileName2 + "\" - " + language2 + "\n" +
      " 13 new rules\n" +
      "This is a good time to review your quality profiles and update them to benefit from the latest evolutions.");
  }

  @Test
  public void notification_contains_profiles_sorted_by_language_then_by_profile_name() {
    String language1 = "lang1_" + randomAlphanumeric(20);
    String language2 = "lang2_" + randomAlphanumeric(20);
    String profileName1 = "profile1_" + randomAlphanumeric(20);
    String profileName2 = "profile2_" + randomAlphanumeric(20);
    String profileName3 = "profile3_" + randomAlphanumeric(20);
    BuiltInQualityProfilesNotification notification = new BuiltInQualityProfilesNotification()
      .addProfile(Profile.newBuilder(profileName3, language2).build())
      .addProfile(Profile.newBuilder(profileName2, language1).build())
      .addProfile(Profile.newBuilder(profileName1, language2).build());

    EmailMessage emailMessage = underTest.format(notification.serialize());

    assertThat(emailMessage.getMessage()).containsSequence(
      "\"" + profileName2 + "\" - " + language1,
      "\"" + profileName1 + "\" - " + language2,
      "\"" + profileName3 + "\" - " + language2);
  }
}
