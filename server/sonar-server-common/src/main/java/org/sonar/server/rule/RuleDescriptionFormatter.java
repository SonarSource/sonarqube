/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.markdown.Markdown;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.INTRODUCTION_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.DEFAULT_KEY;
import static org.sonar.db.rule.RuleDto.Format.MARKDOWN;

public class RuleDescriptionFormatter {

  public static final List<String> SECTION_KEYS = List.of(
    INTRODUCTION_SECTION_KEY,
    ROOT_CAUSE_SECTION_KEY,
    ASSESS_THE_PROBLEM_SECTION_KEY,
    HOW_TO_FIX_SECTION_KEY,
    RESOURCES_SECTION_KEY);

  public static final Map<String, String> HOTSPOT_SECTION_TITLES = Map.of(
    ROOT_CAUSE_SECTION_KEY, "What is the risk?",
    ASSESS_THE_PROBLEM_SECTION_KEY, "Assess the risk",
    HOW_TO_FIX_SECTION_KEY, "How can you fix it?"
  );

  public static final Map<String, String> RULE_SECTION_TITLES = Map.of(
    ROOT_CAUSE_SECTION_KEY, "Why is this an issue?",
    HOW_TO_FIX_SECTION_KEY, "How to fix it?",
    RESOURCES_SECTION_KEY, "Resources"
  );

  @CheckForNull
  public String getDescriptionAsHtml(RuleDto ruleDto) {
    if (ruleDto.getDescriptionFormat() == null) {
      return null;
    }
    Collection<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = ruleDto.getRuleDescriptionSectionDtos();
    return retrieveDescription(ruleDescriptionSectionDtos, RuleType.valueOf(ruleDto.getType()), Objects.requireNonNull(ruleDto.getDescriptionFormat()));
  }

  @CheckForNull
  private static String retrieveDescription(Collection<RuleDescriptionSectionDto> ruleDescriptionSectionDtos,
    RuleType ruleType, RuleDto.Format descriptionFormat) {
    if (ruleDescriptionSectionDtos.isEmpty()) {
      return null;
    }
    Map<String, String> sectionKeyToHtml = ruleDescriptionSectionDtos.stream()
      //TODO MMF-2765, merge operation on toMap should not be needed anymore
      .sorted(comparing(RuleDescriptionSectionDto::getKey).thenComparing(s -> Optional.ofNullable(s.getContext()).map(RuleDescriptionSectionContextDto::getKey).orElse(null)))
      .collect(toMap(RuleDescriptionSectionDto::getKey, section -> toHtml(descriptionFormat, section), (k1, k2) -> k1));
    if (sectionKeyToHtml.containsKey(DEFAULT_KEY)) {
      return sectionKeyToHtml.get(DEFAULT_KEY);
    } else {
      return concatHtmlSections(sectionKeyToHtml, ruleType);
    }
  }

  private static String concatHtmlSections(Map<String, String> sectionKeyToHtml, RuleType ruleType) {
    Map<String, String> titleMapping = ruleType.equals(RuleType.SECURITY_HOTSPOT) ? HOTSPOT_SECTION_TITLES : RULE_SECTION_TITLES;
    var builder = new StringBuilder();
    for (String sectionKey : SECTION_KEYS) {
      if (sectionKeyToHtml.containsKey(sectionKey)) {
        Optional.ofNullable(titleMapping.get(sectionKey)).ifPresent(title -> builder.append("<h2>").append(title).append("</h2>"));
        builder.append(sectionKeyToHtml.get(sectionKey)).append("<br/>");
      }
    }
    return builder.toString();
  }

  private static String toHtml(RuleDto.Format descriptionFormat, RuleDescriptionSectionDto ruleDescriptionSectionDto) {
    if (MARKDOWN.equals(descriptionFormat)) {
      return Markdown.convertToHtml(ruleDescriptionSectionDto.getContent());
    } else {
      return ruleDescriptionSectionDto.getContent();
    }
  }

}
