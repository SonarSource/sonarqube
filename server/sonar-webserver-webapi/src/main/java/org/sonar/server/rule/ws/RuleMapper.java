/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.rule.ws;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.core.rule.ImpactFormatter;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.common.text.MacroInterpreter;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.rule.ws.RulesResponseFormatter.SearchResult;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.RuleScope;
import org.sonarqube.ws.Rules;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.rule.RuleDto.Format.MARKDOWN;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_CLEAN_CODE_ATTRIBUTE;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_CREATED_AT;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEBT_REM_FUNCTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEFAULT_DEBT_REM_FUNCTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEFAULT_REM_FUNCTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEPRECATED_KEYS;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DESCRIPTION_SECTIONS;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_EDUCATION_PRINCIPLES;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_GAP_DESCRIPTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_INTERNAL_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_IS_EXTERNAL;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_IS_TEMPLATE;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_LANGUAGE;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_LANGUAGE_NAME;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_MARKDOWN_DESCRIPTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_NAME;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_NOTE_LOGIN;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_PARAMS;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_REM_FUNCTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_REM_FUNCTION_OVERLOADED;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_REPO;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_SCOPE;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_SEVERITY;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_STATUS;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_SYSTEM_TAGS;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_TAGS;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_TEMPLATE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_UPDATED_AT;
import static org.sonarqube.ws.Rules.Rule.DescriptionSection.Context.newBuilder;

/**
 * Conversion of {@link RuleDto} to {@link Rules.Rule}
 */
public class RuleMapper {

  private final Languages languages;
  private final MacroInterpreter macroInterpreter;
  private final RuleDescriptionFormatter ruleDescriptionFormatter;

  public RuleMapper(final Languages languages, final MacroInterpreter macroInterpreter, RuleDescriptionFormatter ruleDescriptionFormatter) {
    this.languages = languages;
    this.macroInterpreter = macroInterpreter;
    this.ruleDescriptionFormatter = ruleDescriptionFormatter;
  }

  public Rules.Rule toWsRule(RuleDto ruleDefinitionDto, SearchResult result, Set<String> fieldsToReturn) {
    Rules.Rule.Builder ruleResponse = Rules.Rule.newBuilder();
    applyRuleDefinition(ruleResponse, ruleDefinitionDto, result, fieldsToReturn, Collections.emptyMap());
    return ruleResponse.build();
  }

  public Rules.Rule toWsRule(RuleDto ruleDto, SearchResult result, Set<String> fieldsToReturn, Map<String, UserDto> usersByUuid,
    Map<String, List<DeprecatedRuleKeyDto>> deprecatedRuleKeysByRuleUuid) {
    Rules.Rule.Builder ruleResponse = Rules.Rule.newBuilder();
    applyRuleDefinition(ruleResponse, ruleDto, result, fieldsToReturn, deprecatedRuleKeysByRuleUuid);
    setDebtRemediationFunctionFields(ruleResponse, ruleDto, fieldsToReturn);
    setNotesFields(ruleResponse, ruleDto, usersByUuid, fieldsToReturn);
    return ruleResponse.build();
  }

