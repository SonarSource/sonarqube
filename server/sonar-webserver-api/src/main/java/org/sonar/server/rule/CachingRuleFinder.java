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
package org.sonar.server.rule;

import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;

/**
 * A {@link RuleFinder} implementation that retrieves all rule definitions and their parameter when instantiated, cache
 * them in memory and provide implementation of {@link RuleFinder}'s method which only read from this data in memory.
 */
public class CachingRuleFinder implements ServerRuleFinder {

  private static final Ordering<Map.Entry<RuleDto, Rule>> FIND_BY_QUERY_ORDER = Ordering.natural().reverse().onResultOf(entry -> entry.getKey().getUpdatedAt());

  private final Map<RuleKey, RuleDto> ruleDtosByKey;
  private final Map<String, RuleDto> ruleDtosByUuid;
  private final Map<RuleDto, Rule> ruleByRuleDto;
  private final Map<RuleKey, Rule> rulesByKey;
  private final RuleDescriptionFormatter ruleDescriptionFormatter;

  public CachingRuleFinder(DbClient dbClient, RuleDescriptionFormatter ruleDescriptionFormatter) {
    this.ruleDescriptionFormatter = ruleDescriptionFormatter;
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<RuleDto> dtos = dbClient.ruleDao().selectAll(dbSession);
      this.ruleDtosByKey = dtos.stream().collect(Collectors.toMap(RuleDto::getKey, d -> d));
      this.ruleDtosByUuid = dtos.stream().collect(Collectors.toMap(RuleDto::getUuid, d -> d));
      this.ruleByRuleDto = buildRulesByRuleDefinitionDto(dbClient, dbSession, dtos);
      this.rulesByKey = this.ruleByRuleDto.entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getKey().getKey(), Map.Entry::getValue));
    }
  }

  private Map<RuleDto, Rule> buildRulesByRuleDefinitionDto(DbClient dbClient, DbSession dbSession, List<RuleDto> dtos) {
    Map<String, List<RuleParamDto>> ruleParamsByRuleUuid = retrieveRuleParameters(dbClient, dbSession);
    Map<RuleDto, Rule> rulesByDefinition = new HashMap<>(dtos.size());
    for (RuleDto definition : dtos) {
      rulesByDefinition.put(definition, toRule(definition, ruleParamsByRuleUuid.getOrDefault(definition.getUuid(), emptyList())));
    }
    return unmodifiableMap(rulesByDefinition);
  }

  private static Map<String, List<RuleParamDto>> retrieveRuleParameters(DbClient dbClient, DbSession dbSession) {
    return dbClient.ruleDao().selectAllRuleParams(dbSession).stream()
      .collect(Collectors.groupingBy(RuleParamDto::getRuleUuid));
  }

  @Override
  @CheckForNull
  public Rule findByKey(@Nullable String repositoryKey, @Nullable String key) {
    if (repositoryKey == null || key == null) {
      return null;
    }
    return findByKey(RuleKey.of(repositoryKey, key));
  }

  @Override
  @CheckForNull
  public Rule findByKey(@Nullable RuleKey key) {
    return rulesByKey.get(key);
  }

  @Override
  @CheckForNull
  public Rule find(@Nullable RuleQuery query) {
    if (query == null) {
      return null;
    }

    return ruleByRuleDto.entrySet().stream()
      .filter(entry -> matchQuery(entry.getKey(), query))
      .sorted(FIND_BY_QUERY_ORDER)
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);
  }

  @Override
  public Collection<Rule> findAll(@Nullable RuleQuery query) {
    if (query == null) {
      return Collections.emptyList();
    }
    return ruleByRuleDto.entrySet().stream()
      .filter(entry -> matchQuery(entry.getKey(), query))
      .sorted(FIND_BY_QUERY_ORDER)
      .map(Map.Entry::getValue)
      .toList();
  }

  private static boolean matchQuery(RuleDto ruleDto, RuleQuery ruleQuery) {
    if (RuleStatus.REMOVED == ruleDto.getStatus()) {
      return false;
    }
    String repositoryKey = ruleQuery.getRepositoryKey();
    if (ruleQuery.getRepositoryKey() != null && !repositoryKey.equals(ruleDto.getRepositoryKey())) {
      return false;
    }
    String key = ruleQuery.getKey();
    if (key != null && !key.equals(ruleDto.getRuleKey())) {
      return false;
    }
    String configKey = ruleQuery.getConfigKey();
    return configKey == null || configKey.equals(ruleDto.getConfigKey());
  }

  private Rule toRule(RuleDto ruleDto, List<RuleParamDto> params) {
    String severity = ruleDto.getSeverityString();

    Rule apiRule = Rule.create()
      .setName(ruleDto.getName())
      .setKey(ruleDto.getRuleKey())
      .setConfigKey(ruleDto.getConfigKey())
      .setIsTemplate(ruleDto.isTemplate())
      .setCreatedAt(new Date(ruleDto.getCreatedAt()))
      .setUpdatedAt(new Date(ruleDto.getUpdatedAt()))
      .setRepositoryKey(ruleDto.getRepositoryKey())
      .setSeverity(severity != null ? RulePriority.valueOf(severity) : null)
      .setStatus(ruleDto.getStatus().name())
      .setSystemTags(ruleDto.getSystemTags().toArray(new String[ruleDto.getSystemTags().size()]))
      .setTags(new String[0]);

    Optional.ofNullable(ruleDescriptionFormatter.getDescriptionAsHtml(ruleDto)).ifPresent(apiRule::setDescription);
    Optional.ofNullable(ruleDto.getLanguage()).ifPresent(apiRule::setLanguage);

    for (RuleParamDto param : params) {
      apiRule.createParameter()
        .setDescription(param.getDescription())
        .setKey(param.getName())
        .setType(param.getType())
        .setDefaultValue(param.getDefaultValue());
    }

    return apiRule;
  }

  @Override
  public Optional<RuleDto> findDtoByKey(RuleKey key) {
    return Optional.ofNullable(this.ruleDtosByKey.get(key));
  }

  @Override
  public Optional<RuleDto> findDtoByUuid(String uuid) {
    return Optional.ofNullable(this.ruleDtosByUuid.get(uuid));
  }

  @Override
  public Collection<RuleDto> findAll() {
    return ruleDtosByUuid.values();
  }

}
