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
package org.sonar.server.rule.ws;

import com.google.common.base.Function;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.user.UserDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.rule.ws.SearchAction.SearchResult;
import org.sonar.server.text.MacroInterpreter;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.RuleScope;
import org.sonarqube.ws.Rules;

import static java.lang.String.format;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_CREATED_AT;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEBT_OVERLOADED;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEBT_REM_FUNCTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEFAULT_DEBT_REM_FUNCTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_DEFAULT_REM_FUNCTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_EFFORT_TO_FIX_DESCRIPTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_GAP_DESCRIPTION;
import static org.sonar.server.rule.ws.RulesWsParameters.FIELD_HTML_DESCRIPTION;
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

/**
 * Conversion of {@link RuleDto} to {@link Rules.Rule}
 */
public class RuleMapper {

  private final Languages languages;
  private final MacroInterpreter macroInterpreter;

  public RuleMapper(final Languages languages, final MacroInterpreter macroInterpreter) {
    this.languages = languages;
    this.macroInterpreter = macroInterpreter;
  }

  public Rules.Rule toWsRule(RuleDefinitionDto ruleDefinitionDto, SearchResult result, Set<String> fieldsToReturn) {
    Rules.Rule.Builder ruleResponse = Rules.Rule.newBuilder();
    applyRuleDefinition(ruleResponse, ruleDefinitionDto, result, fieldsToReturn);
    return ruleResponse.build();
  }

  public Rules.Rule toWsRule(RuleDefinitionDto ruleDefinition, SearchResult result, Set<String> fieldsToReturn, RuleMetadataDto metadata, Map<String, UserDto> usersByUuid) {
    Rules.Rule.Builder ruleResponse = Rules.Rule.newBuilder();
    applyRuleDefinition(ruleResponse, ruleDefinition, result, fieldsToReturn);
    applyRuleMetadata(ruleResponse, ruleDefinition, metadata, usersByUuid, fieldsToReturn);
    setDebtRemediationFunctionFields(ruleResponse, ruleDefinition, metadata, fieldsToReturn);
    return ruleResponse.build();
  }

