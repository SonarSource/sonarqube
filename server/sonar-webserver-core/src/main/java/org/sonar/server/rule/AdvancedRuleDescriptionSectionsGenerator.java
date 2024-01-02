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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.elasticsearch.common.util.set.Sets;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.server.rule.Context;
import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;

import static org.sonar.api.rules.RuleType.*;

public class AdvancedRuleDescriptionSectionsGenerator implements RuleDescriptionSectionsGenerator {
  private final UuidFactory uuidFactory;
  private final LegacyIssueRuleDescriptionSectionsGenerator legacyIssueRuleDescriptionSectionsGenerator;

  public AdvancedRuleDescriptionSectionsGenerator(UuidFactory uuidFactory, LegacyIssueRuleDescriptionSectionsGenerator legacyIssueRuleDescriptionSectionsGenerator) {
    this.uuidFactory = uuidFactory;
    this.legacyIssueRuleDescriptionSectionsGenerator = legacyIssueRuleDescriptionSectionsGenerator;
  }

  @Override
  public boolean isGeneratorForRule(RulesDefinition.Rule rule) {
    return !rule.ruleDescriptionSections().isEmpty() && skipHotspotRulesForSonar16635(rule);
  }

  private static boolean skipHotspotRulesForSonar16635(RulesDefinition.Rule rule) {
    return !SECURITY_HOTSPOT.equals(rule.type());
  }

  @Override
  public Set<RuleDescriptionSectionDto> generateSections(RulesDefinition.Rule rule) {
    Set<RuleDescriptionSectionDto> advancedSections = rule.ruleDescriptionSections().stream()
      .map(this::toRuleDescriptionSectionDto)
      .collect(Collectors.toSet());
    return addLegacySectionToAdvancedSections(advancedSections, rule);
  }

  /**
   * This was done to preserve backward compatibility with SonarLint until they stop using htmlDesc field in api/rules/[show|search] endpoints, see SONAR-16635
   * @deprecated the method should be removed once SonarLint supports rules.descriptionSections fields, I.E in 10.x
   */
  @Deprecated(since = "9.6", forRemoval = true)
  private Set<RuleDescriptionSectionDto> addLegacySectionToAdvancedSections(Set<RuleDescriptionSectionDto> advancedSections, RulesDefinition.Rule rule) {
    Set<RuleDescriptionSectionDto> legacySection = legacyIssueRuleDescriptionSectionsGenerator.generateSections(rule);
    return Sets.union(advancedSections, legacySection);
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
