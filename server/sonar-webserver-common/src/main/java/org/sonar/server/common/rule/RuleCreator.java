/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.common.rule;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.internal.ImpactMapper;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.common.rule.service.NewCustomRule;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.util.TypeValidations;
import org.springframework.util.CollectionUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

@ServerSide
public class RuleCreator {
  private static final String TEMPLATE_KEY_NOT_EXIST_FORMAT = "The template key doesn't exist: %s";
  private final System2 system2;
  private final RuleIndexer ruleIndexer;
  private final DbClient dbClient;
  private final TypeValidations typeValidations;
  private final UuidFactory uuidFactory;

  public RuleCreator(System2 system2, RuleIndexer ruleIndexer, DbClient dbClient, TypeValidations typeValidations, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.ruleIndexer = ruleIndexer;
    this.dbClient = dbClient;
    this.typeValidations = typeValidations;
    this.uuidFactory = uuidFactory;
  }

  public RuleDto create(DbSession dbSession, NewCustomRule newRule) {
    RuleKey templateKey = newRule.templateKey();
    RuleDto templateRule = dbClient.ruleDao().selectByKey(dbSession, templateKey)
      .orElseThrow(() -> new IllegalArgumentException(format(TEMPLATE_KEY_NOT_EXIST_FORMAT, templateKey)));
    checkArgument(templateRule.isTemplate(), "This rule is not a template rule: %s", templateKey.toString());
    checkArgument(templateRule.getStatus() != RuleStatus.REMOVED, TEMPLATE_KEY_NOT_EXIST_FORMAT, templateKey.toString());
    validateCustomRule(newRule, dbSession, templateKey);

    Optional<RuleDto> definition = loadRule(dbSession, newRule.ruleKey());
    RuleDto ruleDto = definition.map(dto -> updateExistingRule(dto, newRule, dbSession))
      .orElseGet(() -> createCustomRule(newRule, templateRule, dbSession));

    ruleIndexer.commitAndIndex(dbSession, ruleDto.getUuid());
    return ruleDto;
  }

  public List<RuleDto> create(DbSession dbSession, List<NewCustomRule> newRules) {
    Set<RuleKey> templateKeys = newRules.stream().map(NewCustomRule::templateKey).collect(Collectors.toSet());
    Map<RuleKey, RuleDto> templateRules = dbClient.ruleDao().selectByKeys(dbSession, templateKeys)
      .stream()
      .collect(Collectors.toMap(
        RuleDto::getKey,
        Function.identity()));

    checkArgument(!templateRules.isEmpty() && templateKeys.size() == templateRules.size(), "Rule template keys should exists for each custom rule!");
    templateRules.values().forEach(ruleDto -> {
      checkArgument(ruleDto.isTemplate(), "This rule is not a template rule: %s", ruleDto.getKey().toString());
      checkArgument(ruleDto.getStatus() != RuleStatus.REMOVED, TEMPLATE_KEY_NOT_EXIST_FORMAT, ruleDto.getKey().toString());
    });

    List<RuleDto> customRules = newRules.stream()
      .map(newCustomRule -> {
        RuleDto templateRule = templateRules.get(newCustomRule.templateKey());
        validateCustomRule(newCustomRule, dbSession, templateRule.getKey());
        return createCustomRule(newCustomRule, templateRule, dbSession);
      })
      .toList();

    ruleIndexer.commitAndIndex(dbSession, customRules.stream().map(RuleDto::getUuid).toList());
    return customRules;
  }

