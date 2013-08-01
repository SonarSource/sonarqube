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

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.utils.SonarException;
import org.sonar.jpa.dao.ProfilesDao;

public class DefaultProfileLoader implements ProfileLoader {
  private ProfilesDao dao;

  public DefaultProfileLoader(ProfilesDao dao) {
    this.dao = dao;
  }

  public RulesProfile load(Project project, Settings settings, Languages languages) {
    if (!languages.allKey().contains(project.getLanguageKey())) {
      String languageList = Joiner.on(", ").join(languages.allKey());
      throw new SonarException("You must install a plugin that supports the language '" + project.getLanguageKey() +
        "'. Supported language keys are: " + languageList);
    }

    String profileName = StringUtils.defaultIfBlank(
        settings.getString("sonar.profile"),
        settings.getString("sonar.profile." + project.getLanguageKey()));

    RulesProfile profile = dao.getProfile(project.getLanguageKey(), profileName);
    if (profile == null) {
      throw new SonarException("Quality profile not found : " + profileName + ", language " + project.getLanguageKey());
    }

    return hibernateHack(profile);
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
