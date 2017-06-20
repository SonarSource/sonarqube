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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.notifications.Notification;
import org.sonar.server.qualityprofile.BuiltInQualityProfilesNotification.Profile;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.qualityprofile.BuiltInQualityProfilesNotificationSender.BUILT_IN_QUALITY_PROFILES;

public class BuiltInQualityProfilesNotificationTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void serialize_and_parse_no_profile() {
    Notification notification = new BuiltInQualityProfilesNotification().serialize();

    BuiltInQualityProfilesNotification result = BuiltInQualityProfilesNotification.parse(notification);

    assertThat(result.getProfiles()).isEmpty();
  }

  @Test
  public void serialize_and_parse_single_profile() {
    String profileName = randomAlphanumeric(20);
    String language = randomAlphanumeric(20);

    Notification notification = new BuiltInQualityProfilesNotification().addProfile(new Profile(profileName, language)).serialize();
    BuiltInQualityProfilesNotification result = BuiltInQualityProfilesNotification.parse(notification);

    assertThat(result.getProfiles()).extracting(Profile::getProfileName, Profile::getLanguage)
      .containsExactlyInAnyOrder(tuple(profileName, language));
  }

  @Test
  public void serialize_and_parse_multiple_profiles() {
    String profileName1 = randomAlphanumeric(20);
    String language1 = randomAlphanumeric(20);
    String profileName2 = randomAlphanumeric(20);
    String language2 = randomAlphanumeric(20);

    Notification notification = new BuiltInQualityProfilesNotification()
      .addProfile(new Profile(profileName1, language1))
      .addProfile(new Profile(profileName2, language2))
      .serialize();
    BuiltInQualityProfilesNotification result = BuiltInQualityProfilesNotification.parse(notification);

    assertThat(result.getProfiles()).extracting(Profile::getProfileName, Profile::getLanguage)
      .containsExactlyInAnyOrder(tuple(profileName1, language1), tuple(profileName2, language2));
  }

  @Test
  public void fail_with_ISE_when_parsing_empty_notification() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Could not read the built-in quality profile notification");

    BuiltInQualityProfilesNotification.parse(new Notification(BUILT_IN_QUALITY_PROFILES));
  }
}
