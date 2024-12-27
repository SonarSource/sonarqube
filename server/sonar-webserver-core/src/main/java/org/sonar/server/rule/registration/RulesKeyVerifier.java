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
package org.sonar.server.rule.registration;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.intersection;
import static java.lang.String.format;

public class RulesKeyVerifier {

  void verifyRuleKeyConsistency(List<RulesDefinition.Repository> repositories, RulesRegistrationContext rulesRegistrationContext) {
    List<RulesDefinition.Rule> definedRules = repositories.stream()
      .flatMap(r -> r.rules().stream())
      .toList();

    Set<RuleKey> definedRuleKeys = definedRules.stream()
      .map(r -> RuleKey.of(r.repository().key(), r.key()))
      .collect(Collectors.toSet());

    List<RuleKey> definedDeprecatedRuleKeys = definedRules.stream()
      .flatMap(r -> r.deprecatedRuleKeys().stream())
      .toList();

    // Find duplicates in declared deprecated rule keys
    Set<RuleKey> duplicates = findDuplicates(definedDeprecatedRuleKeys);
    checkState(duplicates.isEmpty(), "The following deprecated rule keys are declared at least twice [%s]",
      duplicates.isEmpty() ? "" : duplicates.stream().map(RuleKey::toString).collect(Collectors.joining(",")));

    // Find rule keys that are both deprecated and used
    Set<RuleKey> intersection = intersection(new HashSet<>(definedRuleKeys), new HashSet<>(definedDeprecatedRuleKeys)).immutableCopy();
    checkState(intersection.isEmpty(), "The following rule keys are declared both as deprecated and used key [%s]",
      intersection.isEmpty() ? "" : intersection.stream().map(RuleKey::toString).collect(Collectors.joining(",")));

    // Find incorrect usage of deprecated keys
    Map<RuleKey, SingleDeprecatedRuleKey> dbDeprecatedRuleKeysByOldRuleKey = rulesRegistrationContext.getDbDeprecatedKeysByOldRuleKey();

    Set<String> incorrectRuleKeyMessage = definedRules.stream()
      .flatMap(r -> filterInvalidDeprecatedRuleKeys(dbDeprecatedRuleKeysByOldRuleKey, r))
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    checkState(incorrectRuleKeyMessage.isEmpty(), "An incorrect state of deprecated rule keys has been detected.\n %s",
      incorrectRuleKeyMessage.isEmpty() ? "" : String.join("\n", incorrectRuleKeyMessage));
  }

  private static Stream<String> filterInvalidDeprecatedRuleKeys(Map<RuleKey, SingleDeprecatedRuleKey> dbDeprecatedRuleKeysByOldRuleKey, RulesDefinition.Rule rule) {
    return rule.deprecatedRuleKeys().stream()
      .map(rk -> {
        SingleDeprecatedRuleKey singleDeprecatedRuleKey = dbDeprecatedRuleKeysByOldRuleKey.get(rk);
        if (singleDeprecatedRuleKey == null) {
          // new deprecated rule key : OK
          return null;
        }
        RuleKey parentRuleKey = RuleKey.of(rule.repository().key(), rule.key());
        if (parentRuleKey.equals(singleDeprecatedRuleKey.getNewRuleKeyAsRuleKey())) {
          // same parent : OK
          return null;
        }
        if (rule.deprecatedRuleKeys().contains(singleDeprecatedRuleKey.getNewRuleKeyAsRuleKey())) {
          // the new rule is deprecating the old parentRuleKey : OK
          return null;
        }
        return format("The deprecated rule key [%s] was previously deprecated by [%s]. [%s] should be a deprecated key of [%s],",
          rk.toString(),
          singleDeprecatedRuleKey.getNewRuleKeyAsRuleKey().toString(),
          singleDeprecatedRuleKey.getNewRuleKeyAsRuleKey().toString(),
          RuleKey.of(rule.repository().key(), rule.key()).toString());
      });
  }

  private static <T> Set<T> findDuplicates(Collection<T> list) {
    Set<T> duplicates = new HashSet<>();
    Set<T> uniques = new HashSet<>();

    list.forEach(t -> {
      if (!uniques.add(t)) {
        duplicates.add(t);
      }
    });

    return duplicates;
  }
}
