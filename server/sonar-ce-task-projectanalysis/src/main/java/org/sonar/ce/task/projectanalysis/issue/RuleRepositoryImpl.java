/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class RuleRepositoryImpl implements RuleRepository {

  @CheckForNull
  private Map<RuleKey, Rule> rulesByKey;
  @CheckForNull
  private Map<Integer, Rule> rulesById;

  private final AdHocRuleCreator creator;
  private final DbClient dbClient;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  private Map<RuleKey, NewAdHocRule> adHocRulesPersist = new HashMap<>();

  public RuleRepositoryImpl(AdHocRuleCreator creator, DbClient dbClient, AnalysisMetadataHolder analysisMetadataHolder) {
    this.creator = creator;
    this.dbClient = dbClient;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  public void addOrUpdateAddHocRuleIfNeeded(RuleKey ruleKey, Supplier<NewAdHocRule> ruleSupplier) {
    ensureInitialized();

    Rule existingRule = rulesByKey.get(ruleKey);
    if (existingRule == null || (existingRule.isAdHoc() && !adHocRulesPersist.containsKey(ruleKey))) {
      NewAdHocRule newAdHocRule = ruleSupplier.get();
      adHocRulesPersist.put(ruleKey, newAdHocRule);
      rulesByKey.put(ruleKey, new AdHocRuleWrapper(newAdHocRule));
    }
  }

  @Override
  public void saveOrUpdateAddHocRules(DbSession dbSession) {
    ensureInitialized();

    adHocRulesPersist.values().forEach(r -> persistAndIndex(dbSession, r));
  }

  private void persistAndIndex(DbSession dbSession, NewAdHocRule adHocRule) {
    Rule rule = new RuleImpl(creator.persistAndIndex(dbSession, adHocRule, analysisMetadataHolder.getOrganization().toDto()));
    rulesById.put(rule.getId(), rule);
    rulesByKey.put(adHocRule.getKey(), rule);
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
    this.rulesByKey = new HashMap<>();
    this.rulesById = new HashMap<>();
    String organizationUuid = analysisMetadataHolder.getOrganization().getUuid();
    Multimap<Integer, DeprecatedRuleKeyDto> deprecatedRuleKeysByRuleId = dbClient.ruleDao().selectAllDeprecatedRuleKeys(dbSession).stream()
      .collect(MoreCollectors.index(DeprecatedRuleKeyDto::getRuleId));
    for (RuleDto ruleDto : dbClient.ruleDao().selectAll(dbSession, organizationUuid)) {
      Rule rule = new RuleImpl(ruleDto);
      rulesByKey.put(ruleDto.getKey(), rule);
      rulesById.put(ruleDto.getId(), rule);
      deprecatedRuleKeysByRuleId.get(ruleDto.getId()).forEach(t -> rulesByKey.put(RuleKey.of(t.getOldRepositoryKey(), t.getOldRuleKey()), rule));
    }
  }

  private static class AdHocRuleWrapper implements Rule {
    private final NewAdHocRule addHocRule;

    private AdHocRuleWrapper(NewAdHocRule addHocRule) {
      this.addHocRule = addHocRule;
    }

    public NewAdHocRule getDelegate() {
      return addHocRule;
    }

    @Override
    public int getId() {
      throw new UnsupportedOperationException("Rule is not persisted, can't know the id");
    }

    @Override
    public RuleKey getKey() {
      return addHocRule.getKey();
    }

    @Override
    public String getName() {
      return addHocRule.getName();
    }

    @Override
    @CheckForNull
    public String getLanguage() {
      return null;
    }

    @Override
    public RuleStatus getStatus() {
      return RuleStatus.defaultStatus();
    }

    @Override
    @CheckForNull
    public RuleType getType() {
      return null;
    }

    @Override
    public boolean isExternal() {
      return true;
    }

    @Override
    public boolean isAdHoc() {
      return true;
    }

    @Override
    public Set<String> getTags() {
      return Collections.emptySet();
    }

    @CheckForNull
    @Override
    public DebtRemediationFunction getRemediationFunction() {
      return null;
    }

    @CheckForNull
    @Override
    public String getPluginKey() {
      return null;
    }
  }
}
