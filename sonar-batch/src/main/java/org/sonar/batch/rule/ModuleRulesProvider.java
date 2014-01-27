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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.rule.ModuleRules;
import org.sonar.api.batch.rule.internal.ModuleRulesBuilder;
import org.sonar.api.batch.rule.internal.NewModuleRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;

/**
 * Loads the rules that are activated on the Quality profiles
 * used by the current module and build {@link org.sonar.api.batch.rule.ModuleRules}.
 */
public class ModuleRulesProvider extends ProviderAdapter {

  private ModuleRules singleton = null;

  public ModuleRules provide(ModuleQProfiles qProfiles, ActiveRuleDao dao, RuleFinder ruleFinder) {
    if (singleton == null) {
      singleton = load(qProfiles, dao, ruleFinder);
    }
    return singleton;
  }

  private ModuleRules load(ModuleQProfiles qProfiles, ActiveRuleDao dao, RuleFinder ruleFinder) {
    ModuleRulesBuilder builder = new ModuleRulesBuilder();
    for (ModuleQProfiles.QProfile qProfile : qProfiles.findAll()) {
      ListMultimap<Integer, ActiveRuleParamDto> paramDtosByActiveRuleId = ArrayListMultimap.create();
      for (ActiveRuleParamDto dto : dao.selectParamsByProfileId(qProfile.id())) {
        paramDtosByActiveRuleId.put(dto.getActiveRuleId(), dto);
      }

      for (ActiveRuleDto activeDto : dao.selectByProfileId(qProfile.id())) {
        Rule rule = ruleFinder.findById(activeDto.getRulId());
        if (rule != null) {
          NewModuleRule newModuleRule = builder.activate(rule.ruleKey());
          newModuleRule.setSeverity(activeDto.getSeverityString());
          for (ActiveRuleParamDto paramDto : paramDtosByActiveRuleId.get(activeDto.getId())) {
            newModuleRule.setParam(paramDto.getKey(), paramDto.getValue());
          }
          // TODO add default values declared on rule parameters
        }
      }
    }
    return builder.build();
  }
}
