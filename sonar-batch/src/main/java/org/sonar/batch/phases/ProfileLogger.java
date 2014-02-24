/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.phases;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.ModuleLanguages;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.rule.ModuleQProfiles.QProfile;
import org.sonar.batch.rule.ProjectAlerts;
import org.sonar.batch.rule.RulesProfileWrapper;

public class ProfileLogger implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileLogger.class);

  private final Settings settings;
  private final ModuleLanguages languages;
  private final ModuleQProfiles profiles;
  private final ProjectAlerts projectAlerts;

  private final RulesProfile rulesProfile;

  public ProfileLogger(Settings settings, ModuleLanguages languages, ModuleQProfiles profiles, ProjectAlerts projectAlerts, RulesProfile rulesProfile) {
    this.settings = settings;
    this.languages = languages;
    this.profiles = profiles;
    this.projectAlerts = projectAlerts;
    this.rulesProfile = rulesProfile;
  }

  public void execute() {
    execute(LOG);
  }

  @VisibleForTesting
  void execute(Logger logger) {
    String defaultName = settings.getString(ModuleQProfiles.SONAR_PROFILE_PROP);
    boolean defaultNameUsed = StringUtils.isBlank(defaultName);
    for (String lang : languages.keys()) {
      QProfile profile = profiles.findByLanguage(lang);
      if (profile == null) {
        logger.warn("No Quality profile found for language " + lang);
      } else {
        logger.info("Quality profile for {}: {}", lang, profile.name());
        if (StringUtils.isNotBlank(defaultName) && defaultName.equals(profile.name())) {
          defaultNameUsed = true;
        }
      }
    }
    if (!defaultNameUsed && !languages.keys().isEmpty()) {
      throw MessageException.of("sonar.profile was set to '" + defaultName + "' but didn't match any profile for any language. Please check your configuration.");
    }

    addModuleAlertsToProjectAlerts();
  }

  private void addModuleAlertsToProjectAlerts() {
    RulesProfileWrapper profileWrapper = (RulesProfileWrapper) rulesProfile;
    for (String lang : languages.keys()) {
      RulesProfile profile = profileWrapper.getProfileByLanguage(lang);
      for (Alert alert : profile.getAlerts()) {
        projectAlerts.add(alert);
      }
    }
  }

}
