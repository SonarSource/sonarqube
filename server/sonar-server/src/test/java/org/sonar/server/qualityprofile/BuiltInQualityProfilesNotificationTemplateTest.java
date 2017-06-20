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
  public void notification_contains_list_of_quality_profiles() {
    String profileName = randomAlphanumeric(20);
    String language = randomAlphanumeric(20);
    BuiltInQualityProfilesNotification notification = new BuiltInQualityProfilesNotification()
      .addProfile(new Profile(profileName, language));

    EmailMessage emailMessage = underTest.format(notification.serialize());

    assertThat(emailMessage.getMessage()).isEqualTo("Built-in quality profiles have been updated:\n" +
      "\"" + profileName + "\" - " + language + "\n" +
      "This is a good time to review your quality profiles and update them to benefit from the latest evolutions.");
  }
}
