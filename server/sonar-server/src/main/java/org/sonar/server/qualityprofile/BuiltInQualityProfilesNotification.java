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
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.qualityprofile.BuiltInQualityProfilesNotificationSender.BUILT_IN_QUALITY_PROFILES;

public class BuiltInQualityProfilesNotification {

  private static final String NUMBER_OF_PROFILES = "numberOfProfiles";
  private static final String PROFILE_NAME = ".profileName";
  private static final String LANGUAGE = ".language";
  private static final String NEW_RULES = ".newRules";
  private static final String UPDATED_RULES = ".updatedRules";
  private static final String REMOVED_RULES = ".removedRules";

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
      notification.setFieldValue(index + PROFILE_NAME, profile.getProfileName());
      notification.setFieldValue(index + LANGUAGE, profile.getLanguage());
      notification.setFieldValue(index + NEW_RULES, String.valueOf(profile.getNewRules()));
      notification.setFieldValue(index + UPDATED_RULES, String.valueOf(profile.getUpdatedRules()));
      notification.setFieldValue(index + REMOVED_RULES, String.valueOf(profile.getRemovedRules()));
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
      .mapToObj(index -> Profile.newBuilder(
        getNonNullFieldValue(notification, index + PROFILE_NAME),
        getNonNullFieldValue(notification, index + LANGUAGE))
        .setNewRules(parseInt(getNonNullFieldValue(notification, index + NEW_RULES)))
        .setUpdatedRules(parseInt(getNonNullFieldValue(notification, index + UPDATED_RULES)))
        .setRemovedRules(parseInt(getNonNullFieldValue(notification, index + REMOVED_RULES)))
        .build())
      .forEach(notif::addProfile);
    return notif;
  }

  private static String getNonNullFieldValue(Notification notification, String key) {
    String value = notification.getFieldValue(key);
    return requireNonNull(value, format("Notification field '%s' is null", key));
  }

  public List<Profile> getProfiles() {
    return profiles;
  }

  public static class Profile {
    private final String profileName;
    private final String language;
    private final int newRules;
    private final int updatedRules;
    private final int removedRules;

    public Profile(Builder builder) {
      this.profileName = builder.profileName;
      this.language = builder.language;
      this.newRules = builder.newRules;
      this.updatedRules = builder.updatedRules;
      this.removedRules = builder.removedRules;
    }

    public String getProfileName() {
      return profileName;
    }

    public String getLanguage() {
      return language;
    }

    public int getNewRules() {
      return newRules;
    }

    public int getUpdatedRules() {
      return updatedRules;
    }

    public int getRemovedRules() {
      return removedRules;
    }

    public static Builder newBuilder(String profileName, String language) {
      return new Builder(profileName, language);
    }

    public static class Builder {
      private final String profileName;
      private final String language;
      private int newRules;
      private int updatedRules;
      private int removedRules;

      private Builder(String profileName, String language) {
        this.profileName = requireNonNull(profileName, "profileName should not be null");
        this.language = requireNonNull(language, "language should not be null");
      }

      public Builder setNewRules(int newRules) {
        checkState(newRules >= 0, "newRules should not be negative");
        this.newRules = newRules;
        return this;
      }

      public Builder setUpdatedRules(int updatedRules) {
        checkState(updatedRules >= 0, "updatedRules should not be negative");
        this.updatedRules = updatedRules;
        return this;
      }

      public Builder setRemovedRules(int removedRules) {
        checkState(removedRules >= 0, "removedRules should not be negative");
        this.removedRules = removedRules;
        return this;
      }

      public Profile build() {
        return new Profile(this);
      }
    }
  }
}
