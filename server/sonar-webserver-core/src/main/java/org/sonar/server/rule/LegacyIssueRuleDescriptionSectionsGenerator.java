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

import java.util.EnumSet;
import java.util.Set;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.rule.RuleDescriptionSectionDto;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;

public class LegacyIssueRuleDescriptionSectionsGenerator implements RuleDescriptionSectionsGenerator {
  private static final Set<RuleType> ISSUE_RULE_TYPES = EnumSet.of(CODE_SMELL, BUG, VULNERABILITY);

  private final UuidFactory uuidFactory;

  public LegacyIssueRuleDescriptionSectionsGenerator(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  @Override
  public boolean isGeneratorForRule(RulesDefinition.Rule rule) {
    return ISSUE_RULE_TYPES.contains(rule.type()) && rule.ruleDescriptionSections().isEmpty();
  }

  @Override
  public Set<RuleDescriptionSectionDto> generateSections(RulesDefinition.Rule rule) {
    if (isNotEmpty(rule.htmlDescription())) {
      return singleton(createDefaultRuleDescriptionSection(uuidFactory.create(), rule.htmlDescription()));
    } else if (isNotEmpty(rule.markdownDescription())) {
      return singleton(createDefaultRuleDescriptionSection(uuidFactory.create(), rule.markdownDescription()));
    }
    return emptySet();
  }
}
