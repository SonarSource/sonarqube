/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleForIndexingDto;

import static java.lang.Character.isWhitespace;
import static java.util.Optional.ofNullable;

public class HotspotRuleDescription {
  private static final HotspotRuleDescription NO_DESCRIPTION = new HotspotRuleDescription(null, null, null);

  @CheckForNull
  private final String risk;
  @CheckForNull
  private final String vulnerable;
  @CheckForNull
  private final String fixIt;

  private HotspotRuleDescription(@Nullable String risk, @Nullable String vulnerable, @Nullable String fixIt) {
    this.risk = risk;
    this.vulnerable = vulnerable;
    this.fixIt = fixIt;
  }

  public static HotspotRuleDescription from(RuleDefinitionDto dto) {
    String description = dto.getDescription();
    return from(description);
  }

  public static HotspotRuleDescription from(RuleForIndexingDto dto) {
    return from(dto.getDescription());
  }

  private static HotspotRuleDescription from(@Nullable String description) {
    if (description == null) {
      return NO_DESCRIPTION;
    }

    String vulnerableTitle = "<h2>Ask Yourself Whether</h2>";
    String fixItTitle = "<h2>Recommended Secure Coding Practices</h2>";
    int vulnerableTitlePosition = description.indexOf(vulnerableTitle);
    int fixItTitlePosition = description.indexOf(fixItTitle);
    if (vulnerableTitlePosition == -1 && fixItTitlePosition == -1) {
      return NO_DESCRIPTION;
    }

    if (vulnerableTitlePosition == -1) {
      return new HotspotRuleDescription(
        trimingSubstring(description, 0, fixItTitlePosition),
        null,
        trimingSubstring(description, fixItTitlePosition, description.length())
      );
    }
    if (fixItTitlePosition == -1) {
      return new HotspotRuleDescription(
        trimingSubstring(description, 0, vulnerableTitlePosition),
        trimingSubstring(description, vulnerableTitlePosition, description.length()),
        null
      );
    }
    return new HotspotRuleDescription(
      trimingSubstring(description, 0, vulnerableTitlePosition),
      trimingSubstring(description, vulnerableTitlePosition, fixItTitlePosition),
      trimingSubstring(description, fixItTitlePosition, description.length())
    );
  }

  @CheckForNull
  private static String trimingSubstring(String description, int beginIndex, int endIndex) {
    if (beginIndex == endIndex) {
      return null;
    }

    int trimmedBeginIndex = beginIndex;
    while (trimmedBeginIndex < endIndex && isWhitespace(description.charAt(trimmedBeginIndex))) {
      trimmedBeginIndex++;
    }
    int trimmedEndIndex = endIndex;
    while (trimmedEndIndex > 0 && trimmedEndIndex > trimmedBeginIndex && isWhitespace(description.charAt(trimmedEndIndex - 1))) {
      trimmedEndIndex--;
    }
    if (trimmedBeginIndex == trimmedEndIndex) {
      return null;
    }

    return description.substring(trimmedBeginIndex, trimmedEndIndex);
  }

  public Optional<String> getRisk() {
    return ofNullable(risk);
  }

  public Optional<String> getVulnerable() {
    return ofNullable(vulnerable);
  }

  public Optional<String> getFixIt() {
    return ofNullable(fixIt);
  }

  public boolean isComplete() {
    return risk != null && vulnerable != null && fixIt != null;
  }
  @Override
  public String toString() {
    return "HotspotRuleDescription{" +
      "risk='" + risk + '\'' +
      ", vulnerable='" + vulnerable + '\'' +
      ", fixIt='" + fixIt + '\'' +
      '}';
  }

}
