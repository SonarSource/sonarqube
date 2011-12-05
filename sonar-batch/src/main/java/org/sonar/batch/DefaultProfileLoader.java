/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.jpa.dao.ProfilesDao;

public class DefaultProfileLoader implements ProfileLoader {
  static final String PARAM_PROFILE = "sonar.profile";

  private ProfilesDao dao;

  public DefaultProfileLoader(ProfilesDao dao) {
    this.dao = dao;
  }

  public RulesProfile load(Project project) {
    String profileName = (String) project.getProperty(PARAM_PROFILE);
    RulesProfile profile;
    if (profileName == null) {
      Project root = project.getRoot();
      profile = dao.getActiveProfile(root.getLanguageKey(), root.getKey());
      if (profile == null) {
        throw new RuntimeException("Quality profile not found for " + root.getKey() + ", language " + root.getLanguageKey());
      }

    } else {
      profile = dao.getProfile(project.getLanguageKey(), profileName);
      if (profile == null) {
        throw new RuntimeException("Quality profile not found : " + profileName + ", language " + project.getLanguageKey());
      }
    }

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
