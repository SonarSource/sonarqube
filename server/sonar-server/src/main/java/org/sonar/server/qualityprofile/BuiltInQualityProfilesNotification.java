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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.sonar.api.notifications.Notification;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.qualityprofile.BuiltInQualityProfilesNotificationSender.BUILT_IN_QUALITY_PROFILES;

public class BuiltInQualityProfilesNotification {

  private static final String NUMBER_OF_PROFILES = "numberOfProfiles";
  private static final String PROFILE_NAME = ".profileName";
  private static final String LANGUAGE = ".language";
  private final List<Profile> profiles = new ArrayList<>();

  public BuiltInQualityProfilesNotification addProfile(Profile profile) {
    profiles.add(profile);
    return this;
  }

  public Notification serialize() {
    Notification notification = new Notification(BUILT_IN_QUALITY_PROFILES);
    notification.setFieldValue(NUMBER_OF_PROFILES, String.valueOf(profiles.size()));
    AtomicInteger count = new AtomicInteger();
    profiles.forEach(profile -> {
      int index = count.getAndIncrement();
      notification.setFieldValue(index + ".profileName", profile.getProfileName());
      notification.setFieldValue(index + ".language", profile.getLanguage());
    });
    return notification;
  }

  public static BuiltInQualityProfilesNotification parse(Notification notification) {
    checkState(BUILT_IN_QUALITY_PROFILES.equals(notification.getType()),
      "Expected notification of type %s but got %s", BUILT_IN_QUALITY_PROFILES, notification.getType());
    BuiltInQualityProfilesNotification notif = new BuiltInQualityProfilesNotification();
    String numberOfProfilesText = notification.getFieldValue(NUMBER_OF_PROFILES);
    checkState(numberOfProfilesText != null, "Could not read the built-in quality profile notification");
    Integer numberOfProfiles = Integer.valueOf(numberOfProfilesText);
    IntStream.rangeClosed(0, numberOfProfiles - 1)
      .mapToObj(index -> new Profile(
        requireNonNull(notification.getFieldValue(index + PROFILE_NAME)),
        requireNonNull(notification.getFieldValue(index + LANGUAGE))))
      .forEach(notif::addProfile);
    return notif;
  }

  public List<Profile> getProfiles() {
    return profiles;
  }

  public static class Profile {
    private final String profileName;
    private final String language;

    public Profile(String profileName, String language) {
      this.profileName = profileName;
      this.language = language;
    }

    public String getProfileName() {
      return profileName;
    }

    public String getLanguage() {
      return language;
    }
  }
}