  private void validateCustomRule(NewCustomRule newRule, DbSession dbSession, RuleKey templateKey) {
    List<String> errors = new ArrayList<>();

    validateRuleKey(errors, newRule.ruleKey(), templateKey);
    validateName(errors, newRule);
    validateDescription(errors, newRule);

    if (newRule.status() == RuleStatus.REMOVED) {
      errors.add(format("Rule status '%s' is not allowed", RuleStatus.REMOVED));
    }
    String severity = newRule.severity();
    if (severity != null && !Severity.ALL.contains(severity)) {
      errors.add(format("Severity \"%s\" is invalid", severity));
    }
    if (!CollectionUtils.isEmpty(newRule.getImpacts()) && (StringUtils.isNotBlank(newRule.severity()) || newRule.type() != null)) {
      errors.add("The rule cannot have both impacts and type/severity specified");
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
    if (Strings.isNullOrEmpty(newRule.markdownDescription())) {
      errors.add("The description is missing");
    }
  }

  private static void validateRuleKey(List<String> errors, RuleKey ruleKey, RuleKey templateKey) {
    if (!ruleKey.repository().equals(templateKey.repository())) {
      errors.add("Custom and template keys must be in the same repository");
    }
  }

  private Optional<RuleDto> loadRule(DbSession dbSession, RuleKey ruleKey) {
    return dbClient.ruleDao().selectByKey(dbSession, ruleKey);
  }

  private RuleDto createCustomRule(NewCustomRule newRule, RuleDto templateRuleDto, DbSession dbSession) {
    RuleDescriptionSectionDto ruleDescriptionSectionDto = createDefaultRuleDescriptionSection(uuidFactory.create(), requireNonNull(newRule.markdownDescription()));
    OrganizationDto organizationDto = dbClient.organizationDao().selectByKey(dbSession, newRule.getOrganizationKey())
            .orElseThrow(() -> new NotFoundException("No organization with key " + newRule.getOrganizationKey()));
    RuleDto ruleDto = new RuleDto()
      .setUuid(uuidFactory.create())
      .setRuleKey(newRule.ruleKey())
      .setPluginKey(templateRuleDto.getPluginKey())
      .setTemplateUuid(templateRuleDto.getUuid())
      .setConfigKey(templateRuleDto.getConfigKey())
      .setName(newRule.name())
      .setOrganizationUuid(organizationDto.getUuid())
      .setStatus(ofNullable(newRule.status()).orElse(RuleStatus.READY))
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
      .setUpdatedAt(system2.now())
      .setDescriptionFormat(Format.MARKDOWN)
      .addRuleDescriptionSectionDto(ruleDescriptionSectionDto);

    setCleanCodeAttributeAndImpacts(newRule, ruleDto, templateRuleDto);

    Set<String> tags = templateRuleDto.getTags();
    if (!tags.isEmpty()) {
      ruleDto.setTags(tags);
    }
    dbClient.ruleDao().insert(dbSession, ruleDto);

    for (RuleParamDto templateRuleParamDto : dbClient.ruleDao().selectRuleParamsByRuleKey(dbSession, templateRuleDto.getKey())) {
      String customRuleParamValue = Strings.emptyToNull(newRule.parameter(templateRuleParamDto.getName()));
      createCustomRuleParams(customRuleParamValue, ruleDto, templateRuleParamDto, dbSession);
    }
    return ruleDto;
  }

  private static void setCleanCodeAttributeAndImpacts(NewCustomRule newRule, RuleDto ruleDto, RuleDto templateRuleDto) {
    RuleType ruleType = newRule.type();
    int type = ruleType == null ? templateRuleDto.getType() : ruleType.getDbConstant();
    String severity = ofNullable(newRule.severity()).orElse(Severity.MAJOR);

    if (type == RuleType.SECURITY_HOTSPOT.getDbConstant()) {
      ruleDto.setType(type).setSeverity(severity);
    } else {
      ruleDto.setCleanCodeAttribute(ofNullable(newRule.getCleanCodeAttribute()).orElse(CleanCodeAttribute.CONVENTIONAL));

      if (!CollectionUtils.isEmpty(newRule.getImpacts())) {
        newRule.getImpacts().stream()
          .map(impact -> new ImpactDto(impact.softwareQuality(), impact.severity()))
          .forEach(ruleDto::addDefaultImpact);
        // Back-map old type and severity from the impact
        Map.Entry<SoftwareQuality, org.sonar.api.issue.impact.Severity> impact = ImpactMapper.getBestImpactForBackmapping(
          newRule.getImpacts().stream().collect(Collectors.toMap(NewCustomRule.Impact::softwareQuality, NewCustomRule.Impact::severity)));
        ruleDto.setType(ImpactMapper.convertToRuleType(impact.getKey()).getDbConstant());
        ruleDto.setSeverity(ImpactMapper.convertToDeprecatedSeverity(impact.getValue()));
      } else {
        // Map old type and severity to impact
        SoftwareQuality softwareQuality = ImpactMapper.convertToSoftwareQuality(RuleType.valueOf(type));
        org.sonar.api.issue.impact.Severity impactSeverity = ImpactMapper.convertToImpactSeverity(severity);
        ruleDto.addDefaultImpact(new ImpactDto()
          .setSoftwareQuality(softwareQuality)
          .setSeverity(impactSeverity))
          .setType(type)
          .setSeverity(severity);
      }
    }
  }

  private void createCustomRuleParams(@Nullable String paramValue, RuleDto ruleDto, RuleParamDto templateRuleParam, DbSession dbSession) {
    RuleParamDto ruleParamDto = RuleParamDto.createFor(ruleDto)
      .setName(templateRuleParam.getName())
      .setType(templateRuleParam.getType())
      .setDescription(templateRuleParam.getDescription())
      .setDefaultValue(paramValue);
    dbClient.ruleDao().insertRuleParam(dbSession, ruleDto, ruleParamDto);
  }

  private RuleDto updateExistingRule(RuleDto ruleDto, NewCustomRule newRule, DbSession dbSession) {
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
    return ruleDto;
  }

}
