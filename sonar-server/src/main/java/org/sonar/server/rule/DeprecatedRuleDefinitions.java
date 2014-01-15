/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleDefinitions;
import org.sonar.api.rule.RuleParamType;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RuleRepository;
import org.sonar.check.Cardinality;
import org.sonar.core.i18n.RuleI18nManager;

import javax.annotation.CheckForNull;

/**
 * Inject deprecated RuleRepository into RuleDefinitions for backward-compatibility.
 *
 * @since 4.2
 */
public class DeprecatedRuleDefinitions implements RuleDefinitions {
  private final RuleI18nManager i18n;
  private final RuleRepository[] repositories;

  public DeprecatedRuleDefinitions(RuleI18nManager i18n, RuleRepository[] repositories) {
    this.i18n = i18n;
    this.repositories = repositories;
  }

  public DeprecatedRuleDefinitions(RuleI18nManager i18n) {
    this(i18n, new RuleRepository[0]);
  }

  @Override
  public void define(Context context) {
    for (RuleRepository repository : repositories) {
      // RuleRepository API does not handle difference between new and extended repositories,
      NewRepository newRepository;
      if (context.repository(repository.getKey()) == null) {
        newRepository = context.newRepository(repository.getKey(), repository.getLanguage());
        newRepository.setName(repository.getName());
      } else {
        newRepository = (NewRepository) context.extendRepository(repository.getKey(), repository.getLanguage());
      }
      for (org.sonar.api.rules.Rule rule : repository.createRules()) {
        NewRule newRule = newRepository.newRule(rule.getKey());
        newRule.setName(ruleName(repository.getKey(), rule));
        newRule.setHtmlDescription(ruleDescription(repository.getKey(), rule));
        newRule.setMetadata(rule.getConfigKey());
        newRule.setTemplate(Cardinality.MULTIPLE.equals(rule.getCardinality()));
        newRule.setDefaultSeverity(rule.getSeverity().toString());
        newRule.setStatus(rule.getStatus() == null ? Status.READY : Status.valueOf(rule.getStatus()));
        for (RuleParam param : rule.getParams()) {
          NewParam newParam = newRule.newParam(param.getKey());
          newParam.setDefaultValue(param.getDefaultValue());
          newParam.setDescription(paramDescription(repository.getKey(), rule.getKey(), param));
          newParam.setType(RuleParamType.parse(param.getType()));
        }
      }
      newRepository.done();
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
}
