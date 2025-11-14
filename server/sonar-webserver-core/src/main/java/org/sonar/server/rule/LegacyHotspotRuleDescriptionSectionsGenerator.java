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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.rule.RuleTypeMapper;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.markdown.Markdown;

import static java.util.Collections.emptySet;
import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;

public class LegacyHotspotRuleDescriptionSectionsGenerator implements RuleDescriptionSectionsGenerator {
  private final UuidFactory uuidFactory;

  public LegacyHotspotRuleDescriptionSectionsGenerator(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  @Override
  public boolean isGeneratorForRule(RulesDefinition.Rule rule) {
    return SECURITY_HOTSPOT.equals(RuleTypeMapper.toRuleType(rule.type())) && rule.ruleDescriptionSections().isEmpty();
  }

  @Override
  public Set<RuleDescriptionSectionDto> generateSections(RulesDefinition.Rule rule) {
    return getDescriptionInHtml(rule)
      .map(this::generateSections)
      .orElse(emptySet());
  }

  private static Optional<String> getDescriptionInHtml(RulesDefinition.Rule rule) {
    if (rule.htmlDescription() != null) {
      return Optional.of(rule.htmlDescription());
    } else if (rule.markdownDescription() != null) {
      return Optional.of(Markdown.convertToHtml(rule.markdownDescription()));
    }
    return Optional.empty();
  }

  private Set<RuleDescriptionSectionDto> generateSections(String descriptionInHtml) {
    if (descriptionInHtml.isEmpty()) {
      return emptySet();
    }
    String[] split = extractSection("", descriptionInHtml);
    String remainingText = split[0];
    String ruleDescriptionSection = split[1];

    split = extractSection("<h2>Exceptions</h2>", remainingText);
    remainingText = split[0];
    String exceptions = split[1];

    split = extractSection("<h2>Ask Yourself Whether</h2>", remainingText);
    remainingText = split[0];
    String askSection = split[1];

    split = extractSection("<h2>Sensitive Code Example</h2>", remainingText);
    remainingText = split[0];
    String sensitiveSection = split[1];

    split = extractSection("<h2>Noncompliant Code Example</h2>", remainingText);
    remainingText = split[0];
    String noncompliantSection = split[1];

    split = extractSection("<h2>Recommended Secure Coding Practices</h2>", remainingText);
    remainingText = split[0];
    String recommendedSection = split[1];

    split = extractSection("<h2>Compliant Solution</h2>", remainingText);
    remainingText = split[0];
    String compliantSection = split[1];

    split = extractSection("<h2>See</h2>", remainingText);
    remainingText = split[0];
    String seeSection = split[1];

    RuleDescriptionSectionDto rootSection = createSection(ROOT_CAUSE_SECTION_KEY, ruleDescriptionSection, exceptions, remainingText);
    RuleDescriptionSectionDto assessSection = createSection(ASSESS_THE_PROBLEM_SECTION_KEY, askSection, sensitiveSection, noncompliantSection);
    RuleDescriptionSectionDto fixSection = createSection(HOW_TO_FIX_SECTION_KEY, recommendedSection, compliantSection, seeSection);

    // For backward compatibility with SonarLint, see SONAR-16635. Should be removed in 10.x
    RuleDescriptionSectionDto defaultSection = createDefaultRuleDescriptionSection(uuidFactory.create(), descriptionInHtml);

    return Stream.of(rootSection, assessSection, fixSection, defaultSection)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  private static String[] extractSection(String beginning, String description) {
    String endSection = "<h2>";
    int beginningIndex = description.indexOf(beginning);
    if (beginningIndex != -1) {
      int endIndex = description.indexOf(endSection, beginningIndex + beginning.length());
      if (endIndex == -1) {
        endIndex = description.length();
      }
      return new String[] {
        description.substring(0, beginningIndex) + description.substring(endIndex),
        description.substring(beginningIndex, endIndex)
      };
    } else {
      return new String[] {description, ""};
    }

  }

  @CheckForNull
  private RuleDescriptionSectionDto createSection(String key, String... contentPieces) {
    String content = trimToNull(String.join("", contentPieces));
    if (content == null) {
      return null;
    }
    return RuleDescriptionSectionDto.builder()
      .uuid(uuidFactory.create())
      .key(key)
      .content(content)
      .build();
  }

  @CheckForNull
  private static String trimToNull(String input) {
    return input.isEmpty() ? null : input;
  }

}
