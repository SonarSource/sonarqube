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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.check.Cardinality;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.server.debt.DebtModelPluginRepository;
import org.sonar.server.debt.DebtModelXMLExporter;
import org.sonar.server.debt.DebtRulesXMLImporter;

import javax.annotation.CheckForNull;

import java.io.Reader;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;

/**
 * Inject deprecated RuleRepository into RuleDefinitions for backward-compatibility.
 *
 * @since 4.2
 */
public class DeprecatedRulesDefinition implements RulesDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(DeprecatedRulesDefinition.class);

  private final RuleI18nManager i18n;
  private final RuleRepository[] repositories;

  private final DebtModelPluginRepository languageModelFinder;
  private final DebtRulesXMLImporter importer;

  public DeprecatedRulesDefinition(RuleI18nManager i18n, RuleRepository[] repositories, DebtModelPluginRepository languageModelFinder, DebtRulesXMLImporter importer) {
    this.i18n = i18n;
    this.repositories = repositories;
    this.languageModelFinder = languageModelFinder;
    this.importer = importer;
  }

  public DeprecatedRulesDefinition(RuleI18nManager i18n, DebtModelPluginRepository languageModelFinder, DebtRulesXMLImporter importer) {
    this(i18n, new RuleRepository[0], languageModelFinder, importer);
  }

  @Override
  public void define(Context context) {
    // Load rule debt definitions from xml files provided by plugin
    List<RuleDebt> ruleDebts = loadRuleDebtList();

    for (RuleRepository repository : repositories) {
      // RuleRepository API does not handle difference between new and extended repositories,
      NewRepository newRepository;
      if (context.repository(repository.getKey()) == null) {
        newRepository = context.createRepository(repository.getKey(), repository.getLanguage());
        newRepository.setName(repository.getName());
      } else {
        newRepository = (NewRepository) context.extendRepository(repository.getKey(), repository.getLanguage());
      }
      for (org.sonar.api.rules.Rule rule : repository.createRules()) {
        NewRule newRule = newRepository.createRule(rule.getKey());
        newRule.setName(ruleName(repository.getKey(), rule));
        newRule.setHtmlDescription(ruleDescription(repository.getKey(), rule));
        newRule.setInternalKey(rule.getConfigKey());
        newRule.setTemplate(Cardinality.MULTIPLE.equals(rule.getCardinality()));
        newRule.setSeverity(rule.getSeverity().toString());
        newRule.setStatus(rule.getStatus() == null ? RuleStatus.defaultStatus() : RuleStatus.valueOf(rule.getStatus()));
        newRule.setTags(rule.getTags());
        for (RuleParam param : rule.getParams()) {
          NewParam newParam = newRule.createParam(param.getKey());
          newParam.setDefaultValue(param.getDefaultValue());
          newParam.setDescription(paramDescription(repository.getKey(), rule.getKey(), param));
          newParam.setType(RuleParamType.parse(param.getType()));
        }
        updateRuleDebtDefinitions(newRule, repository.getKey(), rule.getKey(), ruleDebts);
      }
      newRepository.done();
    }
  }

  private void updateRuleDebtDefinitions(NewRule newRule, String repoKey, String ruleKey, List<RuleDebt> ruleDebts) {
    RuleDebt ruleDebt = findRequirement(ruleDebts, repoKey, ruleKey);
    if (ruleDebt != null) {
      newRule.setDebtSubCharacteristic(ruleDebt.subCharacteristicKey());
      String function = ruleDebt.function();
      String coefficient = ruleDebt.coefficient();
      String offset = ruleDebt.offset();

      if (DebtRemediationFunction.Type.LINEAR.name().equals(function) && coefficient != null) {
        newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().linear(coefficient));
      } else if (DebtRemediationFunction.Type.CONSTANT_ISSUE.name().equals(function) && offset != null) {
        newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().constantPerIssue(offset));
      } else if (DebtRemediationFunction.Type.LINEAR_OFFSET.name().equals(function) && coefficient != null && offset != null) {
        newRule.setDebtRemediationFunction(newRule.debtRemediationFunctions().linearWithOffset(coefficient, offset));
      } else {
        throw new IllegalArgumentException(String.format("Debt definition on rule '%s:%s' is invalid", repoKey, ruleKey));
      }
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
