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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.server.debt.DebtModelPluginRepository;
import org.sonar.server.debt.DebtModelXMLExporter;
import org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;
import org.sonar.server.debt.DebtRulesXMLImporter;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Reader;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Inject deprecated RuleRepository into {@link org.sonar.api.server.rule.RulesDefinition} for backward-compatibility.
 */
@ServerSide
public class DeprecatedRulesDefinitionLoader {

  private static final Logger LOG = Loggers.get(DeprecatedRulesDefinitionLoader.class);

  private final RuleI18nManager i18n;
  private final RuleRepository[] repositories;

  private final DebtModelPluginRepository languageModelFinder;
  private final DebtRulesXMLImporter importer;

  public DeprecatedRulesDefinitionLoader(RuleI18nManager i18n, DebtModelPluginRepository languageModelFinder, DebtRulesXMLImporter importer, RuleRepository[] repositories) {
    this.i18n = i18n;
    this.repositories = repositories;
    this.languageModelFinder = languageModelFinder;
    this.importer = importer;
  }

  /**
   * Used when no deprecated repositories
   */
  public DeprecatedRulesDefinitionLoader(RuleI18nManager i18n, DebtModelPluginRepository languageModelFinder, DebtRulesXMLImporter importer) {
    this(i18n, languageModelFinder, importer, new RuleRepository[0]);
  }

  void complete(RulesDefinition.Context context) {
    // Load rule debt definitions from xml files provided by plugin
    List<RuleDebt> ruleDebts = loadRuleDebtList();

    for (RuleRepository repository : repositories) {
      // RuleRepository API does not handle difference between new and extended repositories,
      RulesDefinition.NewRepository newRepository;
      if (context.repository(repository.getKey()) == null) {
        newRepository = context.createRepository(repository.getKey(), repository.getLanguage());
        newRepository.setName(repository.getName());
      } else {
        newRepository = (RulesDefinition.NewRepository) context.extendRepository(repository.getKey(), repository.getLanguage());
      }
      for (org.sonar.api.rules.Rule rule : repository.createRules()) {
        RulesDefinition.NewRule newRule = newRepository.createRule(rule.getKey());
        newRule.setName(ruleName(repository.getKey(), rule));
        newRule.setHtmlDescription(ruleDescription(repository.getKey(), rule));
        newRule.setInternalKey(rule.getConfigKey());
        newRule.setTemplate(rule.isTemplate());
        newRule.setSeverity(rule.getSeverity().toString());
        newRule.setStatus(rule.getStatus() == null ? RuleStatus.defaultStatus() : RuleStatus.valueOf(rule.getStatus()));
        newRule.setTags(rule.getTags());
        for (RuleParam param : rule.getParams()) {
          RulesDefinition.NewParam newParam = newRule.createParam(param.getKey());
          newParam.setDefaultValue(param.getDefaultValue());
          newParam.setDescription(paramDescription(repository.getKey(), rule.getKey(), param));
          newParam.setType(RuleParamType.parse(param.getType()));
        }
        updateRuleDebtDefinitions(newRule, repository.getKey(), rule.getKey(), ruleDebts);
      }
      newRepository.done();
    }
  }

  private void updateRuleDebtDefinitions(RulesDefinition.NewRule newRule, String repoKey, String ruleKey, List<RuleDebt> ruleDebts) {
    RuleDebt ruleDebt = findRequirement(ruleDebts, repoKey, ruleKey);
    if (ruleDebt != null) {
      newRule.setDebtSubCharacteristic(ruleDebt.subCharacteristicKey());
      newRule.setDebtRemediationFunction(remediationFunction(DebtRemediationFunction.Type.valueOf(ruleDebt.function()),
        ruleDebt.coefficient(),
        ruleDebt.offset(),
        newRule.debtRemediationFunctions(),
        repoKey, ruleKey
        ));
    }
  }

  private DebtRemediationFunction remediationFunction(DebtRemediationFunction.Type function, @Nullable String coefficient, @Nullable String offset,
    RulesDefinition.DebtRemediationFunctions functions, String repoKey, String ruleKey) {
    if (DebtRemediationFunction.Type.LINEAR.equals(function) && coefficient != null) {
      return functions.linear(coefficient);
    } else if (DebtRemediationFunction.Type.CONSTANT_ISSUE.equals(function) && offset != null) {
      return functions.constantPerIssue(offset);
    } else if (DebtRemediationFunction.Type.LINEAR_OFFSET.equals(function) && coefficient != null && offset != null) {
      return functions.linearWithOffset(coefficient, offset);
    } else {
      throw new IllegalArgumentException(String.format("Debt definition on rule '%s:%s' is invalid", repoKey, ruleKey));
    }
  }

  @CheckForNull
  private String ruleName(String repositoryKey, org.sonar.api.rules.Rule rule) {
    String name = i18n.getName(repositoryKey, rule.getKey());
    if (StringUtils.isNotBlank(name)) {
      return name;
    }
    return StringUtils.defaultIfBlank(rule.getName(), null);
  }

  @CheckForNull
  private String ruleDescription(String repositoryKey, org.sonar.api.rules.Rule rule) {
    String description = i18n.getDescription(repositoryKey, rule.getKey());
    if (StringUtils.isNotBlank(description)) {
      return description;
    }
    return StringUtils.defaultIfBlank(rule.getDescription(), null);
  }

  @CheckForNull
  private String paramDescription(String repositoryKey, String ruleKey, RuleParam param) {
    String desc = StringUtils.defaultIfEmpty(
      i18n.getParamDescription(repositoryKey, ruleKey, param.getKey()),
      param.getDescription()
      );
    return StringUtils.defaultIfBlank(desc, null);
  }

  public List<DebtModelXMLExporter.RuleDebt> loadRuleDebtList() {
    List<RuleDebt> ruleDebtList = newArrayList();
    for (String pluginKey : getContributingPluginListWithoutSqale()) {
      ruleDebtList.addAll(loadRuleDebtsFromXml(pluginKey));
    }
    return ruleDebtList;
  }

  public List<RuleDebt> loadRuleDebtsFromXml(String pluginKey) {
    Reader xmlFileReader = null;
    try {
      xmlFileReader = languageModelFinder.createReaderForXMLFile(pluginKey);
      ValidationMessages validationMessages = ValidationMessages.create();
      List<RuleDebt> rules = importer.importXML(xmlFileReader, validationMessages);
      validationMessages.log(LOG);
      return rules;
    } finally {
      IOUtils.closeQuietly(xmlFileReader);
    }
  }

  private Collection<String> getContributingPluginListWithoutSqale() {
    Collection<String> pluginList = newArrayList(languageModelFinder.getContributingPluginList());
    pluginList.remove(DebtModelPluginRepository.DEFAULT_MODEL);
    return pluginList;
  }

  @CheckForNull
  private RuleDebt findRequirement(List<RuleDebt> requirements, final String repoKey, final String ruleKey) {
    return Iterables.find(requirements, new Predicate<RuleDebt>() {
      @Override
      public boolean apply(RuleDebt input) {
        return input.ruleKey().equals(RuleKey.of(repoKey, ruleKey));
      }
    }, null);
  }

}
