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
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Will be removed in the future.
 */
public class DefaultRuleFinder implements RuleFinder {

  private final DbClient dbClient;
  private final RuleDao ruleDao;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public DefaultRuleFinder(DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.ruleDao = dbClient.ruleDao();
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  @CheckForNull
  public org.sonar.api.rules.Rule findById(int ruleId) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<RuleDto> rule = ruleDao.selectById(ruleId, defaultOrganizationProvider.get().getUuid(), dbSession);
      if (rule.isPresent() && rule.get().getStatus() != RuleStatus.REMOVED) {
        return toRule(rule.get(), ruleDao.selectRuleParamsByRuleKey(dbSession, rule.get().getKey()));
      }
      return null;
    }
  }

  @Override
  @CheckForNull
  public org.sonar.api.rules.Rule findByKey(RuleKey key) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String defaultOrganizationUuid = defaultOrganizationProvider.get().getUuid();
      OrganizationDto defaultOrganization = dbClient.organizationDao().selectByUuid(dbSession, defaultOrganizationUuid)
        .orElseThrow(() -> new IllegalStateException(String.format("Cannot find default organization '%s'", defaultOrganizationUuid)));
      Optional<RuleDto> rule = ruleDao.selectByKey(dbSession, defaultOrganization, key);
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
      List<RuleDto> rules = ruleDao.selectByQuery(dbSession, defaultOrganizationProvider.get().getUuid(), query);
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
      List<RuleDto> rules = ruleDao.selectByQuery(dbSession, defaultOrganizationProvider.get().getUuid(), query);
      if (rules.isEmpty()) {
        return Collections.emptyList();
      }
      return convertToRuleApi(dbSession, rules);
    }
  }

  private Collection<org.sonar.api.rules.Rule> convertToRuleApi(DbSession dbSession, List<RuleDto> ruleDtos) {
    List<org.sonar.api.rules.Rule> rules = new ArrayList<>();
    List<RuleKey> ruleKeys = FluentIterable.from(ruleDtos).transform(RuleDtoToKey.INSTANCE).toList();
    List<RuleParamDto> ruleParamDtos = ruleDao.selectRuleParamsByRuleKeys(dbSession, ruleKeys);
    ImmutableListMultimap<Integer, RuleParamDto> ruleParamByRuleId = FluentIterable.from(ruleParamDtos).index(RuleParamDtoToRuleId.INSTANCE);
    for (RuleDto rule : ruleDtos) {
      rules.add(toRule(rule, ruleParamByRuleId.get(rule.getId())));
    }
    return rules;
  }

  private static org.sonar.api.rules.Rule toRule(RuleDto rule, List<RuleParamDto> params) {
    String severity = rule.getSeverityString();
    String description = rule.getDescription();
    RuleDto.Format descriptionFormat = rule.getDescriptionFormat();

    org.sonar.api.rules.Rule apiRule = new org.sonar.api.rules.Rule();
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
      .setTags(rule.getTags().toArray(new String[rule.getTags().size()]))
      .setId(rule.getId());
    if (description != null && descriptionFormat != null) {
      if (RuleDto.Format.HTML.equals(descriptionFormat)) {
        apiRule.setDescription(description);
      } else {
        apiRule.setDescription(Markdown.convertToHtml(description));
      }
    }

    List<org.sonar.api.rules.RuleParam> apiParams = newArrayList();
    for (RuleParamDto param : params) {
      apiParams.add(new org.sonar.api.rules.RuleParam(apiRule, param.getName(), param.getDescription(), param.getType())
        .setDefaultValue(param.getDefaultValue()));
    }
    apiRule.setParams(apiParams);

    return apiRule;
  }

  private enum RuleDtoToKey implements Function<RuleDto, RuleKey> {
    INSTANCE;

    @Override
    public RuleKey apply(@Nonnull RuleDto input) {
      return input.getKey();
    }
  }

  private enum RuleParamDtoToRuleId implements Function<RuleParamDto, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull RuleParamDto input) {
      return input.getRuleId();
    }
  }

}
