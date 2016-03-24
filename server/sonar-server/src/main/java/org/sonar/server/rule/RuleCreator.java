/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.rule;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.Message;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.util.TypeValidations;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class RuleCreator {

  private final System2 system2;
  private final RuleIndexer ruleIndexer;
  private final DbClient dbClient;
  private final TypeValidations typeValidations;

  public RuleCreator(System2 system2, RuleIndexer ruleIndexer, DbClient dbClient, TypeValidations typeValidations) {
    this.system2 = system2;
    this.ruleIndexer = ruleIndexer;
    this.dbClient = dbClient;
    this.typeValidations = typeValidations;
  }

  public RuleKey create(NewCustomRule newRule) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return createCustomRule(newRule, dbSession);
    } finally {
      dbSession.close();
    }
  }

  private RuleKey createCustomRule(NewCustomRule newRule, DbSession dbSession) {
    RuleKey templateKey = newRule.templateKey();
    if (templateKey == null) {
      throw new IllegalArgumentException("Rule template key should not be null");
    }
    RuleDto templateRule = dbClient.ruleDao().selectOrFailByKey(dbSession, templateKey);
    if (!templateRule.isTemplate()) {
      throw new IllegalArgumentException("This rule is not a template rule: " + templateKey.toString());
    }
    validateCustomRule(newRule, dbSession, templateKey);

    RuleKey customRuleKey = RuleKey.of(templateRule.getRepositoryKey(), newRule.ruleKey());

    Optional<RuleDto> existingRule = loadRule(customRuleKey, dbSession);
    if (existingRule.isPresent()) {
      updateExistingRule(existingRule.get(), newRule, dbSession);
    } else {
      createCustomRule(customRuleKey, newRule, templateRule, dbSession);
    }

    dbSession.commit();
    ruleIndexer.setEnabled(true).index();
    return customRuleKey;
  }

  private void validateCustomRule(NewCustomRule newRule, DbSession dbSession, RuleKey templateKey) {
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

    for (RuleParamDto ruleParam : dbClient.ruleDao().selectRuleParamsByRuleKey(dbSession, templateKey)) {
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

  private static void validateName(Errors errors, NewCustomRule newRule) {
    if (Strings.isNullOrEmpty(newRule.name())) {
      errors.add(Message.of("coding_rules.validation.missing_name"));
    }
  }

  private static void validateDescription(Errors errors, NewCustomRule newRule) {
    if (Strings.isNullOrEmpty(newRule.htmlDescription()) && Strings.isNullOrEmpty(newRule.markdownDescription())) {
      errors.add(Message.of("coding_rules.validation.missing_description"));
    }
  }

  private static void validateRuleKey(Errors errors, String ruleKey) {
    if (!ruleKey.matches("^[\\w]+$")) {
      errors.add(Message.of("coding_rules.validation.invalid_rule_key", ruleKey));
    }
  }

  private Optional<RuleDto> loadRule(RuleKey ruleKey, DbSession dbSession) {
    return dbClient.ruleDao().selectByKey(dbSession, ruleKey);
  }

  private RuleKey createCustomRule(RuleKey ruleKey, NewCustomRule newRule, RuleDto templateRuleDto, DbSession dbSession) {
    RuleDto ruleDto = RuleDto.createFor(ruleKey)
      .setTemplateId(templateRuleDto.getId())
      .setConfigKey(templateRuleDto.getConfigKey())
      .setName(newRule.name())
      .setDescription(newRule.markdownDescription())
      .setDescriptionFormat(Format.MARKDOWN)
      .setSeverity(newRule.severity())
      .setStatus(newRule.status())
      .setLanguage(templateRuleDto.getLanguage())
      .setDefaultRemediationFunction(templateRuleDto.getDefaultRemediationFunction())
      .setDefaultRemediationGapMultiplier(templateRuleDto.getDefaultRemediationGapMultiplier())
      .setDefaultRemediationBaseEffort(templateRuleDto.getDefaultRemediationBaseEffort())
      .setGapDescription(templateRuleDto.getGapDescription())
      .setTags(templateRuleDto.getTags())
      .setSystemTags(templateRuleDto.getSystemTags())
      .setType(templateRuleDto.getType())
      .setCreatedAt(system2.now())
      .setUpdatedAt(system2.now());
    dbClient.ruleDao().insert(dbSession, ruleDto);

    for (RuleParamDto templateRuleParamDto : dbClient.ruleDao().selectRuleParamsByRuleKey(dbSession, templateRuleDto.getKey())) {
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
    dbClient.ruleDao().insertRuleParam(dbSession, ruleDto, ruleParamDto);
  }

  private void updateExistingRule(RuleDto ruleDto, NewCustomRule newRule, DbSession dbSession) {
    if (ruleDto.getStatus().equals(RuleStatus.REMOVED)) {
      if (newRule.isPreventReactivation()) {
        throw new ReactivationException(String.format("A removed rule with the key '%s' already exists", ruleDto.getKey().rule()), ruleDto.getKey());
      } else {
        ruleDto.setStatus(RuleStatus.READY)
          .setUpdatedAt(system2.now());
        dbClient.ruleDao().update(dbSession, ruleDto);
      }
    } else {
      throw new IllegalArgumentException(String.format("A rule with the key '%s' already exists", ruleDto.getKey().rule()));
    }
  }

}
