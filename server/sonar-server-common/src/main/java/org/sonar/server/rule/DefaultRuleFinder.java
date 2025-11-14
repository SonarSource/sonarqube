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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RulePriority;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;

/**
 * Will be removed in the future.
 */
public class DefaultRuleFinder implements ServerRuleFinder {

  private final DbClient dbClient;
  private final RuleDao ruleDao;
  private final RuleDescriptionFormatter ruleDescriptionFormatter;

  public DefaultRuleFinder(DbClient dbClient, RuleDescriptionFormatter ruleDescriptionFormatter) {
    this.dbClient = dbClient;
    this.ruleDao = dbClient.ruleDao();
    this.ruleDescriptionFormatter = ruleDescriptionFormatter;
  }

  @Override
  public Optional<RuleDto> findDtoByKey(RuleKey key) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return ruleDao.selectByKey(dbSession, key)
        .filter(r -> r.getStatus() != RuleStatus.REMOVED);
    }
  }

  @Override
  public Optional<RuleDto> findDtoByUuid(String uuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return ruleDao.selectByUuid(uuid, dbSession)
        .filter(r -> r.getStatus() != RuleStatus.REMOVED);
    }
  }

  @Override
  public Collection<RuleDto> findAll() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return ruleDao.selectEnabled(dbSession);
    }
  }

  @Override
  @CheckForNull
  public org.sonar.api.rules.Rule findByKey(RuleKey key) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<RuleDto> rule = ruleDao.selectByKey(dbSession, key);
      if (rule.isPresent() && rule.get().getStatus() != RuleStatus.REMOVED) {
        return toRule(rule.get(), ruleDao.selectRuleParamsByRuleKey(dbSession, rule.get().getKey()));
      } else {
        return null;
      }
    }
  }

  @Override
  @CheckForNull
  public org.sonar.api.rules.Rule findByKey(String repositoryKey, String key) {
    return findByKey(RuleKey.of(repositoryKey, key));
  }

  @Override
  public final org.sonar.api.rules.Rule find(org.sonar.api.rules.RuleQuery query) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<RuleDto> rules = ruleDao.selectByQuery(dbSession, query);
      if (!rules.isEmpty()) {
        RuleDto rule = rules.get(0);
        return toRule(rule, ruleDao.selectRuleParamsByRuleKey(dbSession, rule.getKey()));
      }
      return null;
    }
  }

  @Override
  public final Collection<org.sonar.api.rules.Rule> findAll(org.sonar.api.rules.RuleQuery query) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<RuleDto> rules = ruleDao.selectByQuery(dbSession, query);
      if (rules.isEmpty()) {
        return Collections.emptyList();
      }
      return convertToRuleApi(dbSession, rules);
    }
  }

  private Collection<org.sonar.api.rules.Rule> convertToRuleApi(DbSession dbSession, List<RuleDto> ruleDtos) {
    List<org.sonar.api.rules.Rule> rules = new ArrayList<>();
    List<RuleKey> ruleKeys = ruleDtos.stream().map(RuleDto::getKey).toList();
    List<RuleParamDto> ruleParamDtos = ruleDao.selectRuleParamsByRuleKeys(dbSession, ruleKeys);
    ImmutableListMultimap<String, RuleParamDto> ruleParamByRuleUuid = FluentIterable.from(ruleParamDtos).index(RuleParamDtoToRuleUuid.INSTANCE);
    for (RuleDto rule : ruleDtos) {
      rules.add(toRule(rule, ruleParamByRuleUuid.get(rule.getUuid())));
    }
    return rules;
  }

  private org.sonar.api.rules.Rule toRule(RuleDto rule, List<RuleParamDto> params) {
    String severity = rule.getSeverityString();

    org.sonar.api.rules.Rule apiRule = org.sonar.api.rules.Rule.create();
    apiRule
      .setName(rule.getName())
      .setLanguage(rule.getLanguage())
      .setKey(rule.getRuleKey())
      .setConfigKey(rule.getConfigKey())
      .setIsTemplate(rule.isTemplate())
      .setCreatedAt(new Date(rule.getCreatedAt()))
      .setUpdatedAt(new Date(rule.getUpdatedAt()))
      .setRepositoryKey(rule.getRepositoryKey())
      .setSeverity(severity != null ? RulePriority.valueOf(severity) : null)
      .setStatus(rule.getStatus().name())
      .setSystemTags(rule.getSystemTags().toArray(new String[rule.getSystemTags().size()]))
      .setTags(rule.getTags().toArray(new String[rule.getTags().size()]));

    Optional.ofNullable(ruleDescriptionFormatter.getDescriptionAsHtml(rule)).ifPresent(apiRule::setDescription);

    for (RuleParamDto param : params) {
      apiRule.createParameter()
        .setType(param.getType())
        .setDescription(param.getDescription())
        .setKey(param.getName())
        .setDefaultValue(param.getDefaultValue());
    }

    return apiRule;
  }

  private enum RuleParamDtoToRuleUuid implements Function<RuleParamDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull RuleParamDto input) {
      return input.getRuleUuid();
    }
  }

}
