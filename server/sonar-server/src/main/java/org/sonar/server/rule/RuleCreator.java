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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleDto.Format;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.Message;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.util.TypeValidations;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class RuleCreator {

  private final DbClient dbClient;

  private final TypeValidations typeValidations;

  public RuleCreator(DbClient dbClient, TypeValidations typeValidations) {
    this.dbClient = dbClient;
    this.typeValidations = typeValidations;
  }

  public RuleKey create(NewRule newRule) {
    DbSession dbSession = dbClient.openSession(false);
    try {

      if (newRule.isCustom()) {
        return createCustomRule(newRule, dbSession);
      }

      if (newRule.isManual()) {
        return createManualRule(newRule, dbSession);
      }

      throw new IllegalStateException("Only custom rule and manual rule can be created");
    } finally {
      dbSession.close();
    }
  }

  private RuleKey createCustomRule(NewRule newRule, DbSession dbSession) {
    RuleKey templateKey = newRule.templateKey();
    if (templateKey == null) {
      throw new IllegalArgumentException("Rule template key should not be null");
    }
    RuleDto templateRule = dbClient.ruleDao().getByKey(dbSession, templateKey);
    if (!templateRule.isTemplate()) {
      throw new IllegalArgumentException("This rule is not a template rule: " + templateKey.toString());
    }
    validateCustomRule(newRule, dbSession, templateKey);

    RuleKey customRuleKey = RuleKey.of(templateRule.getRepositoryKey(), newRule.ruleKey());

    RuleDto existingRule = loadRule(customRuleKey, dbSession);
    if (existingRule != null) {
      updateExistingRule(existingRule, newRule, dbSession);
    } else {
      createCustomRule(customRuleKey, newRule, templateRule, dbSession);
    }

    dbSession.commit();
    return customRuleKey;
  }

  private RuleKey createManualRule(NewRule newRule, DbSession dbSession) {
    validateManualRule(newRule);

    RuleKey customRuleKey = RuleKey.of(RuleDoc.MANUAL_REPOSITORY, newRule.ruleKey());
    RuleDto existingRule = loadRule(customRuleKey, dbSession);
    if (existingRule != null) {
      updateExistingRule(existingRule, newRule, dbSession);
    } else {
      createManualRule(customRuleKey, newRule, dbSession);
    }

    dbSession.commit();
    return customRuleKey;
  }

  private void validateCustomRule(NewRule newRule, DbSession dbSession, RuleKey templateKey) {
    Errors errors = new Errors();

    validateRuleKey(errors, newRule.ruleKey());
    validateName(errors, newRule);
    validateDescription(errors, newRule);

    String severity = newRule.severity();
    if (Strings.isNullOrEmpty(severity)) {
      errors.add(Message.of("coding_rules.validation.missing_severity"));
    } else if (!Severity.ALL.contains(severity)) {
      errors.add(Message.of("coding_rules.validation.invalid_severity", severity));
    }
    if (newRule.status() == null) {
      errors.add(Message.of("coding_rules.validation.missing_status"));
    }

    for (RuleParamDto ruleParam : dbClient.ruleDao().findRuleParamsByRuleKey(dbSession, templateKey)) {
      try {
        validateParam(ruleParam, newRule.parameter(ruleParam.getName()));
      } catch (BadRequestException validationError) {
        errors.add(validationError.errors());
      }
    }

    if (!errors.isEmpty()) {
      throw new BadRequestException(errors);
    }
  }

  @CheckForNull
  private void validateParam(RuleParamDto ruleParam, @Nullable String value) {
    if (value != null) {
      RuleParamType ruleParamType = RuleParamType.parse(ruleParam.getType());
      if (ruleParamType.multiple()) {
        List<String> values = newArrayList(Splitter.on(",").split(value));
        typeValidations.validate(values, ruleParamType.type(), ruleParamType.values());
      } else {
        typeValidations.validate(value, ruleParamType.type(), ruleParamType.values());
      }
    }
  }

  private static void validateManualRule(NewRule newRule) {
    Errors errors = new Errors();
    validateRuleKey(errors, newRule.ruleKey());
    validateName(errors, newRule);
    validateDescription(errors, newRule);

    if (!newRule.parameters().isEmpty()) {
      errors.add(Message.of("coding_rules.validation.manual_rule_params"));
    }

    if (!errors.isEmpty()) {
      throw new BadRequestException(errors);
    }
  }

  private static void validateName(Errors errors, NewRule newRule) {
    if (Strings.isNullOrEmpty(newRule.name())) {
      errors.add(Message.of("coding_rules.validation.missing_name"));
    }
  }

  private static void validateDescription(Errors errors, NewRule newRule) {
    if (Strings.isNullOrEmpty(newRule.htmlDescription()) && Strings.isNullOrEmpty(newRule.markdownDescription())) {
      errors.add(Message.of("coding_rules.validation.missing_description"));
    }
  }

  private static void validateRuleKey(Errors errors, String ruleKey) {
    if (!ruleKey.matches("^[\\w]+$")) {
      errors.add(Message.of("coding_rules.validation.invalid_rule_key", ruleKey));
    }
  }

  @CheckForNull
  private RuleDto loadRule(RuleKey ruleKey, DbSession dbSession) {
    return dbClient.ruleDao().getNullableByKey(dbSession, ruleKey);
  }

  private RuleKey createCustomRule(RuleKey ruleKey, NewRule newRule, RuleDto templateRuleDto, DbSession dbSession) {
    RuleDto ruleDto = RuleDto.createFor(ruleKey)
      .setTemplateId(templateRuleDto.getId())
      .setConfigKey(templateRuleDto.getConfigKey())
      .setName(newRule.name())
      .setDescription(newRule.markdownDescription())
      .setDescriptionFormat(Format.MARKDOWN)
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
      String customRuleParamValue = Strings.emptyToNull(newRule.parameter(templateRuleParamDto.getName()));
      createCustomRuleParams(customRuleParamValue, ruleDto, templateRuleParamDto, dbSession);
    }
    return ruleKey;
  }

  private void createCustomRuleParams(@Nullable String paramValue, RuleDto ruleDto, RuleParamDto templateRuleParam, DbSession dbSession) {
    RuleParamDto ruleParamDto = RuleParamDto.createFor(ruleDto)
      .setName(templateRuleParam.getName())
      .setType(templateRuleParam.getType())
      .setDescription(templateRuleParam.getDescription())
      .setDefaultValue(paramValue);
    dbClient.ruleDao().addRuleParam(dbSession, ruleDto, ruleParamDto);
  }

  private RuleKey createManualRule(RuleKey ruleKey, NewRule newRule, DbSession dbSession) {
    RuleDto ruleDto = RuleDto.createFor(ruleKey)
      .setName(newRule.name())
      .setDescription(newRule.markdownDescription())
      .setDescriptionFormat(Format.MARKDOWN)
      .setSeverity(newRule.severity())
      .setStatus(RuleStatus.READY);
    dbClient.ruleDao().insert(dbSession, ruleDto);
    return ruleKey;
  }

  private void updateExistingRule(RuleDto ruleDto, NewRule newRule, DbSession dbSession) {
    if (ruleDto.getStatus().equals(RuleStatus.REMOVED)) {
      if (newRule.isPreventReactivation()) {
        throw new ReactivationException(String.format("A removed rule with the key '%s' already exists", ruleDto.getKey().rule()), ruleDto.getKey());
      } else {
        ruleDto.setStatus(RuleStatus.READY);
        dbClient.ruleDao().update(dbSession, ruleDto);
      }
    } else {
      throw new IllegalArgumentException(String.format("A rule with the key '%s' already exists", ruleDto.getKey().rule()));
    }
  }

}
