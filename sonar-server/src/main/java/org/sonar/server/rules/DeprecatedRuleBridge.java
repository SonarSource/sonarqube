/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.rules;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.RulesRepository;
import org.sonar.api.utils.SonarException;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridge for the deprecated extension "org.sonar.api.rules.RulesRepository"
 */
public final class DeprecatedRuleBridge extends RuleRepository {

  private RulesRepository deprecatedRepository;
  private DefaultServerFileSystem fileSystem;

  public DeprecatedRuleBridge(String repositoryKey, String repositoryName, RulesRepository deprecatedRepository, DefaultServerFileSystem fileSystem) {
    super(repositoryKey, deprecatedRepository.getLanguage().getKey());
    this.deprecatedRepository = deprecatedRepository;
    this.fileSystem = fileSystem;
    setName(repositoryName);
  }

  @Override
  public List<Rule> createRules() {
    List<Rule> rules = new ArrayList<Rule>();
    registerRules(rules);
    registerRuleExtensions(rules);
    return rules;
  }

  private void registerRules(List<Rule> rules) {
    List<Rule> deprecatedRules = deprecatedRepository.getInitialReferential();
    if (deprecatedRules != null) {
      for (Rule deprecatedRule : deprecatedRules) {
        rules.add(cloneRule(deprecatedRule));
      }
    }
  }

  private void registerRuleExtensions(List<Rule> rules) {
    for (File file : fileSystem.getPluginExtensionXml(getKey())) {
      try {
        String fileContent = FileUtils.readFileToString(file, CharEncoding.UTF_8);
        List<Rule> deprecatedRules = deprecatedRepository.parseReferential(fileContent);
        if (deprecatedRules != null) {
          for (Rule deprecatedRule : deprecatedRules) {
            rules.add(cloneRule(deprecatedRule));
          }
        }
      } catch (IOException e) {
        throw new SonarException("Can not read the file: " + file.getPath(), e);
      }
    }
  }

  private Rule cloneRule(Rule deprecatedRule) {
    Rule rule = Rule.create(getKey(), deprecatedRule.getKey(), deprecatedRule.getName());
    rule.setRulesCategory(deprecatedRule.getRulesCategory());
    rule.setConfigKey(deprecatedRule.getConfigKey());
    rule.setPriority(deprecatedRule.getPriority());
    rule.setDescription(deprecatedRule.getDescription());
    rule.setEnabled(true);
    if (deprecatedRule.getParams() != null) {
      for (RuleParam deprecatedParam : deprecatedRule.getParams()) {
        rule.createParameter(deprecatedParam.getKey())
            .setDescription(deprecatedParam.getDescription())
            .setType(deprecatedParam.getType());
      }
    }
    return rule;
  }
}
