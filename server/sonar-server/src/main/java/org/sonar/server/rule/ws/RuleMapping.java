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
package org.sonar.server.rule.ws;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.markdown.Markdown;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleParam;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.ws.BaseMapping;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.api.utils.DateUtils.formatDateTime;

/**
 * Conversion of {@link org.sonar.server.rule.index.RuleDoc} to WS JSON document
 */
public class RuleMapping extends BaseMapping<RuleDoc, RuleMappingContext> {

  private final DebtModel debtModel;
  private final Languages languages;
  private final MacroInterpreter macroInterpreter;

  public RuleMapping(final Languages languages, final MacroInterpreter macroInterpreter, final DebtModel debtModel, UserSession userSession) {
    super(userSession);
    this.debtModel = debtModel;
    this.languages = languages;
    this.macroInterpreter = macroInterpreter;

    mapBasicFields();
    mapDescriptionFields();
    mapDebtFields();
    mapParamFields();
  }

  private void mapBasicFields() {
    map("repo", RuleNormalizer.RuleField.REPOSITORY.field());
    map("name", RuleNormalizer.RuleField.NAME.field());
    mapDateTime("createdAt", RuleNormalizer.RuleField.CREATED_AT.field());
    map("severity", RuleNormalizer.RuleField.SEVERITY.field());
    map("status", RuleNormalizer.RuleField.STATUS.field());
    map("internalKey", RuleNormalizer.RuleField.INTERNAL_KEY.field());
    mapBoolean("isTemplate", RuleNormalizer.RuleField.IS_TEMPLATE.field());
    map("templateKey", RuleNormalizer.RuleField.TEMPLATE_KEY.field());
    mapArray("tags", RuleNormalizer.RuleField.TAGS.field());
    mapArray("sysTags", RuleNormalizer.RuleField.SYSTEM_TAGS.field());
    map("lang", RuleNormalizer.RuleField.LANGUAGE.field());
    map("langName", RuleNormalizer.RuleField.LANGUAGE.field());
  }

  private void mapDescriptionFields() {
    map("htmlDesc", RuleNormalizer.RuleField.HTML_DESCRIPTION.field());
    map("mdDesc", RuleNormalizer.RuleField.MARKDOWN_DESCRIPTION.field());
    map("noteLogin", RuleNormalizer.RuleField.NOTE_LOGIN.field());
    map("mdNote", RuleNormalizer.RuleField.NOTE.field());
    map("htmlNote", RuleNormalizer.RuleField.NOTE.field());
  }

  private void mapDebtFields() {
    map("defaultDebtChar", new IndexStringMapper("defaultDebtChar", RuleNormalizer.RuleField.DEFAULT_CHARACTERISTIC.field()));
    map("defaultDebtSubChar", new IndexStringMapper("defaultDebtSubChar", RuleNormalizer.RuleField.DEFAULT_SUB_CHARACTERISTIC.field()));
    map("debtChar", RuleNormalizer.RuleField.CHARACTERISTIC.field());
    map("debtSubChar", RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field());

    map("debtCharName", RuleNormalizer.RuleField.CHARACTERISTIC.field());
    map("debtSubCharName", RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field());

    map("defaultDebtRemFn", new IndexStringMapper("defaultDebtRemFnType", RuleNormalizer.RuleField.DEFAULT_DEBT_FUNCTION_TYPE.field()));
    map("defaultDebtRemFn", new IndexStringMapper("defaultDebtRemFnCoeff", RuleNormalizer.RuleField.DEFAULT_DEBT_FUNCTION_COEFFICIENT.field()));
    map("defaultDebtRemFn", new IndexStringMapper("defaultDebtRemFnOffset", RuleNormalizer.RuleField.DEFAULT_DEBT_FUNCTION_OFFSET.field()));
    map("effortToFixDescription", RuleNormalizer.RuleField.FIX_DESCRIPTION.field());
    map("debtOverloaded", new SimpleMapper(
      RuleNormalizer.RuleField.CHARACTERISTIC_OVERLOADED.field(),
      RuleNormalizer.RuleField.SUB_CHARACTERISTIC_OVERLOADED.field(),
      RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE_OVERLOADED.field()));

    map("debtRemFn", new IndexStringMapper("debtRemFnType", RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE.field()));
    map("debtRemFn", new IndexStringMapper("debtRemFnCoeff", RuleNormalizer.RuleField.DEBT_FUNCTION_COEFFICIENT.field()));
    map("debtRemFn", new IndexStringMapper("debtRemFnOffset", RuleNormalizer.RuleField.DEBT_FUNCTION_OFFSET.field()));
  }

  private void mapParamFields() {
    map("params", RuleNormalizer.RuleField.PARAMS.field());
  }

