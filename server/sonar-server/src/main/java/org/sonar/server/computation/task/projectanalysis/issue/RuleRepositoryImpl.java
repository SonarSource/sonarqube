/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class RuleRepositoryImpl implements RuleRepository {

  @CheckForNull
  private Map<RuleKey, Rule> rulesByKey;
  @CheckForNull
  private Map<Integer, Rule> rulesById;

  private final DbClient dbClient;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public RuleRepositoryImpl(DbClient dbClient, AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public Rule getByKey(RuleKey key) {
    verifyKeyArgument(key);

    ensureInitialized();

    Rule rule = rulesByKey.get(key);
    checkArgument(rule != null, "Can not find rule for key %s. This rule does not exist in DB", key);
    return rule;
  }

  @Override
  public Optional<Rule> findByKey(RuleKey key) {
    verifyKeyArgument(key);

    ensureInitialized();

    return Optional.ofNullable(rulesByKey.get(key));
  }

  @Override
  public Rule getById(int id) {
    ensureInitialized();

    Rule rule = rulesById.get(id);
    checkArgument(rule != null, "Can not find rule for id %s. This rule does not exist in DB", id);
    return rule;
  }

  @Override
  public Optional<Rule> findById(int id) {
    ensureInitialized();

    return Optional.ofNullable(rulesById.get(id));
  }

  private static void verifyKeyArgument(RuleKey key) {
    requireNonNull(key, "RuleKey can not be null");
  }

  private void ensureInitialized() {
    if (rulesByKey == null) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        loadRulesFromDb(dbSession);
      }
    }
  }

  private void loadRulesFromDb(DbSession dbSession) {
    ImmutableMap.Builder<RuleKey, Rule> rulesByKeyBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<Integer, Rule> rulesByIdBuilder = ImmutableMap.builder();
    String organizationUuid = analysisMetadataHolder.getOrganization().getUuid();
    Multimap<Integer, DeprecatedRuleKeyDto> deprecatedRuleKeysByRuleId = dbClient.ruleDao().selectAllDeprecatedRuleKeys(dbSession).stream()
      .collect(MoreCollectors.index(DeprecatedRuleKeyDto::getRuleId));
    for (RuleDto ruleDto : dbClient.ruleDao().selectAll(dbSession, organizationUuid)) {
      Rule rule = new RuleImpl(ruleDto);
      rulesByKeyBuilder.put(ruleDto.getKey(), rule);
      rulesByIdBuilder.put(ruleDto.getId(), rule);
      deprecatedRuleKeysByRuleId.get(ruleDto.getId()).forEach(t -> rulesByKeyBuilder.put(RuleKey.of(t.getOldRepositoryKey(), t.getOldRuleKey()), rule));
    }
    this.rulesByKey = rulesByKeyBuilder.build();
    this.rulesById = rulesByIdBuilder.build();
  }

}
