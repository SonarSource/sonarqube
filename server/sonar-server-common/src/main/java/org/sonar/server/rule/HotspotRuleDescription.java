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

import static java.util.Optional.ofNullable;

/***
 * Temporary rule split before we have a better
 * solution to specify security hotspot.
 */
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

    String[] split = extractSection("", description);
    description = split[0];
    String ruleDescriptionSection = split[1];

    split = extractSection("<h2>Exceptions</h2>", description);
    description = split[0];
    String exceptions = split[1];

    split = extractSection("<h2>Ask Yourself Whether</h2>", description);
    description = split[0];
    String askSection = split[1];

    split = extractSection("<h2>Sensitive Code Example</h2>", description);
    description = split[0];
    String sensitiveSection = split[1];

    split = extractSection("<h2>Noncompliant Code Example</h2>", description);
    description = split[0];
    String noncompliantSection = split[1];

    split = extractSection("<h2>Recommended Secure Coding Practices</h2>", description);
    description = split[0];
    String recommendedSection = split[1];

    split = extractSection("<h2>Compliant Solution</h2>", description);
    description = split[0];
    String compliantSection = split[1];

    split = extractSection("<h2>See</h2>", description);
    description = split[0];
    String seeSection = split[1];

    return new HotspotRuleDescription(
      trimToNull(ruleDescriptionSection + exceptions + description),
      trimToNull(askSection + sensitiveSection + noncompliantSection),
      trimToNull(recommendedSection + compliantSection + seeSection));
  }

  private static String trimToNull(String input) {
    return input.isEmpty() ? null : input;
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
