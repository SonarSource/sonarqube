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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.server.qualityprofile.ActiveRuleChange;

/**
 * Builder for generating a text description of the changes made to a quality profile.
 */
public final class QualityProfileTextGenerator {

  private static final Map<ActiveRuleChange.Type, String> CHANGE_TO_TEXT_MAP = Map.ofEntries(
    Map.entry(ActiveRuleChange.Type.ACTIVATED, " new rule"),
    Map.entry(ActiveRuleChange.Type.DEACTIVATED, " deactivated rule"),
    Map.entry(ActiveRuleChange.Type.UPDATED, " modified rule")
  );

  private QualityProfileTextGenerator() {
    // only static methods
  }

  /**
   * Returns a text description of the changes made to a quality profile. Oxford comma is not used.
   * The order of the changes is based on the order of the enum name (activated, deactivated, updated) to keep consistency.
   * 0 values are filtered out.
   *
   * @param changesMappedToNumberOfRules the changes mapped to the number of rules
   * @return a text description of the changes made to the profile
   */
  public static String generateRuleChangeText(Map<ActiveRuleChange.Type, Long> changesMappedToNumberOfRules) {

    return changesMappedToNumberOfRules.entrySet().stream()
      .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
      .filter(entry -> entry.getValue() > 0)
      .map(entry -> generateRuleText(entry.getValue(), CHANGE_TO_TEXT_MAP.get(entry.getKey())))
      .collect(Collectors.collectingAndThen(Collectors.toList(), joiningLastDelimiter(", ", " and ")));
  }

  private static String generateRuleText(Long ruleNumber, String ruleText) {
    return ruleNumber + ruleText + (ruleNumber > 1 ? "s" : "");
  }

  private static Function<List<String>, String> joiningLastDelimiter(String delimiter, String lastDelimiter) {
    return list -> {
      int last = list.size() - 1;
      if (last < 1) return String.join(delimiter, list);
      return String.join(lastDelimiter,
        String.join(delimiter, list.subList(0, last)),
        list.get(last));
    };
  }
}
