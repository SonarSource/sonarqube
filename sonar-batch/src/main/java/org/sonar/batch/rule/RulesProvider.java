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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.batch.rule.internal.RulesBuilder;
import org.sonar.api.batch.rule.internal.NewRule;
import org.sonar.batch.protocol.input.Rule;
import org.sonar.api.batch.rule.Rules;

public class RulesProvider extends ProviderAdapter {
  private Rules singleton = null;

  public Rules provide(RulesLoader ref) {
    if (singleton == null) {
      singleton = load(ref);
    }
    return singleton;
  }

  private static Rules load(RulesLoader ref) {
    RulesBuilder builder = new RulesBuilder();

    for (Rule inputRule : ref.load().getRules()) {
      NewRule newRule = builder.add(RuleKey.parse(inputRule.ruleKey()));
      newRule.setName(inputRule.name());
      newRule.setSeverity(inputRule.severity());
      newRule.setInternalKey(inputRule.internalKey());
    }

    return builder.build();
  }

}