  private Rules.Rule.Builder applyRuleDefinition(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, SearchResult result,
    Set<String> fieldsToReturn, Map<String, List<DeprecatedRuleKeyDto>> deprecatedRuleKeysByRuleUuid) {

    // Mandatory fields
    ruleResponse.setKey(ruleDto.getKey().toString());
    ruleResponse.setType(Common.RuleType.forNumber(ruleDto.getType()));
    setImpacts(ruleResponse, ruleDto);

    // Optional fields
    setName(ruleResponse, ruleDto, fieldsToReturn);
    setRepository(ruleResponse, ruleDto, fieldsToReturn);
    setStatus(ruleResponse, ruleDto, fieldsToReturn);
    setSysTags(ruleResponse, ruleDto, fieldsToReturn);
    setParams(ruleResponse, ruleDto, result, fieldsToReturn);
    setCreatedAt(ruleResponse, ruleDto, fieldsToReturn);
    setUpdatedAt(ruleResponse, ruleDto, fieldsToReturn);
    setDescriptionFields(ruleResponse, ruleDto, fieldsToReturn);
    setSeverity(ruleResponse, ruleDto, fieldsToReturn);
    setInternalKey(ruleResponse, ruleDto, fieldsToReturn);
    setLanguage(ruleResponse, ruleDto, fieldsToReturn);
    setLanguageName(ruleResponse, ruleDto, fieldsToReturn);
    setIsTemplate(ruleResponse, ruleDto, fieldsToReturn);
    setIsExternal(ruleResponse, ruleDto, fieldsToReturn);
    setTemplateKey(ruleResponse, ruleDto, result, fieldsToReturn);
    setDefaultDebtRemediationFunctionFields(ruleResponse, ruleDto, fieldsToReturn);
    setGapDescription(ruleResponse, ruleDto, fieldsToReturn);
    setScope(ruleResponse, ruleDto, fieldsToReturn);
    setDeprecatedKeys(ruleResponse, ruleDto, fieldsToReturn, deprecatedRuleKeysByRuleUuid);

    setTags(ruleResponse, ruleDto, fieldsToReturn);
    setIsRemediationFunctionOverloaded(ruleResponse, ruleDto, fieldsToReturn);
    if (ruleDto.isAdHoc()) {
      setAdHocName(ruleResponse, ruleDto, fieldsToReturn);
      setAdHocDescription(ruleResponse, ruleDto, fieldsToReturn);
      setAdHocSeverity(ruleResponse, ruleDto, fieldsToReturn);
      setAdHocType(ruleResponse, ruleDto);
    }
    setEducationPrinciples(ruleResponse, ruleDto, fieldsToReturn);
    setCleanCodeAttributes(ruleResponse, ruleDto, fieldsToReturn);

    return ruleResponse;
  }

  private static void setImpacts(Rules.Rule.Builder ruleResponse, RuleDto ruleDto) {
    Rules.Impacts.Builder impactsBuilder = Rules.Impacts.newBuilder();
    ruleDto.getDefaultImpacts().forEach(impactDto -> impactsBuilder.addImpacts(toImpact(impactDto)));
    ruleResponse.setImpacts(impactsBuilder.build());
  }

  private static Common.Impact toImpact(ImpactDto impactDto) {
    Common.ImpactSeverity severity = ImpactFormatter.mapImpactSeverity(impactDto.getSeverity());
    Common.SoftwareQuality softwareQuality = Common.SoftwareQuality.valueOf(impactDto.getSoftwareQuality().name());
    return Common.Impact.newBuilder().setSeverity(severity).setSoftwareQuality(softwareQuality).build();
  }

  private static void setAdHocName(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    String adHocName = ruleDto.getAdHocName();
    if (adHocName != null && shouldReturnField(fieldsToReturn, FIELD_NAME)) {
      ruleResponse.setName(adHocName);
    }
  }

  private void setAdHocDescription(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    String adHocDescription = ruleDto.getAdHocDescription();
    if (shouldReturnField(fieldsToReturn, FIELD_DESCRIPTION_SECTIONS) && adHocDescription != null) {
      ruleResponse.clearDescriptionSections();
      ruleResponse.getDescriptionSectionsBuilder().addDescriptionSectionsBuilder()
        .setKey(RuleDescriptionSectionDto.DEFAULT_KEY)
        .setContent(macroInterpreter.interpret(adHocDescription));
    }
  }

  private static void setAdHocSeverity(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    String severity = ruleDto.getAdHocSeverity();
    if (shouldReturnField(fieldsToReturn, FIELD_SEVERITY) && severity != null) {
      ruleResponse.setSeverity(severity);
    }
  }

  private static void setAdHocType(Rules.Rule.Builder ruleResponse, RuleDto ruleDto) {
    Integer ruleType = ruleDto.getAdHocType();
    if (ruleType != null) {
      ruleResponse.setType(Common.RuleType.forNumber(ruleType));
    }
  }