  public Rules.Rule buildRuleResponse(Rule ruleDoc, @Nullable QueryContext queryContext) {
    Rules.Rule.Builder ruleResponse = Rules.Rule.newBuilder();

    RuleMappingContext context = new RuleMappingContext();
    Set<String> fieldsToReturn = fieldsToReturn(queryContext);

    ruleResponse.setKey(ruleDoc.key().toString());
    setRepository(ruleResponse, ruleDoc, fieldsToReturn);
    setDebtCharacteristicNames(ruleResponse, ruleDoc, queryContext, context);
    setDebtSubCharacteristicNames(ruleResponse, ruleDoc, queryContext, context);
    setName(ruleResponse, ruleDoc, fieldsToReturn);
    setStatus(ruleResponse, ruleDoc, fieldsToReturn);
    setTags(ruleResponse, ruleDoc, fieldsToReturn);
    setSysTags(ruleResponse, ruleDoc, fieldsToReturn);
    setParams(ruleResponse, ruleDoc, fieldsToReturn);
    setCreatedAt(ruleResponse, ruleDoc, fieldsToReturn);
    setDescriptionFields(ruleResponse, ruleDoc, fieldsToReturn);
    setNotesFields(ruleResponse, ruleDoc, fieldsToReturn);
    setSeverity(ruleResponse, ruleDoc, fieldsToReturn);
    setInternalKey(ruleResponse, ruleDoc, fieldsToReturn);
    setLanguage(ruleResponse, ruleDoc, fieldsToReturn);
    setLanguageName(ruleResponse, ruleDoc, fieldsToReturn);
    setIsTemplate(ruleResponse, ruleDoc, fieldsToReturn);
    setTemplateKey(ruleResponse, ruleDoc, fieldsToReturn);
    setDebtRemediationFunctionFields(ruleResponse, ruleDoc, fieldsToReturn);
    setDefaultDebtRemediationFunctionFields(ruleResponse, ruleDoc, fieldsToReturn);
    setIsDebtOverloaded(ruleResponse, ruleDoc, fieldsToReturn);
    setDefaultDebtChar(ruleResponse, ruleDoc, fieldsToReturn);
    setDefaultDebtSubChar(ruleResponse, ruleDoc, fieldsToReturn);
    setEffortToFixDescription(ruleResponse, ruleDoc, fieldsToReturn);

    return ruleResponse.build();
  }

