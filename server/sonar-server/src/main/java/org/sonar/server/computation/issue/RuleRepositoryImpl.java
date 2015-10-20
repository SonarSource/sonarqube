/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.issue;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class RuleRepositoryImpl implements RuleRepository {

  @CheckForNull
  private Map<RuleKey, Rule> rulesByKey;
  @CheckForNull
  private Map<Integer, Rule> rulesById;

  private final DbClient dbClient;

  public RuleRepositoryImpl(DbClient dbClient) {
    this.dbClient = dbClient;
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

    return Optional.fromNullable(rulesByKey.get(key));
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

    return Optional.fromNullable(rulesById.get(id));
  }

  private static void verifyKeyArgument(RuleKey key) {
    requireNonNull(key, "RuleKey can not be null");
  }

  private void ensureInitialized() {
    if (rulesByKey == null) {
      DbSession dbSession = dbClient.openSession(false);
      try {
        loadRulesFromDb(dbSession);
      } finally {
        dbClient.closeSession(dbSession);
      }
    }
  }

  private void loadRulesFromDb(DbSession dbSession) {
    ImmutableMap.Builder<RuleKey, Rule> rulesByKeyBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<Integer, Rule> rulesByIdBuilder = ImmutableMap.builder();
    for (RuleDto ruleDto : dbClient.ruleDao().selectAll(dbSession)) {
      Rule rule = new RuleImpl(ruleDto);
      rulesByKeyBuilder.put(ruleDto.getKey(), rule);
      rulesByIdBuilder.put(ruleDto.getId(), rule);
    }
    this.rulesByKey = rulesByKeyBuilder.build();
    this.rulesById = rulesByIdBuilder.build();
  }

}
