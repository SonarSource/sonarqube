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

import com.google.common.collect.MoreCollectors;
import java.util.Collection;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.markdown.Markdown;

import static org.sonar.db.rule.RuleDto.Format.MARKDOWN;

public class RuleDescriptionFormatter {

  @CheckForNull
  public String getDescriptionAsHtml(RuleDto ruleDto) {
    if (ruleDto.getDescriptionFormat() == null) {
      return null;
    }
    Collection<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = ruleDto.getRuleDescriptionSectionDtos();
    return retrieveDescription(ruleDescriptionSectionDtos, Objects.requireNonNull(ruleDto.getDescriptionFormat()));
  }

  @CheckForNull
  private String retrieveDescription(Collection<RuleDescriptionSectionDto> ruleDescriptionSectionDtos, RuleDto.Format descriptionFormat) {
    return ruleDescriptionSectionDtos.stream()
      .filter(RuleDescriptionSectionDto::isDefault)
      .collect(MoreCollectors.toOptional())
      .map(section -> toHtml(descriptionFormat, section))
      .orElse(null);
  }

  public String toHtml(@Nullable RuleDto.Format descriptionFormat, RuleDescriptionSectionDto ruleDescriptionSectionDto) {
    if (MARKDOWN.equals(descriptionFormat)) {
      return Markdown.convertToHtml(ruleDescriptionSectionDto.getContent());
    }
    return ruleDescriptionSectionDto.getContent();
  }

}
