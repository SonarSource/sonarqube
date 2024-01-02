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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;

class RulesRegistrationContext {

  private static final Logger LOG = Loggers.get(RulesRegistrationContext.class);

  // initial immutable data
  private final Map<RuleKey, RuleDto> dbRules;
  private final Set<RuleDto> known;
  private final Map<String, Set<SingleDeprecatedRuleKey>> dbDeprecatedKeysByUuid;
  private final Map<String, List<RuleParamDto>> ruleParamsByRuleUuid;
  private final Map<RuleKey, RuleDto> dbRulesByDbDeprecatedKey;
  // mutable data
  private final Set<RuleDto> created = new HashSet<>();
  private final Map<RuleDto, RuleKey> renamed = new HashMap<>();
  private final Set<RuleDto> updated = new HashSet<>();
  private final Set<RuleDto> unchanged = new HashSet<>();
  private final Set<RuleDto> removed = new HashSet<>();

  private RulesRegistrationContext(Map<RuleKey, RuleDto> dbRules, Map<String, Set<SingleDeprecatedRuleKey>> dbDeprecatedKeysByUuid,
    Map<String, List<RuleParamDto>> ruleParamsByRuleUuid) {
    this.dbRules = ImmutableMap.copyOf(dbRules);
    this.known = ImmutableSet.copyOf(dbRules.values());
    this.dbDeprecatedKeysByUuid = dbDeprecatedKeysByUuid;
    this.ruleParamsByRuleUuid = ruleParamsByRuleUuid;
    this.dbRulesByDbDeprecatedKey = buildDbRulesByDbDeprecatedKey(dbDeprecatedKeysByUuid, dbRules);
  }

  private static Map<RuleKey, RuleDto> buildDbRulesByDbDeprecatedKey(Map<String, Set<SingleDeprecatedRuleKey>> dbDeprecatedKeysByUuid,
    Map<RuleKey, RuleDto> dbRules) {
    Map<String, RuleDto> dbRulesByRuleUuid = dbRules.values().stream()
      .collect(Collectors.toMap(RuleDto::getUuid, Function.identity()));

    Map<RuleKey, RuleDto> rulesByKey = new LinkedHashMap<>();
    for (Map.Entry<String, Set<SingleDeprecatedRuleKey>> entry : dbDeprecatedKeysByUuid.entrySet()) {
      String ruleUuid = entry.getKey();
      RuleDto rule = dbRulesByRuleUuid.get(ruleUuid);
      if (rule == null) {
        LOG.warn("Could not retrieve rule with uuid %s referenced by a deprecated rule key. " +
            "The following deprecated rule keys seem to be referencing a non-existing rule",
          ruleUuid, entry.getValue());
      } else {
        entry.getValue().forEach(d -> rulesByKey.put(d.getOldRuleKeyAsRuleKey(), rule));
      }
    }
    return unmodifiableMap(rulesByKey);
  }

  boolean hasDbRules() {
    return !dbRules.isEmpty();
  }

  Optional<RuleDto> getDbRuleFor(RulesDefinition.Rule ruleDef) {
    RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());
    Optional<RuleDto> res = Stream.concat(Stream.of(ruleKey), ruleDef.deprecatedRuleKeys().stream())
      .map(dbRules::get)
      .filter(Objects::nonNull)
      .findFirst();
    // may occur in case of plugin downgrade
    if (res.isEmpty()) {
      return Optional.ofNullable(dbRulesByDbDeprecatedKey.get(ruleKey));
    }
    return res;
  }

  Map<RuleKey, SingleDeprecatedRuleKey> getDbDeprecatedKeysByOldRuleKey() {
    return dbDeprecatedKeysByUuid.values().stream()
      .flatMap(Collection::stream)
      .collect(Collectors.toMap(SingleDeprecatedRuleKey::getOldRuleKeyAsRuleKey, Function.identity()));
  }

  Set<SingleDeprecatedRuleKey> getDBDeprecatedKeysFor(RuleDto rule) {
    return dbDeprecatedKeysByUuid.getOrDefault(rule.getUuid(), emptySet());
  }

  List<RuleParamDto> getRuleParametersFor(String ruleUuid) {
    return ruleParamsByRuleUuid.getOrDefault(ruleUuid, emptyList());
  }

  Stream<RuleDto> getRemaining() {
    Set<RuleDto> res = new HashSet<>(dbRules.values());
    res.removeAll(unchanged);
    res.removeAll(renamed.keySet());
    res.removeAll(updated);
    res.removeAll(removed);
    return res.stream();
  }

  Stream<RuleDto> getRemoved() {
    return removed.stream();
  }

  public Stream<Map.Entry<RuleDto, RuleKey>> getRenamed() {
    return renamed.entrySet().stream();
  }

  Stream<RuleDto> getAllModified() {
    return Stream.of(
        created.stream(),
        updated.stream(),
        removed.stream(),
        renamed.keySet().stream())
      .flatMap(s -> s);
  }

  boolean isCreated(RuleDto ruleDto) {
    return created.contains(ruleDto);
  }

  boolean isRenamed(RuleDto ruleDto) {
    return renamed.containsKey(ruleDto);
  }

  boolean isUpdated(RuleDto ruleDto) {
    return updated.contains(ruleDto);
  }

  void created(RuleDto ruleDto) {
    checkState(!known.contains(ruleDto), "known RuleDto can't be created");
    created.add(ruleDto);
  }

  void renamed(RuleDto ruleDto) {
    ensureKnown(ruleDto);
    renamed.put(ruleDto, ruleDto.getKey());
  }

  void updated(RuleDto ruleDto) {
    ensureKnown(ruleDto);
    updated.add(ruleDto);
  }

  void removed(RuleDto ruleDto) {
    ensureKnown(ruleDto);
    removed.add(ruleDto);
  }

  void unchanged(RuleDto ruleDto) {
    ensureKnown(ruleDto);
    unchanged.add(ruleDto);
  }

  private void ensureKnown(RuleDto ruleDto) {
    checkState(known.contains(ruleDto), "unknown RuleDto");
  }

  static RulesRegistrationContext create(DbClient dbClient, DbSession dbSession) {
    Map<RuleKey, RuleDto> allRules = dbClient.ruleDao().selectAll(dbSession).stream()
      .collect(Collectors.toMap(RuleDto::getKey, Function.identity()));
    Map<String, Set<SingleDeprecatedRuleKey>> existingDeprecatedKeysById = loadDeprecatedRuleKeys(dbClient, dbSession);
    Map<String, List<RuleParamDto>> ruleParamsByRuleUuid = loadAllRuleParameters(dbClient, dbSession);
    return new RulesRegistrationContext(allRules, existingDeprecatedKeysById, ruleParamsByRuleUuid);
  }

  private static Map<String, List<RuleParamDto>> loadAllRuleParameters(DbClient dbClient, DbSession dbSession) {
    return dbClient.ruleDao().selectAllRuleParams(dbSession).stream()
      .collect(Collectors.groupingBy(RuleParamDto::getRuleUuid));
  }

  private static Map<String, Set<SingleDeprecatedRuleKey>> loadDeprecatedRuleKeys(DbClient dbClient, DbSession dbSession) {
    return dbClient.ruleDao().selectAllDeprecatedRuleKeys(dbSession).stream()
      .map(SingleDeprecatedRuleKey::from)
      .collect(Collectors.groupingBy(SingleDeprecatedRuleKey::getRuleUuid, Collectors.toSet()));
  }
}
