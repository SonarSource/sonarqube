/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
import org.codehaus.plexus.util.StringUtils;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleParam;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;

import javax.annotation.CheckForNull;

/**
 * Loads the rules that are activated on the Quality profiles
 * used by the current module and build {@link org.sonar.api.batch.rule.ActiveRules}.
 */
public class ActiveRulesProvider extends ProviderAdapter {

  private ActiveRules singleton = null;

  public ActiveRules provide(ModuleQProfiles qProfiles, ActiveRuleDao dao, RuleFinder ruleFinder) {
    if (singleton == null) {
      singleton = load(qProfiles, dao, ruleFinder);
    }
    return singleton;
  }

  private ActiveRules load(ModuleQProfiles qProfiles, ActiveRuleDao dao, RuleFinder ruleFinder) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (ModuleQProfiles.QProfile qProfile : qProfiles.findAll()) {
      ListMultimap<Integer, ActiveRuleParamDto> paramDtosByActiveRuleId = ArrayListMultimap.create();
      for (ActiveRuleParamDto dto : dao.selectParamsByProfileId(qProfile.id())) {
        paramDtosByActiveRuleId.put(dto.getActiveRuleId(), dto);
      }

      for (ActiveRuleDto activeDto : dao.selectByProfileId(qProfile.id())) {
        Rule rule = ruleFinder.findById(activeDto.getRulId());
        if (rule != null) {
          NewActiveRule newActiveRule = builder.activate(rule.ruleKey());
          newActiveRule.setSeverity(activeDto.getSeverityString());
          if (rule.getParent() != null) {
            newActiveRule.setInternalKey(rule.getParent().getConfigKey());
          } else {
            newActiveRule.setInternalKey(rule.getConfigKey());
          }

          // load parameter values
          for (ActiveRuleParamDto paramDto : paramDtosByActiveRuleId.get(activeDto.getId())) {
            newActiveRule.setParam(paramDto.getKey(), paramDto.getValue());
          }

          // load default values
          for (RuleParam param : rule.getParams()) {
            if (!newActiveRule.params().containsKey(param.getKey())) {
              newActiveRule.setParam(param.getKey(), param.getDefaultValue());
            }
          }
        }
      }
    }
    return builder.build();
  }

  @CheckForNull
  private String defaultParamValue(Rule rule, String paramKey) {
    RuleParam param = rule.getParam(paramKey);
    return param != null ? param.getDefaultValue() : null;
  }
}