  private static void setRepository(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.REPOSITORY)) {
      ruleResponse.setRepo(ruleDoc.key().repository());
    }
  }

  private static void setEffortToFixDescription(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.FIX_DESCRIPTION) && ruleDoc.effortToFixDescription() != null) {
      ruleResponse.setEffortToFixDescription(ruleDoc.effortToFixDescription());
    }
  }

  private static void setDefaultDebtSubChar(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, "defaultDebtSubChar") && ruleDoc.defaultDebtSubCharacteristicKey() != null) {
      ruleResponse.setDefaultDebtSubChar(ruleDoc.defaultDebtSubCharacteristicKey());
    }
  }

  private static void setDefaultDebtChar(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, "defaultDebtChar") && ruleDoc.defaultDebtCharacteristicKey() != null) {
      ruleResponse.setDefaultDebtChar(ruleDoc.defaultDebtCharacteristicKey());
    }
  }

  private static void setIsDebtOverloaded(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, "debtOverloaded")) {
      ruleResponse.setDebtOverloaded(ruleToOverloaded(ruleDoc));
    }
  }

  private static void setDefaultDebtRemediationFunctionFields(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, "defaultDebtRemFn")) {
      DebtRemediationFunction defaultDebtRemediationFunction = ruleDoc.defaultDebtRemediationFunction();
      if (defaultDebtRemediationFunction != null) {
        if (defaultDebtRemediationFunction.coefficient() != null) {
          ruleResponse.setDefaultDebtRemFnCoeff(defaultDebtRemediationFunction.coefficient());
        }
        if (defaultDebtRemediationFunction.offset() != null) {
          ruleResponse.setDefaultDebtRemFnOffset(defaultDebtRemediationFunction.offset());
        }
        if (defaultDebtRemediationFunction.type() != null) {
          ruleResponse.setDefaultDebtRemFnType(defaultDebtRemediationFunction.type().name());
        }
      }
    }
  }

  private static void setDebtRemediationFunctionFields(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, "debtRemFn")) {
      DebtRemediationFunction debtRemediationFunction = ruleDoc.debtRemediationFunction();
      if (debtRemediationFunction != null) {
        if (debtRemediationFunction.type() != null) {
          ruleResponse.setDebtRemFnType(debtRemediationFunction.type().name());
        }
        if (debtRemediationFunction.coefficient() != null) {
          ruleResponse.setDebtRemFnCoeff(debtRemediationFunction.coefficient());
        }
        if (debtRemediationFunction.offset() != null) {
          ruleResponse.setDebtRemFnOffset(debtRemediationFunction.offset());
        }
      }
    }
  }

  private void setDebtCharacteristicNames(Rules.Rule.Builder ruleResponse, Rule ruleDoc, @Nullable QueryContext queryContext, RuleMappingContext context) {
    if (needDebtCharacteristicNames(queryContext)) {
      String debtCharacteristicKey = ruleDoc.debtCharacteristicKey();
      if (debtCharacteristicKey != null) {
        // load debt characteristics if requested
        context.add(debtModel.characteristicByKey(debtCharacteristicKey));
        buildCharacteristicRuleResponse(ruleResponse, ruleDoc, context);
      }
    }
  }

  private void setDebtSubCharacteristicNames(Rules.Rule.Builder ruleResponse, Rule ruleDoc, @Nullable QueryContext queryContext, RuleMappingContext context) {
    if (needDebtSubCharacteristicNames(queryContext)) {
      String debtSubCharacteristicKey = ruleDoc.debtSubCharacteristicKey();
      if (debtSubCharacteristicKey != null) {
        context.add(debtModel.characteristicByKey(debtSubCharacteristicKey));
        buildDebtSubCharacteristicRuleResponse(ruleResponse, ruleDoc, context);
      }
    }
  }

  private static Set<String> fieldsToReturn(@Nullable QueryContext queryContext) {
    return queryContext == null ? Collections.<String>emptySet() : queryContext.getFieldsToReturn();
  }

  private static void setName(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.NAME) && ruleDoc.name() != null) {
      ruleResponse.setName(ruleDoc.name());
    }
  }

  private static void setStatus(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.STATUS) && ruleDoc.status() != null) {
      ruleResponse.setStatus(Common.RuleStatus.valueOf(ruleDoc.status().toString()));
    }
  }

  private static void setTags(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.TAGS)) {
      ruleResponse.getTagsBuilder().addAllTags(ruleDoc.tags());
    }
  }

  private static void setSysTags(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.SYSTEM_TAGS)) {
      ruleResponse.getSysTagsBuilder().addAllSysTags(ruleDoc.systemTags());
    }
  }

  private static void setParams(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.PARAMS)) {
      ruleResponse.getParamsBuilder().addAllParams(from(ruleDoc.params())
        .transform(RuleParamToResponseRuleParam.INSTANCE)
        .toList());
    }
  }

  private static void setCreatedAt(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.CREATED_AT) && ruleDoc.createdAt() != null) {
      ruleResponse.setCreatedAt(formatDateTime(ruleDoc.createdAt()));
    }
  }

  private void setDescriptionFields(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.HTML_DESCRIPTION)) {
      if (ruleDoc.markdownDescription() != null) {
        ruleResponse.setHtmlDesc(macroInterpreter.interpret(Markdown.convertToHtml(ruleDoc.markdownDescription())));
      } else if (ruleDoc.htmlDescription() != null) {
        ruleResponse.setHtmlDesc(macroInterpreter.interpret(ruleDoc.htmlDescription()));
      }
    }
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.MARKDOWN_DESCRIPTION) && ruleDoc.markdownDescription() != null) {
      ruleResponse.setMdDesc(ruleDoc.markdownDescription());
    }
  }

  private void setNotesFields(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, "htmlNote") && ruleDoc.markdownNote() != null) {
      ruleResponse.setHtmlNote(macroInterpreter.interpret(Markdown.convertToHtml(ruleDoc.markdownNote())));
    }
    if (shouldReturnField(fieldsToReturn, "mdNote") && ruleDoc.markdownNote() != null) {
      ruleResponse.setMdNote(ruleDoc.markdownNote());
    }
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.NOTE_LOGIN) && ruleDoc.noteLogin() != null) {
      ruleResponse.setNoteLogin(ruleDoc.noteLogin());
    }
  }

  private static void setSeverity(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.SEVERITY) && ruleDoc.severity() != null) {
      ruleResponse.setSeverity(ruleDoc.severity());
    }
  }

  private static void setInternalKey(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.INTERNAL_KEY) && ruleDoc.internalKey() != null) {
      ruleResponse.setInternalKey(ruleDoc.internalKey());
    }
  }

  private static void setLanguage(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.LANGUAGE) && ruleDoc.language() != null) {
      ruleResponse.setLang(ruleDoc.language());
    }
  }

  private void setLanguageName(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, "langName") && ruleDoc.language() != null) {
      String languageKey = ruleDoc.language();
      Language language = languages.get(languageKey);
      ruleResponse.setLangName(language == null ? languageKey : language.getName());
    }
  }

  private static void setIsTemplate(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.IS_TEMPLATE)) {
      ruleResponse.setIsTemplate(ruleDoc.isTemplate());
    }
  }

  private static void setTemplateKey(Rules.Rule.Builder ruleResponse, Rule ruleDoc, Set<String> fieldsToReturn) {
    if (shouldReturnField(fieldsToReturn, RuleNormalizer.RuleField.TEMPLATE_KEY) && ruleDoc.templateKey() != null) {
      RuleKey templateKey = ruleDoc.templateKey();
      if (templateKey != null) {
        ruleResponse.setTemplateKey(templateKey.toString());
      }
    }
  }

  private static boolean shouldReturnField(Set<String> fieldsToReturn, IndexField field) {
    return fieldsToReturn.isEmpty() || fieldsToReturn.contains(field.field());
  }

  private static boolean shouldReturnField(Set<String> fieldsToReturn, String fieldName) {
    return fieldsToReturn.isEmpty() || fieldsToReturn.contains(fieldName);
  }

  private static void buildCharacteristicRuleResponse(Rules.Rule.Builder ruleResponse, Rule ruleDoc, RuleMappingContext context) {
    String ruleCharacteristic = ruleToCharacteristic(ruleDoc);
    if (ruleCharacteristic != null) {
      ruleResponse.setDebtChar(ruleCharacteristic);
      String ruleCharacteristicName = ruleToCharacteristicName(ruleDoc, context);
      if (ruleCharacteristicName != null) {
        ruleResponse.setDebtCharName(ruleCharacteristicName);
      }
    }
  }

  private static void buildDebtSubCharacteristicRuleResponse(Rules.Rule.Builder ruleResponse, Rule ruleDoc, RuleMappingContext context) {
    String ruleSubCharacteristic = ruleToSubCharacteristic(ruleDoc);
    if (ruleSubCharacteristic != null) {
      ruleResponse.setDebtSubChar(ruleSubCharacteristic);
      String ruleSubCharacteristicName = ruleToSubCharacteristicName(ruleDoc, context);
      if (ruleSubCharacteristicName != null) {
        ruleResponse.setDebtSubCharName(ruleSubCharacteristicName);
      }
    }
  }

  private static boolean needDebtCharacteristicNames(@Nullable QueryContext context) {
    return context == null || context.getFieldsToReturn().contains("debtCharName");
  }

  private static boolean needDebtSubCharacteristicNames(@Nullable QueryContext context) {
    return context == null || context.getFieldsToReturn().contains("debtSubCharName");
  }

  @CheckForNull
  private static String ruleToCharacteristic(Rule rule) {
    String debtCharacteristicKey = rule.debtCharacteristicKey();
    if (debtCharacteristicKey != null && !DebtCharacteristic.NONE.equals(debtCharacteristicKey)) {
      return debtCharacteristicKey;
    }
    return null;
  }

  @CheckForNull
  private static String ruleToSubCharacteristic(Rule rule) {
    String debtSubCharacteristicKey = rule.debtSubCharacteristicKey();
    if (debtSubCharacteristicKey != null && !DebtCharacteristic.NONE.equals(debtSubCharacteristicKey)) {
      return debtSubCharacteristicKey;
    }

    return null;
  }

  private static String ruleToCharacteristicName(Rule rule, RuleMappingContext context) {
    return context.debtCharacteristicName(rule.debtCharacteristicKey());
  }

  private static String ruleToSubCharacteristicName(Rule rule, RuleMappingContext context) {
    return context.debtCharacteristicName(rule.debtSubCharacteristicKey());
  }

  private static boolean ruleToOverloaded(Rule rule) {
    return rule.debtOverloaded();
  }

  private static class SimpleMapper extends IndexMapper<RuleDoc, RuleMappingContext> {
    private SimpleMapper(String... fields) {
      super(fields);
    }

    @Override
    public void write(JsonWriter json, RuleDoc doc, RuleMappingContext context) {
      // do not do anything
    }
  }

  private enum RuleParamToResponseRuleParam implements Function<RuleParam, Rules.Rule.Param> {
    INSTANCE;

    @Override
    public Rules.Rule.Param apply(@Nonnull RuleParam param) {
      Rules.Rule.Param.Builder paramResponse = Rules.Rule.Param.newBuilder();
      paramResponse.setKey(param.key());
      if (param.description() != null) {
        paramResponse.setHtmlDesc(Markdown.convertToHtml(param.description()));
      }
      if (param.defaultValue() != null) {
        paramResponse.setDefaultValue(param.defaultValue());
      }
      if (param.type() != null) {
        paramResponse.setType(param.type().type());
      }

      return paramResponse.build();
    }
  }
}

class RuleMappingContext {
  private final Map<String, String> debtCharacteristicNamesByKey = Maps.newHashMap();

  @CheckForNull
  public String debtCharacteristicName(@Nullable String key) {
    return debtCharacteristicNamesByKey.get(key);
  }

  void add(@Nullable DebtCharacteristic c) {
    if (c != null) {
      debtCharacteristicNamesByKey.put(c.key(), c.name());
    }
  }
}
