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
package org.sonar.batch.rule;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.ProjectRepositories;

import java.util.Map.Entry;

/**
 * Loads the rules that are activated on the Quality profiles
 * used by the current project and build {@link org.sonar.api.batch.rule.ActiveRules}.
 */
public class ActiveRulesProvider extends ProviderAdapter {

  private ActiveRules singleton = null;

  public ActiveRules provide(ProjectRepositories ref) {
    if (singleton == null) {
      singleton = load(ref);
    }
    return singleton;
  }

  private ActiveRules load(ProjectRepositories ref) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (ActiveRule activeRule : ref.activeRules()) {
      NewActiveRule newActiveRule = builder.create(RuleKey.of(activeRule.repositoryKey(), activeRule.ruleKey()));
      newActiveRule.setName(activeRule.name());
      newActiveRule.setSeverity(activeRule.severity());
      newActiveRule.setLanguage(activeRule.language());
      newActiveRule.setInternalKey(activeRule.internalKey());
      newActiveRule.setTemplateRuleKey(activeRule.templateRuleKey());

      // load parameters
      for (Entry<String, String> param : activeRule.params().entrySet()) {
        newActiveRule.setParam(param.getKey(), param.getValue());
      }

      newActiveRule.activate();
    }
    return builder.build();
  }
}
