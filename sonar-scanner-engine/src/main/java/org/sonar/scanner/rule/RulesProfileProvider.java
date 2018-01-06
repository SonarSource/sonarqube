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
package org.sonar.scanner.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.config.Configuration;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

/**
 * Ensures backward-compatibility with extensions that use {@link org.sonar.api.profiles.RulesProfile}.
 */
public class RulesProfileProvider extends ProviderAdapter {

  private RulesProfile singleton = null;

  public RulesProfile provide(ModuleQProfiles qProfiles, ActiveRules activeRules, Configuration settings) {
    if (singleton == null) {
      String lang = settings.get(CoreProperties.PROJECT_LANGUAGE_PROPERTY).orElse(null);
      if (StringUtils.isNotBlank(lang)) {
        // Backward-compatibility with single-language modules
        singleton = loadSingleLanguageProfile(qProfiles, activeRules, lang);
      } else {
        singleton = loadProfiles(qProfiles, activeRules);
      }
    }
    return singleton;
  }

  private static RulesProfile loadSingleLanguageProfile(ModuleQProfiles qProfiles, ActiveRules activeRules, String language) {
    QProfile qProfile = qProfiles.findByLanguage(language);
    if (qProfile != null) {
      return new RulesProfileWrapper(select(qProfile, activeRules));
    }
    return new RulesProfileWrapper(new ArrayList<>());
  }

  private static RulesProfile loadProfiles(ModuleQProfiles qProfiles, ActiveRules activeRules) {
    Collection<QProfile> profiles = qProfiles.findAll();
    Collection<RulesProfile> dtos = new ArrayList<>(profiles.size());
    for (QProfile qProfile : profiles) {
      dtos.add(select(qProfile, activeRules));
    }
    return new RulesProfileWrapper(dtos);
  }

  private static RulesProfile select(QProfile qProfile, ActiveRules activeRules) {
    RulesProfile deprecatedProfile = new RulesProfile();
    deprecatedProfile.setName(qProfile.getName());
    deprecatedProfile.setLanguage(qProfile.getLanguage());
    for (org.sonar.api.batch.rule.ActiveRule activeRule : activeRules.findByLanguage(qProfile.getLanguage())) {
      Rule rule = Rule.create(activeRule.ruleKey().repository(), activeRule.ruleKey().rule());
      rule.setConfigKey(activeRule.internalKey());

      // SONAR-6706
      if (activeRule.templateRuleKey() != null) {
        rule.setTemplate(Rule.create(activeRule.ruleKey().repository(), activeRule.templateRuleKey()));
      }

      ActiveRule deprecatedActiveRule = deprecatedProfile.activateRule(rule,
        RulePriority.valueOf(activeRule.severity()));
      for (Map.Entry<String, String> param : activeRule.params().entrySet()) {
        rule.createParameter(param.getKey());
        deprecatedActiveRule.setParameter(param.getKey(), param.getValue());
      }
    }
    return deprecatedProfile;
  }

}
