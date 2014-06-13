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

import com.google.common.base.Strings;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;

public class RuleCreator implements ServerComponent {

  private final DbClient dbClient;

  public RuleCreator(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public RuleKey create(NewRule newRule) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      RuleKey templateKey = newRule.templateKey();
      if (templateKey != null) {
        RuleDto templateRule = dbClient.ruleDao().getByKey(dbSession, templateKey);
        if (!templateRule.isTemplate()) {
          throw new IllegalArgumentException("This rule is not a template rule: " + templateKey.toString());
        }
        validateRule(newRule);

        RuleKey customRuleKey = RuleKey.of(templateRule.getRepositoryKey(), newRule.ruleKey());
        checkRuleKeyUnicity(customRuleKey, dbSession);
        createCustomRule(customRuleKey, newRule, templateRule, dbSession);
        dbSession.commit();
        return customRuleKey;
      }
      throw new IllegalArgumentException("Not supported");
    } finally {
      dbSession.close();
    }
  }

  private static void validateRule(NewRule newRule) {
    validateRuleKey(newRule.ruleKey());
    if (Strings.isNullOrEmpty(newRule.name())) {
      throw new IllegalArgumentException("The name is missing");
    }
    if (Strings.isNullOrEmpty(newRule.htmlDescription())) {
      throw new IllegalArgumentException("The description is missing");
    }
    String severity = newRule.severity();
    if (Strings.isNullOrEmpty(severity)) {
      throw new IllegalArgumentException("The severity is missing");
    } else if (!Severity.ALL.contains(severity)) {
      throw new IllegalArgumentException("This severity is invalid : " + severity);
    }
    if (newRule.status() == null) {
      throw new IllegalArgumentException("The status is missing");
    }
  }

  private static void validateRuleKey(String ruleKey) {
    if (Strings.isNullOrEmpty(ruleKey)) {
      throw new IllegalArgumentException("The rule key is missing");
    } else {
      if (!ruleKey.matches("^[\\w]+$")) {
        throw new IllegalArgumentException(String.format("The rule key '%s' is invalid, it should only contains : a-z, 0-9, '_'", ruleKey));
      }
    }
  }

  private void checkRuleKeyUnicity(RuleKey ruleKey, DbSession dbSession){
    if (dbClient.ruleDao().getNullableByKey(dbSession, ruleKey) != null) {
      throw new IllegalArgumentException(String.format("A rule with the key '%s' already exits", ruleKey.rule()));
    }
  }

  private RuleKey createCustomRule(RuleKey ruleKey, NewRule newRule, RuleDto templateRuleDto, DbSession dbSession){
    RuleDto ruleDto = RuleDto.createFor(ruleKey)
      .setTemplateId(templateRuleDto.getId())
      .setConfigKey(templateRuleDto.getConfigKey())
      .setName(newRule.name())
      .setDescription(newRule.htmlDescription())
      .setSeverity(newRule.severity())
      .setStatus(newRule.status())
      .setLanguage(templateRuleDto.getLanguage())
      .setDefaultSubCharacteristicId(templateRuleDto.getDefaultSubCharacteristicId())
      .setDefaultRemediationFunction(templateRuleDto.getDefaultRemediationFunction())
      .setDefaultRemediationCoefficient(templateRuleDto.getDefaultRemediationCoefficient())
      .setDefaultRemediationOffset(templateRuleDto.getDefaultRemediationOffset())
      .setEffortToFixDescription(templateRuleDto.getEffortToFixDescription())
      .setTags(templateRuleDto.getTags())
      .setSystemTags(templateRuleDto.getSystemTags());
    dbClient.ruleDao().insert(dbSession, ruleDto);

    for (RuleParamDto templateRuleParamDto : dbClient.ruleDao().findRuleParamsByRuleKey(dbSession, templateRuleDto.getKey())) {
      String newRuleParam = newRule.parameter(templateRuleParamDto.getName());
      if (newRuleParam != null) {
        createCustomRuleParams(newRuleParam, ruleDto, templateRuleParamDto, dbSession);
      }
    }
    return ruleKey;
  }

  private void createCustomRuleParams(String param, RuleDto ruleDto, RuleParamDto templateRuleParam, DbSession dbSession){
    RuleParamDto ruleParamDto = RuleParamDto.createFor(ruleDto)
      .setName(templateRuleParam.getName())
      .setType(templateRuleParam.getType())
      .setDescription(templateRuleParam.getDescription())
      .setDefaultValue(param);
    dbClient.ruleDao().addRuleParam(dbSession, ruleDto, ruleParamDto);
  }

}
