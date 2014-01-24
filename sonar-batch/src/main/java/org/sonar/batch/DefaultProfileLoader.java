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
package org.sonar.batch;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ModuleLanguages;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.utils.SonarException;
import org.sonar.jpa.dao.ProfilesDao;

import java.util.HashMap;
import java.util.Map;

public class DefaultProfileLoader implements ProfileLoader {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultProfileLoader.class);

  private ProfilesDao dao;
  private Languages languages;
  private ModuleLanguages moduleLanguages;

  public DefaultProfileLoader(ProfilesDao dao, ModuleLanguages moduleLanguages, Languages languages) {
    this.dao = dao;
    this.moduleLanguages = moduleLanguages;
    this.languages = languages;
  }

  public RulesProfile load(Project module, Settings settings) {
    Map<String, RulesProfile> profilesPerLanguageKey = new HashMap<String, RulesProfile>();

    // TODO For now we have to load profile of all languages even if module will only use part of them because ModuleLanguages may not be
    // initialized yet
    for (Language language : languages.all()) {
      String languageKey = language.getKey();

      String profileName = StringUtils.defaultIfBlank(
        settings.getString("sonar.profile"),
        settings.getString("sonar.profile." + languageKey));

      RulesProfile profile = dao.getProfile(languageKey, profileName);
      if (profile == null) {
        throw new SonarException("Quality profile not found : " + profileName + ", language " + languageKey);
      }

      profilesPerLanguageKey.put(languageKey, hibernateHack(profile));
    }

    RulesProfile profile = new RulesProfileWrapper(moduleLanguages, profilesPerLanguageKey);
    for (Map.Entry<String, RulesProfile> profiles : profilesPerLanguageKey.entrySet()) {
      LOG.info("Quality profile for {}: {}", profiles.getKey(), profiles.getValue());
    }
    return profile;
  }

  private RulesProfile hibernateHack(RulesProfile profile) {
    // hack to lazy initialize the profile collections
    profile.getActiveRules().size();
    for (ActiveRule activeRule : profile.getActiveRules()) {
      activeRule.getActiveRuleParams().size();
      activeRule.getRule().getParams().size();
    }
    profile.getAlerts().size();
    return profile;
  }

}
