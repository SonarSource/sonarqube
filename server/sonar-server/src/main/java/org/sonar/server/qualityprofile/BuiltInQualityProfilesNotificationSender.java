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

import com.google.common.collect.Multimap;
import java.util.Collection;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.qualityprofile.BuiltInQualityProfilesNotification.Profile;

import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.DEACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.UPDATED;

public class BuiltInQualityProfilesNotificationSender {

  static final String BUILT_IN_QUALITY_PROFILES = "built-in-quality-profiles";

  private final NotificationManager notificationManager;
  private final Languages languages;

  public BuiltInQualityProfilesNotificationSender(NotificationManager notificationManager, Languages languages) {
    this.notificationManager = notificationManager;
    this.languages = languages;
  }

  void send(Multimap<QProfileName, ActiveRuleChange> changedProfiles) {
    BuiltInQualityProfilesNotification notification = new BuiltInQualityProfilesNotification();
    changedProfiles.keySet().stream()
      .map(changedProfile -> {
        String profileName = changedProfile.getName();
        Language language = languages.get(changedProfile.getLanguage());
        Collection<ActiveRuleChange> activeRuleChanges = changedProfiles.get(changedProfile);
        int newRules = (int) activeRuleChanges.stream().map(ActiveRuleChange::getType).filter(ACTIVATED::equals).count();
        int updatedRules = (int) activeRuleChanges.stream().map(ActiveRuleChange::getType).filter(UPDATED::equals).count();
        int removedRules = (int) activeRuleChanges.stream().map(ActiveRuleChange::getType).filter(DEACTIVATED::equals).count();
        return Profile.newBuilder()
          .setProfileName(profileName)
          .setLanguageKey(language.getKey())
          .setLanguageName(language.getName())
          .setNewRules(newRules)
          .setUpdatedRules(updatedRules)
          .setRemovedRules(removedRules)
          .build();
      })
      .forEach(notification::addProfile);
    notificationManager.scheduleForSending(notification.serialize());
  }
}
