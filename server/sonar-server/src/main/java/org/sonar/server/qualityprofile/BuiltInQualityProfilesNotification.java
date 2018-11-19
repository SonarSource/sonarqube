/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.qualityprofile.BuiltInQualityProfilesUpdateListener.BUILT_IN_QUALITY_PROFILES;

public class BuiltInQualityProfilesNotification {

  private static final String NUMBER_OF_PROFILES = "numberOfProfiles";
  private static final String PROFILE_NAME = ".profileName";
  private static final String LANGUAGE_KEY = ".languageKey";
  private static final String LANGUAGE_NAME = ".languageName";
  private static final String NEW_RULES = ".newRules";
  private static final String UPDATED_RULES = ".updatedRules";
  private static final String REMOVED_RULES = ".removedRules";
  private static final String START_DATE = ".startDate";
  private static final String END_DATE = ".endDate";

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
      notification.setFieldValue(index + LANGUAGE_KEY, profile.getLanguageKey());
      notification.setFieldValue(index + LANGUAGE_NAME, profile.getLanguageName());
      notification.setFieldValue(index + NEW_RULES, String.valueOf(profile.getNewRules()));
      notification.setFieldValue(index + UPDATED_RULES, String.valueOf(profile.getUpdatedRules()));
      notification.setFieldValue(index + REMOVED_RULES, String.valueOf(profile.getRemovedRules()));
      notification.setFieldValue(index + START_DATE, String.valueOf(profile.getStartDate()));
      notification.setFieldValue(index + END_DATE, String.valueOf(profile.getEndDate()));
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
      .mapToObj(index -> Profile.newBuilder()
        .setProfileName(getNonNullFieldValue(notification, index + PROFILE_NAME))
        .setLanguageKey(getNonNullFieldValue(notification, index + LANGUAGE_KEY))
        .setLanguageName(getNonNullFieldValue(notification, index + LANGUAGE_NAME))
        .setNewRules(parseInt(getNonNullFieldValue(notification, index + NEW_RULES)))
        .setUpdatedRules(parseInt(getNonNullFieldValue(notification, index + UPDATED_RULES)))
        .setRemovedRules(parseInt(getNonNullFieldValue(notification, index + REMOVED_RULES)))
        .setStartDate(parseLong(getNonNullFieldValue(notification, index + START_DATE)))
        .setEndDate(parseLong(getNonNullFieldValue(notification, index + END_DATE)))
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
    private final String languageKey;
    private final String languageName;
    private final int newRules;
    private final int updatedRules;
    private final int removedRules;
    private final long startDate;
    private final long endDate;

    public Profile(Builder builder) {
      this.profileName = builder.profileName;
      this.languageKey = builder.languageKey;
      this.languageName = builder.languageName;
      this.newRules = builder.newRules;
      this.updatedRules = builder.updatedRules;
      this.removedRules = builder.removedRules;
      this.startDate = builder.startDate;
      this.endDate = builder.endDate;
    }

    public String getProfileName() {
      return profileName;
    }

    public String getLanguageKey() {
      return languageKey;
    }

    public String getLanguageName() {
      return languageName;
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

    public long getStartDate() {
      return startDate;
    }

    public long getEndDate() {
      return endDate;
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public static class Builder {
      private String profileName;
      private String languageKey;
      private String languageName;
      private int newRules;
      private int updatedRules;
      private int removedRules;
      private long startDate;
      private long endDate;

      private Builder() {
      }

      public Builder setLanguageKey(String languageKey) {
        this.languageKey = requireNonNull(languageKey, "languageKEy should not be null");
        return this;
      }

      public Builder setLanguageName(String languageName) {
        this.languageName = requireNonNull(languageName, "languageName should not be null");
        return this;
      }

      public Builder setProfileName(String profileName) {
        this.profileName = requireNonNull(profileName, "profileName should not be null");
        return this;
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

      public Builder setStartDate(long startDate) {
        this.startDate = startDate;
        return this;
      }

      public Builder setEndDate(long endDate) {
        this.endDate = endDate;
        return this;
      }

      public Profile build() {
        return new Profile(this);
      }
    }
  }
}
