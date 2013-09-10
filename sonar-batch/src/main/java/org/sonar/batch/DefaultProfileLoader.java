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
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.utils.SonarException;
import org.sonar.jpa.dao.ProfilesDao;

public class DefaultProfileLoader implements ProfileLoader {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultProfileLoader.class);

  private ProfilesDao dao;

  public DefaultProfileLoader(ProfilesDao dao) {
    this.dao = dao;
  }

  public RulesProfile load(Project project, Settings settings) {

    String profileName = StringUtils.defaultIfBlank(
        settings.getString("sonar.profile"),
        settings.getString("sonar.profile." + project.getLanguageKey()));

    if (StringUtils.isBlank(profileName)) {
      // This means that the current language is not supported by any installed plugin, otherwise at least a
      // "Default <Language Name>" profile would have been created by ActivateDefaultProfiles class.
      String msg = "You must install a plugin that supports the language key '" + project.getLanguageKey() + "'.";
      if (!LOG.isDebugEnabled()) {
        msg += " Run analysis in verbose mode to see list of available language keys.";
      } else {
        msg += " See analysis log for a list of available language keys.";
      }
      throw new SonarException(msg);
    }

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
