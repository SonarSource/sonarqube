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
package org.sonar.batch.rule;

import com.google.common.collect.Lists;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.ModuleLanguages;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.jpa.dao.ProfilesDao;

import java.util.Collection;

/**
 * Ensures backward-compatibility with extensions that use {@link org.sonar.api.profiles.RulesProfile}.
 */
public class RulesProfileProvider extends ProviderAdapter {

  private RulesProfile singleton = null;

  public RulesProfile provide(ModuleQProfiles qProfiles, ModuleLanguages moduleLanguages, ProfilesDao dao) {
    if (singleton == null) {
      if (moduleLanguages.keys().size() == 1) {
        // Backward-compatibility with single-language modules
        singleton = loadSingleLanguageProfile(qProfiles, moduleLanguages.keys().iterator().next(), dao);
      } else {
        singleton = loadProfiles(qProfiles, dao);
      }
    }
    return singleton;
  }

  private RulesProfile loadSingleLanguageProfile(ModuleQProfiles qProfiles, String language, ProfilesDao dao) {
    ModuleQProfiles.QProfile qProfile = qProfiles.findByLanguage(language);
    return new RulesProfileWrapper(select(qProfile, dao));
  }

  private RulesProfile loadProfiles(ModuleQProfiles qProfiles, ProfilesDao dao) {
    Collection<RulesProfile> dtos = Lists.newArrayList();
    for (ModuleQProfiles.QProfile qProfile : qProfiles.findAll()) {
      dtos.add(select(qProfile, dao));
    }
    return new RulesProfileWrapper(dtos);
  }

  private RulesProfile select(ModuleQProfiles.QProfile qProfile, ProfilesDao dao) {
    RulesProfile dto = dao.getProfile(qProfile.language(), qProfile.name());
    return hibernateHack(dto);
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
