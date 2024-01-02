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
package org.sonar.server.rule;

import java.util.Set;
import org.sonar.api.server.rule.RulesDefinition;

import static java.util.stream.Collectors.toSet;
import static org.sonar.api.utils.Preconditions.checkState;

public class RuleDescriptionSectionsGeneratorResolver {
  private final Set<RuleDescriptionSectionsGenerator> ruleDescriptionSectionsGenerators;

  RuleDescriptionSectionsGeneratorResolver(Set<RuleDescriptionSectionsGenerator> ruleDescriptionSectionsGenerators) {
    this.ruleDescriptionSectionsGenerators = ruleDescriptionSectionsGenerators;
  }

  RuleDescriptionSectionsGenerator getRuleDescriptionSectionsGenerator(RulesDefinition.Rule ruleDef) {
    Set<RuleDescriptionSectionsGenerator> generatorsFound = ruleDescriptionSectionsGenerators.stream()
      .filter(generator -> generator.isGeneratorForRule(ruleDef))
      .collect(toSet());
    checkState(generatorsFound.size() < 2, "More than one rule description section generator found for rule with key %s", ruleDef.key());
    checkState(!generatorsFound.isEmpty(), "No rule description section generator found for rule with key %s", ruleDef.key());
    return generatorsFound.iterator().next();
  }

}
