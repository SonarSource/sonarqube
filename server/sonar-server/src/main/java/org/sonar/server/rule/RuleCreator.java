/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.rule;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.util.TypeValidations;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.sonar.server.ws.WsUtils.checkRequest;

@ServerSide
public class RuleCreator {

  private final System2 system2;
  private final RuleIndexer ruleIndexer;
  private final DbClient dbClient;
  private final TypeValidations typeValidations;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public RuleCreator(System2 system2, RuleIndexer ruleIndexer, DbClient dbClient, TypeValidations typeValidations, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.system2 = system2;
    this.ruleIndexer = ruleIndexer;
    this.dbClient = dbClient;
    this.typeValidations = typeValidations;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public RuleKey create(DbSession dbSession, NewCustomRule newRule) {
    RuleKey templateKey = newRule.templateKey();
    checkArgument(templateKey != null, "Rule template key should not be null");
    String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
    OrganizationDto defaultOrganization = dbClient.organizationDao().selectByUuid(dbSession, defaultOrganizationUuid)
      .orElseThrow(() -> new IllegalStateException(format("Could not find default organization for uuid '%s'", defaultOrganizationUuid)));
    RuleDto templateRule = dbClient.ruleDao().selectByKey(dbSession, defaultOrganization, templateKey)
      .orElseThrow(() -> new IllegalArgumentException(format("The template key doesn't exist: %s", templateKey)));
    checkArgument(templateRule.isTemplate(), "This rule is not a template rule: %s", templateKey.toString());
    checkArgument(templateRule.getStatus() != RuleStatus.REMOVED, "The template key doesn't exist: %s", templateKey.toString());
    validateCustomRule(newRule, dbSession, templateKey);

    RuleKey customRuleKey = RuleKey.of(templateRule.getRepositoryKey(), newRule.ruleKey());
    Optional<RuleDefinitionDto> definition = loadRule(dbSession, customRuleKey);
    int customRuleId = definition.map(d -> updateExistingRule(d, newRule, dbSession))
      .orElseGet(() -> createCustomRule(customRuleKey, newRule, templateRule, dbSession));

    ruleIndexer.commitAndIndex(dbSession, customRuleId);
    return customRuleKey;
  }

  private void validateCustomRule(NewCustomRule newRule, DbSession dbSession, RuleKey templateKey) {
    List<String> errors = new ArrayList<>();

    validateRuleKey(errors, newRule.ruleKey());
    validateName(errors, newRule);
    validateDescription(errors, newRule);

    String severity = newRule.severity();
    if (Strings.isNullOrEmpty(severity)) {
      errors.add("The severity is missing");
    } else if (!Severity.ALL.contains(severity)) {
      errors.add(format("Severity \"%s\" is invalid", severity));
    }
    if (newRule.status() == null) {
      errors.add("The status is missing");
    }

    for (RuleParamDto ruleParam : dbClient.ruleDao().selectRuleParamsByRuleKey(dbSession, templateKey)) {
      try {
        validateParam(ruleParam, newRule.parameter(ruleParam.getName()));
      } catch (BadRequestException validationError) {
        errors.addAll(validationError.errors());
      }
    }
    checkRequest(errors.isEmpty(), errors);
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

  private static void validateName(List<String> errors, NewCustomRule newRule) {
    if (Strings.isNullOrEmpty(newRule.name())) {
      errors.add("The name is missing");
    }
  }

  private static void validateDescription(List<String> errors, NewCustomRule newRule) {
    if (Strings.isNullOrEmpty(newRule.htmlDescription()) && Strings.isNullOrEmpty(newRule.markdownDescription())) {
      errors.add("The description is missing");
    }
  }

  private static void validateRuleKey(List<String> errors, String ruleKey) {
    if (!ruleKey.matches("^[\\w]+$")) {
      errors.add(format("The rule key \"%s\" is invalid, it should only contain: a-z, 0-9, \"_\"", ruleKey));
    }
  }

  private Optional<RuleDefinitionDto> loadRule(DbSession dbSession, RuleKey ruleKey) {
    return dbClient.ruleDao().selectDefinitionByKey(dbSession, ruleKey);
  }

  private int createCustomRule(RuleKey ruleKey, NewCustomRule newRule, RuleDto templateRuleDto, DbSession dbSession) {
    RuleDefinitionDto ruleDefinition = new RuleDefinitionDto()
      .setRuleKey(ruleKey)
      .setPluginKey(templateRuleDto.getPluginKey())
      .setTemplateId(templateRuleDto.getId())
      .setConfigKey(templateRuleDto.getConfigKey())
      .setName(newRule.name())
      .setDescription(newRule.markdownDescription())
      .setDescriptionFormat(Format.MARKDOWN)
      .setSeverity(newRule.severity())
      .setStatus(newRule.status())
      .setType(newRule.type() == null ? templateRuleDto.getType() : newRule.type().getDbConstant())
      .setLanguage(templateRuleDto.getLanguage())
      .setDefRemediationFunction(templateRuleDto.getDefRemediationFunction())
      .setDefRemediationGapMultiplier(templateRuleDto.getDefRemediationGapMultiplier())
      .setDefRemediationBaseEffort(templateRuleDto.getDefRemediationBaseEffort())
      .setGapDescription(templateRuleDto.getGapDescription())
      .setScope(templateRuleDto.getScope())
      .setSystemTags(templateRuleDto.getSystemTags())
      .setSecurityStandards(templateRuleDto.getSecurityStandards())
      .setIsExternal(false)
      .setIsAdHoc(false)
      .setCreatedAt(system2.now())
      .setUpdatedAt(system2.now());
    dbClient.ruleDao().insert(dbSession, ruleDefinition);

    Set<String> tags = templateRuleDto.getTags();
    if (!tags.isEmpty()) {
      RuleMetadataDto ruleMetadata = new RuleMetadataDto()
        .setOrganizationUuid(defaultOrganizationProvider.get().getUuid())
        .setRuleId(ruleDefinition.getId())
        .setTags(tags)
        .setCreatedAt(system2.now())
        .setUpdatedAt(system2.now());
      dbClient.ruleDao().insertOrUpdate(dbSession, ruleMetadata);
    }

    for (RuleParamDto templateRuleParamDto : dbClient.ruleDao().selectRuleParamsByRuleKey(dbSession, templateRuleDto.getKey())) {
      String customRuleParamValue = Strings.emptyToNull(newRule.parameter(templateRuleParamDto.getName()));
      createCustomRuleParams(customRuleParamValue, ruleDefinition, templateRuleParamDto, dbSession);
    }
    return ruleDefinition.getId();
  }

  private void createCustomRuleParams(@Nullable String paramValue, RuleDefinitionDto ruleDto, RuleParamDto templateRuleParam, DbSession dbSession) {
    RuleParamDto ruleParamDto = RuleParamDto.createFor(ruleDto)
      .setName(templateRuleParam.getName())
      .setType(templateRuleParam.getType())
      .setDescription(templateRuleParam.getDescription())
      .setDefaultValue(paramValue);
    dbClient.ruleDao().insertRuleParam(dbSession, ruleDto, ruleParamDto);
  }

  private int updateExistingRule(RuleDefinitionDto ruleDto, NewCustomRule newRule, DbSession dbSession) {
    if (ruleDto.getStatus().equals(RuleStatus.REMOVED)) {
      if (newRule.isPreventReactivation()) {
        throw new ReactivationException(format("A removed rule with the key '%s' already exists", ruleDto.getKey().rule()), ruleDto.getKey());
      } else {
        ruleDto.setStatus(RuleStatus.READY)
          .setUpdatedAt(system2.now());
        dbClient.ruleDao().update(dbSession, ruleDto);
      }
    } else {
      throw new IllegalArgumentException(format("A rule with the key '%s' already exists", ruleDto.getKey().rule()));
    }
    return ruleDto.getId();
  }

}