  private static void setRepository(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_REPO)) {
      ruleResponse.setRepo(ruleDto.getKey().repository());
    }
  }

  private static void setScope(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_SCOPE)) {
      ruleResponse.setScope(toWsRuleScope(ruleDto.getScope()));
    }
  }

  private static void setEducationPrinciples(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_EDUCATION_PRINCIPLES)) {
      ruleResponse.getEducationPrinciplesBuilder().addAllEducationPrinciples((ruleDto.getEducationPrinciples()));
    }
  }

  private static void setCleanCodeAttributes(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    CleanCodeAttribute cleanCodeAttribute = ruleDto.getCleanCodeAttribute();
    if (shouldReturnField(fieldsToReturn, FIELD_CLEAN_CODE_ATTRIBUTE) && cleanCodeAttribute != null) {
      ruleResponse.setCleanCodeAttribute(Common.CleanCodeAttribute.valueOf(cleanCodeAttribute.name()));
      ruleResponse.setCleanCodeAttributeCategory(Common.CleanCodeAttributeCategory.valueOf(cleanCodeAttribute.getAttributeCategory().name()));
    }
  }

  private static void setDeprecatedKeys(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn,
    Map<String, List<DeprecatedRuleKeyDto>> deprecatedRuleKeysByRuleUuid) {
    if (shouldReturnField(fieldsToReturn, FIELD_DEPRECATED_KEYS)) {
      List<DeprecatedRuleKeyDto> deprecatedRuleKeyDtos = deprecatedRuleKeysByRuleUuid.get(ruleDto.getUuid());
      if (deprecatedRuleKeyDtos == null) {
        return;
      }

      List<String> deprecatedKeys = deprecatedRuleKeyDtos.stream()
        .map(r -> RuleKey.of(r.getOldRepositoryKey(), r.getOldRuleKey()).toString())
        .toList();
      if (!deprecatedKeys.isEmpty()) {
        ruleResponse.setDeprecatedKeys(Rules.DeprecatedKeys.newBuilder().addAllDeprecatedKey(deprecatedKeys).build());
      }
    }
  }

  private static RuleScope toWsRuleScope(Scope scope) {
    switch (scope) {
      case ALL:
        return RuleScope.ALL;
      case MAIN:
        return RuleScope.MAIN;
      case TEST:
        return RuleScope.TEST;
      default:
        throw new IllegalArgumentException("Unknown rule scope: " + scope);
    }
  }

  private static void setGapDescription(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    String gapDescription = ruleDto.getGapDescription();
    if (shouldReturnField(fieldsToReturn, FIELD_GAP_DESCRIPTION) && gapDescription != null) {
      ruleResponse.setGapDescription(gapDescription);
    }
  }

  private static void setIsRemediationFunctionOverloaded(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_REM_FUNCTION_OVERLOADED)) {
      ruleResponse.setRemFnOverloaded(isRemediationFunctionOverloaded(ruleDto));
    }
  }

  private static void setDefaultDebtRemediationFunctionFields(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_DEFAULT_DEBT_REM_FUNCTION) || shouldReturnField(fieldsToReturn, FIELD_DEFAULT_REM_FUNCTION)) {
      DebtRemediationFunction defaultDebtRemediationFunction = defaultDebtRemediationFunction(ruleDto);
      if (defaultDebtRemediationFunction != null) {
        String gapMultiplier = defaultDebtRemediationFunction.gapMultiplier();
        if (gapMultiplier != null) {
          ruleResponse.setDefaultRemFnGapMultiplier(gapMultiplier);
        }
        String baseEffort = defaultDebtRemediationFunction.baseEffort();
        if (baseEffort != null) {
          ruleResponse.setDefaultRemFnBaseEffort(baseEffort);
        }
        if (defaultDebtRemediationFunction.type() != null) {
          ruleResponse.setDefaultRemFnType(defaultDebtRemediationFunction.type().name());
          // Set deprecated field
          ruleResponse.setDefaultDebtRemFnType(defaultDebtRemediationFunction.type().name());
        }
      }
    }
  }

  private static void setDebtRemediationFunctionFields(Rules.Rule.Builder ruleResponse, RuleDto ruleDto,
    Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_DEBT_REM_FUNCTION) || shouldReturnField(fieldsToReturn, FIELD_REM_FUNCTION)) {
      DebtRemediationFunction debtRemediationFunction = debtRemediationFunction(ruleDto);
      if (debtRemediationFunction != null) {
        if (debtRemediationFunction.type() != null) {
          ruleResponse.setRemFnType(debtRemediationFunction.type().name());
          // Set deprecated field
          ruleResponse.setDebtRemFnType(debtRemediationFunction.type().name());
        }
        String gapMultiplier = debtRemediationFunction.gapMultiplier();
        if (gapMultiplier != null) {
          ruleResponse.setRemFnGapMultiplier(gapMultiplier);
        }
        String baseEffort = debtRemediationFunction.baseEffort();
        if (baseEffort != null) {
          ruleResponse.setRemFnBaseEffort(baseEffort);
        }
      }
    }
  }

  private static void setName(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_NAME) && ruleDto.getName() != null) {
      ruleResponse.setName(ruleDto.getName());
    }
  }

  private static void setStatus(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_STATUS) && ruleDto.getStatus() != null) {
      ruleResponse.setStatus(Common.RuleStatus.valueOf(ruleDto.getStatus().toString()));
    }
  }

  private static void setTags(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_TAGS)) {
      ruleResponse.getTagsBuilder().addAllTags(ruleDto.getTags());
    }
  }

  private static void setSysTags(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_SYSTEM_TAGS)) {
      ruleResponse.getSysTagsBuilder().addAllSysTags(ruleDto.getSystemTags());
    }
  }

  private static void setParams(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, SearchResult searchResult, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_PARAMS)) {
      List<RuleParamDto> ruleParameters = searchResult.getRuleParamsByRuleUuid().get(ruleDto.getUuid());
      ruleResponse.getParamsBuilder().addAllParams(ruleParameters.stream().map(RuleParamDtoToWsRuleParam.INSTANCE).toList());
    }
  }

  private static void setCreatedAt(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_CREATED_AT)) {
      ruleResponse.setCreatedAt(formatDateTime(ruleDto.getCreatedAt()));
    }
  }

  private static void setUpdatedAt(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_UPDATED_AT)) {
      ruleResponse.setUpdatedAt(formatDateTime(ruleDto.getUpdatedAt()));
    }
  }

  private void setDescriptionFields(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_DESCRIPTION_SECTIONS)) {
      Set<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = ruleDto.getRuleDescriptionSectionDtos();
      Set<Rules.Rule.DescriptionSection> sections = ruleDescriptionSectionDtos.stream()
        .map(sectionDto -> toDescriptionSection(ruleDto, sectionDto))
        .collect(Collectors.toSet());
      ruleResponse.setDescriptionSections(Rules.Rule.DescriptionSections.newBuilder().addAllDescriptionSections(sections).build());
    }

    if (shouldReturnField(fieldsToReturn, FIELD_MARKDOWN_DESCRIPTION) && MARKDOWN.equals(ruleDto.getDescriptionFormat())) {
      Optional.ofNullable(ruleDto.getDefaultRuleDescriptionSection())
        .map(RuleDescriptionSectionDto::getContent)
        .ifPresent(ruleResponse::setMdDesc);
    }
  }

  private Rules.Rule.DescriptionSection toDescriptionSection(RuleDto ruleDto, RuleDescriptionSectionDto section) {
    String htmlContent = ruleDescriptionFormatter.toHtml(ruleDto.getDescriptionFormat(), section);
    String interpretedHtmlContent = macroInterpreter.interpret(htmlContent);

    Rules.Rule.DescriptionSection.Builder sectionBuilder = Rules.Rule.DescriptionSection.newBuilder()
      .setKey(section.getKey())
      .setContent(interpretedHtmlContent);
    toProtobufContext(section.getContext()).ifPresent(sectionBuilder::setContext);

    return sectionBuilder.build();
  }

  private void setNotesFields(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Map<String, UserDto> usersByUuid, Set<String> fieldsToReturn) {
    String noteData = ruleDto.getNoteData();
    if (shouldReturnField(fieldsToReturn, "htmlNote") && noteData != null) {
      ruleResponse.setHtmlNote(macroInterpreter.interpret(Markdown.convertToHtml(noteData)));
    }
    if (shouldReturnField(fieldsToReturn, "mdNote") && noteData != null) {
      ruleResponse.setMdNote(noteData);
    }
    String userUuid = ruleDto.getNoteUserUuid();
    if (shouldReturnField(fieldsToReturn, FIELD_NOTE_LOGIN) && userUuid != null && usersByUuid.containsKey(userUuid)) {
      ruleResponse.setNoteLogin(usersByUuid.get(userUuid).getLogin());
    }
  }

  private static void setSeverity(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    String severity = ruleDto.getSeverityString();
    if (shouldReturnField(fieldsToReturn, FIELD_SEVERITY) && severity != null) {
      ruleResponse.setSeverity(severity);
    }
  }

  private static void setInternalKey(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_INTERNAL_KEY) && ruleDto.getConfigKey() != null) {
      ruleResponse.setInternalKey(ruleDto.getConfigKey());
    }
  }

  private static void setLanguage(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    String language = ruleDto.getLanguage();
    if (shouldReturnField(fieldsToReturn, FIELD_LANGUAGE) && language != null) {
      ruleResponse.setLang(language);
    }
  }

  private void setLanguageName(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    String languageKey = ruleDto.getLanguage();
    if (shouldReturnField(fieldsToReturn, FIELD_LANGUAGE_NAME) && languageKey != null) {
      Language language = languages.get(languageKey);
      ruleResponse.setLangName(language == null ? languageKey : language.getName());
    }
  }

  private static void setIsTemplate(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_IS_TEMPLATE)) {
      ruleResponse.setIsTemplate(ruleDto.isTemplate());
    }
  }

  private static void setIsExternal(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_IS_EXTERNAL)) {
      ruleResponse.setIsExternal(ruleDto.isExternal());
    }
  }

  private static void setTemplateKey(Rules.Rule.Builder ruleResponse, RuleDto ruleDto, SearchResult result, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_TEMPLATE_KEY) && ruleDto.isCustomRule()) {
      RuleDto templateRule = result.getTemplateRulesByRuleUuid().get(ruleDto.getTemplateUuid());
      if (templateRule != null) {
        ruleResponse.setTemplateKey(templateRule.getKey().toString());
      }
    }
  }

  public static boolean shouldReturnField(Set<String> fieldsToReturn, String fieldName) {
    return fieldsToReturn.isEmpty() || fieldsToReturn.contains(fieldName);
  }

  private static Optional<Rules.Rule.DescriptionSection.Context> toProtobufContext(@Nullable RuleDescriptionSectionContextDto context) {
    return Optional.ofNullable(context)
      .map(c -> newBuilder()
        .setKey(c.getKey())
        .setDisplayName(c.getDisplayName())
        .build());
  }

  private static boolean isRemediationFunctionOverloaded(RuleDto rule) {
    return rule.getRemediationFunction() != null;
  }

  @CheckForNull
  private static DebtRemediationFunction defaultDebtRemediationFunction(final RuleDto ruleDto) {
    final String function = ruleDto.getDefRemediationFunction();
    if (function == null || function.isEmpty()) {
      return null;
    } else {
      return new DefaultDebtRemediationFunction(
        DebtRemediationFunction.Type.valueOf(function.toUpperCase(Locale.ENGLISH)),
        ruleDto.getDefRemediationGapMultiplier(),
        ruleDto.getDefRemediationBaseEffort());
    }
  }

  @CheckForNull
  private static DebtRemediationFunction debtRemediationFunction(RuleDto ruleDto) {
    final String function = ruleDto.getRemediationFunction();
    if (function == null || function.isEmpty()) {
      return defaultDebtRemediationFunction(ruleDto);
    } else {
      return new DefaultDebtRemediationFunction(
        DebtRemediationFunction.Type.valueOf(function.toUpperCase(Locale.ENGLISH)),
        ruleDto.getRemediationGapMultiplier(),
        ruleDto.getRemediationBaseEffort());
    }
  }

  private enum RuleParamDtoToWsRuleParam implements Function<RuleParamDto, Rules.Rule.Param> {
    INSTANCE;

    @Override
    public Rules.Rule.Param apply(RuleParamDto param) {
      Rules.Rule.Param.Builder paramResponse = Rules.Rule.Param.newBuilder();
      paramResponse.setKey(param.getName());
      if (param.getDescription() != null) {
        paramResponse.setHtmlDesc(Markdown.convertToHtml(param.getDescription()));
      }
      String defaultValue = param.getDefaultValue();
      if (defaultValue != null) {
        paramResponse.setDefaultValue(defaultValue);
      }
      if (param.getType() != null) {
        paramResponse.setType(param.getType());
      }

      return paramResponse.build();
    }
  }
}