  private Rules.Rule.Builder applyRuleDefinition(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDefinitionDto, SearchResult result, Set<String> fieldsToReturn) {

    // Mandatory fields
    ruleResponse.setKey(ruleDefinitionDto.getKey().toString());
    ruleResponse.setType(Common.RuleType.forNumber(ruleDefinitionDto.getType()));

    // Optional fields
    setName(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setRepository(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setStatus(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setSysTags(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setParams(ruleResponse, ruleDefinitionDto, result, fieldsToReturn);
    setCreatedAt(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setDescriptionFields(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setSeverity(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setInternalKey(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setLanguage(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setLanguageName(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setIsTemplate(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setIsExternal(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setTemplateKey(ruleResponse, ruleDefinitionDto, result, fieldsToReturn);
    setDefaultDebtRemediationFunctionFields(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setEffortToFixDescription(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    setScope(ruleResponse, ruleDefinitionDto, fieldsToReturn);
    return ruleResponse;
  }

  private void applyRuleMetadata(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDefinition, RuleMetadataDto metadata, Map<String, UserDto> usersByUuid,
    Set<String> fieldsToReturn) {
    setTags(ruleResponse, metadata, fieldsToReturn);
    setNotesFields(ruleResponse, metadata, usersByUuid, fieldsToReturn);
    setIsRemediationFunctionOverloaded(ruleResponse, metadata, fieldsToReturn);
    if (ruleDefinition.isAdHoc()) {
      setAdHocName(ruleResponse, metadata, fieldsToReturn);
      setAdHocDescription(ruleResponse, metadata, fieldsToReturn);
      setAdHocSeverity(ruleResponse, metadata, fieldsToReturn);
      setAdHocType(ruleResponse, metadata);
    }
  }

  private static void setAdHocName(Rules.Rule.Builder ruleResponse, RuleMetadataDto metadata, Set<String> fieldsToReturn) {
    String adHocName = metadata.getAdHocName();
    if (adHocName != null && shouldReturnField(fieldsToReturn, FIELD_NAME)) {
      ruleResponse.setName(adHocName);
    }
  }

  private void setAdHocDescription(Rules.Rule.Builder ruleResponse, RuleMetadataDto metadata, Set<String> fieldsToReturn) {
    String adHocDescription = metadata.getAdHocDescription();
    if (adHocDescription != null && shouldReturnField(fieldsToReturn, FIELD_HTML_DESCRIPTION)) {
      ruleResponse.setHtmlDesc(macroInterpreter.interpret(adHocDescription));
    }
  }

  private static void setAdHocSeverity(Rules.Rule.Builder ruleResponse, RuleMetadataDto metadata, Set<String> fieldsToReturn) {
    String severity = metadata.getAdHocSeverity();
    if (shouldReturnField(fieldsToReturn, FIELD_SEVERITY) && severity != null) {
      ruleResponse.setSeverity(severity);
    }
  }

  private static void setAdHocType(Rules.Rule.Builder ruleResponse, RuleMetadataDto metadata) {
    Integer ruleType = metadata.getAdHocType();
    if (ruleType != null) {
      ruleResponse.setType(Common.RuleType.forNumber(ruleType));
    }
  }

  private static void setRepository(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_REPO)) {
      ruleResponse.setRepo(ruleDto.getKey().repository());
    }
  }

  private static void setScope(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_SCOPE)) {
      ruleResponse.setScope(toWsRuleScope(ruleDto.getScope()));
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

  private static void setEffortToFixDescription(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    String gapDescription = ruleDto.getGapDescription();
    if ((shouldReturnField(fieldsToReturn, FIELD_EFFORT_TO_FIX_DESCRIPTION) || shouldReturnField(fieldsToReturn, FIELD_GAP_DESCRIPTION))
      && gapDescription != null) {
      ruleResponse.setEffortToFixDescription(gapDescription);
      ruleResponse.setGapDescription(gapDescription);
    }
  }

  private static void setIsRemediationFunctionOverloaded(Rules.Rule.Builder ruleResponse, RuleMetadataDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_DEBT_OVERLOADED) || shouldReturnField(fieldsToReturn, FIELD_REM_FUNCTION_OVERLOADED)) {
      ruleResponse.setDebtOverloaded(isRemediationFunctionOverloaded(ruleDto));
      ruleResponse.setRemFnOverloaded(isRemediationFunctionOverloaded(ruleDto));
    }
  }

  private static void setDefaultDebtRemediationFunctionFields(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_DEFAULT_DEBT_REM_FUNCTION) || shouldReturnField(fieldsToReturn, FIELD_DEFAULT_REM_FUNCTION)) {
      DebtRemediationFunction defaultDebtRemediationFunction = defaultDebtRemediationFunction(ruleDto);
      if (defaultDebtRemediationFunction != null) {
        String gapMultiplier = defaultDebtRemediationFunction.gapMultiplier();
        if (gapMultiplier != null) {
          ruleResponse.setDefaultRemFnGapMultiplier(gapMultiplier);
          // Set deprecated field
          ruleResponse.setDefaultDebtRemFnCoeff(gapMultiplier);
        }
        String baseEffort = defaultDebtRemediationFunction.baseEffort();
        if (baseEffort != null) {
          ruleResponse.setDefaultRemFnBaseEffort(baseEffort);
          // Set deprecated field
          ruleResponse.setDefaultDebtRemFnOffset(baseEffort);
        }
        if (defaultDebtRemediationFunction.type() != null) {
          ruleResponse.setDefaultRemFnType(defaultDebtRemediationFunction.type().name());
          // Set deprecated field
          ruleResponse.setDefaultDebtRemFnType(defaultDebtRemediationFunction.type().name());
        }
      }
    }
  }

  private static void setDebtRemediationFunctionFields(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDefinitionDto, RuleMetadataDto ruleMetadataDto,
    Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_DEBT_REM_FUNCTION) || shouldReturnField(fieldsToReturn, FIELD_REM_FUNCTION)) {
      DebtRemediationFunction debtRemediationFunction = debtRemediationFunction(ruleDefinitionDto, ruleMetadataDto);
      if (debtRemediationFunction != null) {
        if (debtRemediationFunction.type() != null) {
          ruleResponse.setRemFnType(debtRemediationFunction.type().name());
          // Set deprecated field
          ruleResponse.setDebtRemFnType(debtRemediationFunction.type().name());
        }
        String gapMultiplier = debtRemediationFunction.gapMultiplier();
        if (gapMultiplier != null) {
          ruleResponse.setRemFnGapMultiplier(gapMultiplier);
          // Set deprecated field
          ruleResponse.setDebtRemFnCoeff(gapMultiplier);
        }
        String baseEffort = debtRemediationFunction.baseEffort();
        if (baseEffort != null) {
          ruleResponse.setRemFnBaseEffort(baseEffort);
          // Set deprecated field
          ruleResponse.setDebtRemFnOffset(baseEffort);
        }
      }
    }
  }

  private static void setName(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_NAME) && ruleDto.getName() != null) {
      ruleResponse.setName(ruleDto.getName());
    }
  }

  private static void setStatus(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_STATUS) && ruleDto.getStatus() != null) {
      ruleResponse.setStatus(Common.RuleStatus.valueOf(ruleDto.getStatus().toString()));
    }
  }

  private static void setTags(Rules.Rule.Builder ruleResponse, RuleMetadataDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_TAGS)) {
      ruleResponse.getTagsBuilder().addAllTags(ruleDto.getTags());
    }
  }

