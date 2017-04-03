/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.picocontainer.Startable;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.rule.RuleDefinitionDto;

public class CachingRuleActivatorContextFactory extends RuleActivatorContextFactory implements Startable {
  private final DbClient dbClient;
  private final Map<RuleKey, RuleDefinitionDto> rulesByRuleKey = new HashMap<>();
  private final Cache<String, Map<RuleKey, ActiveRuleDto>> childrenByParentKey = CacheBuilder.newBuilder()
    .maximumSize(10)
    .build();

  public CachingRuleActivatorContextFactory(DbClient db) {
    super(db);
    this.dbClient = db;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.ruleDao().selectAllDefinitions(dbSession).forEach(rule -> rulesByRuleKey.put(rule.getKey(), rule));
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @Override
  Optional<RuleDefinitionDto> getRule(DbSession dbSession, RuleKey ruleKey) {
    return Optional.ofNullable(rulesByRuleKey.get(ruleKey));
  }

  @Override
  Optional<ActiveRuleDto> getActiveRule(DbSession session, ActiveRuleKey key) {
    try {
      String profileKey = key.qProfile();
      Map<RuleKey, ActiveRuleDto> profileActiveRulesByRuleKey = childrenByParentKey.get(
        profileKey,
        () -> loadActiveRulesOfQualityProfile(session, profileKey));
      return Optional.ofNullable(profileActiveRulesByRuleKey.get(key.ruleKey()));
    } catch (ExecutionException e) {
      throw new IllegalStateException(e.getCause());
    }
  }

  private Map<RuleKey, ActiveRuleDto> loadActiveRulesOfQualityProfile(DbSession session, String profileKey) {
    return dbClient.activeRuleDao().selectByProfileKey(session, profileKey).stream()
      .collect(MoreCollectors.uniqueIndex(dto -> dto.getKey().ruleKey()));
  }
}
