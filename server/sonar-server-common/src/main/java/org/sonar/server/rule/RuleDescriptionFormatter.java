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
import java.util.Objects;
import java.util.Optional;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleForIndexingDto;
import org.sonar.markdown.Markdown;

import static com.google.common.collect.MoreCollectors.toOptional;
import static java.lang.String.format;

public class RuleDescriptionFormatter {

  private RuleDescriptionFormatter() { /* static helpers */ }

  public static String getDescriptionAsHtml(RuleDefinitionDto ruleDefinitionDto) {
    if (ruleDefinitionDto.getDescriptionFormat() == null) {
      return null;
    }
    Collection<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = ruleDefinitionDto.getRuleDescriptionSectionDtos();
    return retrieveDescription(ruleDescriptionSectionDtos, ruleDefinitionDto.getRuleKey(), ruleDefinitionDto.getDescriptionFormat());
  }

  public static String getDescriptionAsHtml(RuleForIndexingDto ruleForIndexingDto) {
    if (ruleForIndexingDto.getDescriptionFormat() == null) {
      return null;
    }
    Collection<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = ruleForIndexingDto.getRuleDescriptionSectionsDtos();
    return retrieveDescription(ruleDescriptionSectionDtos, ruleForIndexingDto.getRuleKey().toString(), ruleForIndexingDto.getDescriptionFormat());
  }

  private static String retrieveDescription(Collection<RuleDescriptionSectionDto> ruleDescriptionSectionDtos,
    String ruleKey, RuleDto.Format descriptionFormat) {
    Optional<RuleDescriptionSectionDto> ruleDescriptionSectionDto = findDefaultDescription(ruleDescriptionSectionDtos);
    return ruleDescriptionSectionDto
      .map(ruleDescriptionSection -> toHtml(ruleKey, descriptionFormat, ruleDescriptionSection))
      .orElse(null);
  }


  private static Optional<RuleDescriptionSectionDto> findDefaultDescription(Collection<RuleDescriptionSectionDto> ruleDescriptionSectionDtos) {
    return ruleDescriptionSectionDtos.stream()
      .filter(RuleDescriptionSectionDto::isDefault)
      .collect(toOptional());
  }

  private static String toHtml(String ruleKey, RuleDto.Format descriptionFormat, RuleDescriptionSectionDto ruleDescriptionSectionDto) {
    RuleDto.Format nonNullDescriptionFormat = Objects.requireNonNull(descriptionFormat,
      "Rule " + descriptionFormat + " contains section(s) but has no format set");
    switch (nonNullDescriptionFormat) {
      case MARKDOWN:
        return Markdown.convertToHtml(ruleDescriptionSectionDto.getContent());
      case HTML:
        return ruleDescriptionSectionDto.getContent();
      default:
        throw new IllegalStateException(format("Rule description section format '%s' is unknown for rule key '%s'", descriptionFormat, ruleKey));
    }
  }

}
