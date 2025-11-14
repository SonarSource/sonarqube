/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.server.rule.Context;
import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;

public class AdvancedRuleDescriptionSectionsGenerator implements RuleDescriptionSectionsGenerator {
  private final UuidFactory uuidFactory;

  public AdvancedRuleDescriptionSectionsGenerator(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  @Override
  public boolean isGeneratorForRule(RulesDefinition.Rule rule) {
    return !rule.ruleDescriptionSections().isEmpty();
  }

  @Override
  public Set<RuleDescriptionSectionDto> generateSections(RulesDefinition.Rule rule) {
    return rule.ruleDescriptionSections().stream()
      .map(this::toRuleDescriptionSectionDto)
      .collect(Collectors.toSet());
  }

  private RuleDescriptionSectionDto toRuleDescriptionSectionDto(RuleDescriptionSection section) {
    return RuleDescriptionSectionDto.builder()
      .uuid(uuidFactory.create())
      .key(section.getKey())
      .content(section.getHtmlContent())
      .context(toRuleDescriptionSectionContextDto(section.getContext()))
      .build();
  }

  @Nullable
  private static RuleDescriptionSectionContextDto toRuleDescriptionSectionContextDto(Optional<Context> context) {
    return context
      .map(c -> RuleDescriptionSectionContextDto.of(c.getKey(), c.getDisplayName()))
      .orElse(null);
  }

}
