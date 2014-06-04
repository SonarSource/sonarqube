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

package org.sonar.server.rule;

import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.index.RuleIndex;

public class RuleCreator implements ServerComponent {

  private final RuleIndex index;
  private final DbClient dbClient;
  private final System2 system;

  public RuleCreator(RuleIndex index, DbClient dbClient, System2 system) {
    this.index = index;
    this.dbClient = dbClient;
    this.system = system;
  }

  public Rule create(NewRule newRule) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      RuleKey templateKey = newRule.templateKey();
      if (templateKey != null) {
        RuleDto templateRule = dbClient.ruleDao().getByKey(dbSession, newRule.templateKey());
        if (templateRule == null) {
          throw new IllegalArgumentException("Template rule does not exists: " + templateKey.toString());
        }
        if (!Cardinality.MULTIPLE.equals(templateRule.getCardinality())) {
          throw new IllegalArgumentException("This rule is not a template rule: " + templateKey.toString());
        }
        RuleKey customRuleKey = createCustomRule(newRule, templateRule, dbSession);
        dbSession.commit();
        return index.getByKey(customRuleKey);
      }
      throw new IllegalArgumentException("Not supported");
    } finally {
      dbSession.close();
    }
  }

  private RuleKey createCustomRule(NewRule newRule, RuleDto templateRuleDto, DbSession dbSession){
    RuleKey ruleKey = RuleKey.of(templateRuleDto.getRepositoryKey(), templateRuleDto.getRuleKey() + "_" + system.now());
    RuleDto ruleDto = RuleDto.createFor(ruleKey)
      .setConfigKey(templateRuleDto.getConfigKey())
      .setName(newRule.name())
      .setDescription(newRule.htmlDescription())
      .setSeverity(newRule.severity())
      .setCardinality(Cardinality.SINGLE)
      .setStatus(newRule.status())
      .setLanguage(templateRuleDto.getLanguage())
      .setDefaultSubCharacteristicId(templateRuleDto.getDefaultSubCharacteristicId())
      .setDefaultRemediationFunction(templateRuleDto.getDefaultRemediationFunction())
      .setDefaultRemediationCoefficient(templateRuleDto.getDefaultRemediationCoefficient())
      .setDefaultRemediationOffset(templateRuleDto.getDefaultRemediationOffset())
      .setEffortToFixDescription(templateRuleDto.getEffortToFixDescription());
    dbClient.ruleDao().insert(dbSession, ruleDto);

    // TODO add tags from template rule

    for (RuleParamDto templateRuleParamDto : dbClient.ruleDao().findRuleParamsByRuleKey(dbSession, templateRuleDto.getKey())) {
      NewRuleParam newRuleParam = newRule.param(templateRuleParamDto.getName());
      if (newRuleParam == null) {
        throw new IllegalArgumentException(String.format("The parameter '%s' has not been set", templateRuleParamDto.getName()));
      }
      createCustomRuleParams(newRuleParam, ruleDto, templateRuleParamDto, dbSession);
    }
    return ruleKey;
  }

  private void createCustomRuleParams(NewRuleParam param, RuleDto ruleDto, RuleParamDto templateRuleParam, DbSession dbSession){
    RuleParamDto ruleParamDto = RuleParamDto.createFor(ruleDto)
      .setName(param.key())
      .setType(templateRuleParam.getType())
      .setDescription(templateRuleParam.getDescription())
      .setDefaultValue(param.defaultValue());
    dbClient.ruleDao().addRuleParam(dbSession, ruleDto, ruleParamDto);
  }

}