  private static void setSysTags(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_SYSTEM_TAGS)) {
      ruleResponse.getSysTagsBuilder().addAllSysTags(ruleDto.getSystemTags());
    }
  }

  private static void setParams(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, SearchResult searchResult, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_PARAMS)) {
      List<RuleParamDto> ruleParameters = searchResult.getRuleParamsByRuleId().get(ruleDto.getId());
      ruleResponse.getParamsBuilder().addAllParams(ruleParameters.stream().map(RuleParamDtoToWsRuleParam.INSTANCE::apply).collect(toList()));
    }
  }

  private static void setCreatedAt(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_CREATED_AT)) {
      ruleResponse.setCreatedAt(formatDateTime(ruleDto.getCreatedAt()));
    }
  }

  private void setDescriptionFields(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    String description = ruleDto.getDescription();
    if (shouldReturnField(fieldsToReturn, FIELD_HTML_DESCRIPTION)) {
      RuleDto.Format descriptionFormat = ruleDto.getDescriptionFormat();
      if (description != null && descriptionFormat != null) {
        switch (descriptionFormat) {
          case MARKDOWN:
            ruleResponse.setHtmlDesc(macroInterpreter.interpret(Markdown.convertToHtml(description)));
            break;
          case HTML:
            ruleResponse.setHtmlDesc(macroInterpreter.interpret(description));
            break;
          default:
            throw new IllegalStateException(format("Rule description format '%s' is unknown for key '%s'", descriptionFormat, ruleDto.getKey().toString()));
        }
      }
    }

    if (shouldReturnField(fieldsToReturn, FIELD_MARKDOWN_DESCRIPTION) && description != null) {
      ruleResponse.setMdDesc(description);
    }
  }

  private void setNotesFields(Rules.Rule.Builder ruleResponse, RuleMetadataDto ruleDto, Map<String, UserDto> usersByUuid, Set<String> fieldsToReturn) {
    String noteData = ruleDto.getNoteData();
    if (shouldReturnField(fieldsToReturn, "htmlNote") && noteData != null) {
      ruleResponse.setHtmlNote(macroInterpreter.interpret(Markdown.convertToHtml(noteData)));
    }
    if (shouldReturnField(fieldsToReturn, "mdNote") && noteData != null) {
      ruleResponse.setMdNote(noteData);
    }
    String userUuid = ruleDto.getNoteUserUuid();
    if (shouldReturnField(fieldsToReturn, FIELD_NOTE_LOGIN) && userUuid != null) {
      ruleResponse.setNoteLogin(usersByUuid.get(userUuid).getLogin());
    }
  }

  private static void setSeverity(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    String severity = ruleDto.getSeverityString();
    if (shouldReturnField(fieldsToReturn, FIELD_SEVERITY) && severity != null) {
      ruleResponse.setSeverity(severity);
    }
  }

  private static void setInternalKey(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_INTERNAL_KEY) && ruleDto.getConfigKey() != null) {
      ruleResponse.setInternalKey(ruleDto.getConfigKey());
    }
  }

  private static void setLanguage(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    String language = ruleDto.getLanguage();
    if (shouldReturnField(fieldsToReturn, FIELD_LANGUAGE) && language != null) {
      ruleResponse.setLang(language);
    }
  }

  private void setLanguageName(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    String languageKey = ruleDto.getLanguage();
    if (shouldReturnField(fieldsToReturn, FIELD_LANGUAGE_NAME) && languageKey != null) {
      Language language = languages.get(languageKey);
      ruleResponse.setLangName(language == null ? languageKey : language.getName());
    }
  }

  private static void setIsTemplate(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_IS_TEMPLATE)) {
      ruleResponse.setIsTemplate(ruleDto.isTemplate());
    }
  }

  private static void setIsExternal(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_IS_EXTERNAL)) {
      ruleResponse.setIsExternal(ruleDto.isExternal());
    }
  }

  private static void setTemplateKey(Rules.Rule.Builder ruleResponse, RuleDefinitionDto ruleDto, SearchResult result, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, FIELD_TEMPLATE_KEY) && ruleDto.isCustomRule()) {
      RuleDefinitionDto templateRule = result.getTemplateRulesByRuleId().get(ruleDto.getTemplateId());
      if (templateRule != null) {
        ruleResponse.setTemplateKey(templateRule.getKey().toString());
      }
    }
  }

  private static boolean shouldReturnField(Set<String> fieldsToReturn, String fieldName) {
    return fieldsToReturn.isEmpty() || fieldsToReturn.contains(fieldName);
  }

  private static boolean isRemediationFunctionOverloaded(RuleMetadataDto rule) {
    return rule.getRemediationFunction() != null;
  }

  @CheckForNull
  private static DebtRemediationFunction defaultDebtRemediationFunction(final RuleDefinitionDto ruleDto) {
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
  private static DebtRemediationFunction debtRemediationFunction(RuleDefinitionDto ruleDefinitionDto, RuleMetadataDto ruleMetadataDto) {
    final String function = ruleMetadataDto.getRemediationFunction();
    if (function == null || function.isEmpty()) {
      return defaultDebtRemediationFunction(ruleDefinitionDto);
    } else {
      return new DefaultDebtRemediationFunction(
        DebtRemediationFunction.Type.valueOf(function.toUpperCase(Locale.ENGLISH)),
        ruleMetadataDto.getRemediationGapMultiplier(),
        ruleMetadataDto.getRemediationBaseEffort());
    }
  }

  private enum RuleParamDtoToWsRuleParam implements Function<RuleParamDto, Rules.Rule.Param> {
    INSTANCE;

    @Override
    public Rules.Rule.Param apply(@Nonnull RuleParamDto param) {
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
