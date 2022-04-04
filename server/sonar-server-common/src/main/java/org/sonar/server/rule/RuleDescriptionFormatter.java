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

import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.markdown.Markdown;

import static java.lang.String.format;

public class RuleDescriptionFormatter {

  private RuleDescriptionFormatter() { /* static helpers */ }

  public static String getDescriptionAsHtml(RuleDefinitionDto ruleDto) {
    String description = ruleDto.getDescription();
    RuleDto.Format descriptionFormat = ruleDto.getDescriptionFormat();
    if (description != null && descriptionFormat != null) {
      switch (descriptionFormat) {
        case MARKDOWN:
          return Markdown.convertToHtml(description);
        case HTML:
          return description;
        default:
          throw new IllegalStateException(format("Rule description format '%s' is unknown for key '%s'", descriptionFormat, ruleDto.getKey().toString()));
      }
    }
    return null;
  }

}
