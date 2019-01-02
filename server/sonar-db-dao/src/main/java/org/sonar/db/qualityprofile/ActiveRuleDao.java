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
package org.sonar.db.qualityprofile;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;
import static org.sonar.db.KeyLongValue.toMap;

public class ActiveRuleDao implements Dao {

  private static final String QUALITY_PROFILE_IS_NOT_PERSISTED = "Quality profile is not persisted (missing id)";
  private static final String RULE_IS_NOT_PERSISTED = "Rule is not persisted";
  private static final String RULE_PARAM_IS_NOT_PERSISTED = "Rule param is not persisted";
  private static final String ACTIVE_RULE_IS_NOT_PERSISTED = "ActiveRule is not persisted";
  private static final String ACTIVE_RULE_IS_ALREADY_PERSISTED = "ActiveRule is already persisted";
  private static final String ACTIVE_RULE_PARAM_IS_NOT_PERSISTED = "ActiveRuleParam is not persisted";
  private static final String ACTIVE_RULE_PARAM_IS_ALREADY_PERSISTED = "ActiveRuleParam is already persisted";

  public Optional<ActiveRuleDto> selectByKey(DbSession dbSession, ActiveRuleKey key) {
    return Optional.ofNullable(mapper(dbSession).selectByKey(key.getRuleProfileUuid(), key.getRuleKey().repository(), key.getRuleKey().rule()));
  }

  public List<OrgActiveRuleDto> selectByRuleId(DbSession dbSession, OrganizationDto organization, int ruleId) {
    return mapper(dbSession).selectByRuleId(organization.getUuid(), ruleId);
  }

  public List<ActiveRuleDto> selectByRuleIdOfAllOrganizations(DbSession dbSession, int ruleId) {
    return mapper(dbSession).selectByRuleIdOfAllOrganizations(ruleId);
  }

  public List<OrgActiveRuleDto> selectByRuleIds(DbSession dbSession, OrganizationDto organization, List<Integer> ids) {
    return executeLargeInputs(ids, chunk -> mapper(dbSession).selectByRuleIds(organization.getUuid(), chunk));
  }

  /**
   * Active rule on removed rule are NOT returned
   */
  public List<OrgActiveRuleDto> selectByProfileUuid(DbSession dbSession, String uuid) {
    return mapper(dbSession).selectByProfileUuid(uuid);
  }

  public List<OrgActiveRuleDto> selectByTypeAndProfileUuids(DbSession dbSession, List<Integer> types, List<String> uuids) {
    return executeLargeInputs(uuids, chunk -> mapper(dbSession).selectByTypeAndProfileUuids(types, chunk));
  }

  public List<OrgActiveRuleDto> selectByProfile(DbSession dbSession, QProfileDto profile) {
    return selectByProfileUuid(dbSession, profile.getKee());
  }

  public List<ActiveRuleDto> selectByRuleProfile(DbSession dbSession, RulesProfileDto ruleProfileDto) {
    return mapper(dbSession).selectByRuleProfileUuid(ruleProfileDto.getKee());
  }

  public Collection<ActiveRuleDto> selectByRulesAndRuleProfileUuids(DbSession dbSession, Collection<RuleDefinitionDto> rules, Collection<String> ruleProfileUuids) {
    if (rules.isEmpty() || ruleProfileUuids.isEmpty()) {
      return emptyList();
    }
    List<Integer> ruleIds = rules.stream().map(RuleDefinitionDto::getId).collect(MoreCollectors.toArrayList(rules.size()));
    ActiveRuleMapper mapper = mapper(dbSession);
    return executeLargeInputs(ruleIds, ruleIdsChunk -> executeLargeInputs(ruleProfileUuids, chunk -> mapper.selectByRuleIdsAndRuleProfileUuids(ruleIdsChunk, chunk)));
  }

  public ActiveRuleDto insert(DbSession dbSession, ActiveRuleDto item) {
    checkArgument(item.getProfileId() != null, QUALITY_PROFILE_IS_NOT_PERSISTED);
    checkArgument(item.getRuleId() != null, RULE_IS_NOT_PERSISTED);
    checkArgument(item.getId() == null, ACTIVE_RULE_IS_ALREADY_PERSISTED);
    mapper(dbSession).insert(item);
    return item;
  }

  public ActiveRuleDto update(DbSession dbSession, ActiveRuleDto item) {
    checkArgument(item.getProfileId() != null, QUALITY_PROFILE_IS_NOT_PERSISTED);
    checkArgument(item.getRuleId() != null, ActiveRuleDao.RULE_IS_NOT_PERSISTED);
    checkArgument(item.getId() != null, ACTIVE_RULE_IS_NOT_PERSISTED);
    mapper(dbSession).update(item);
    return item;
  }

  public Optional<ActiveRuleDto> delete(DbSession dbSession, ActiveRuleKey key) {
    Optional<ActiveRuleDto> activeRule = selectByKey(dbSession, key);
    if (activeRule.isPresent()) {
      mapper(dbSession).deleteParameters(activeRule.get().getId());
      mapper(dbSession).delete(activeRule.get().getId());
    }
    return activeRule;
  }

  public void deleteByRuleProfileUuids(DbSession dbSession, Collection<String> rulesProfileUuids) {
    ActiveRuleMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(rulesProfileUuids, mapper::deleteByRuleProfileUuids);
  }

  public void deleteByIds(DbSession dbSession, List<Integer> activeRuleIds) {
    ActiveRuleMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(activeRuleIds, mapper::deleteByIds);
  }

  public void deleteParametersByRuleProfileUuids(DbSession dbSession, Collection<String> rulesProfileUuids) {
    ActiveRuleMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(rulesProfileUuids, mapper::deleteParametersByRuleProfileUuids);
  }

  /**
   * Nested DTO ActiveRuleParams
   */
  public List<ActiveRuleParamDto> selectParamsByActiveRuleId(DbSession dbSession, Integer activeRuleId) {
    return mapper(dbSession).selectParamsByActiveRuleId(activeRuleId);
  }

  public List<ActiveRuleParamDto> selectParamsByActiveRuleIds(final DbSession dbSession, List<Integer> activeRuleIds) {
    return executeLargeInputs(activeRuleIds, mapper(dbSession)::selectParamsByActiveRuleIds);
  }

  public ActiveRuleParamDto insertParam(DbSession dbSession, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    checkArgument(activeRule.getId() != null, ACTIVE_RULE_IS_NOT_PERSISTED);
    checkArgument(activeRuleParam.getId() == null, ACTIVE_RULE_PARAM_IS_ALREADY_PERSISTED);
    Preconditions.checkNotNull(activeRuleParam.getRulesParameterId(), RULE_PARAM_IS_NOT_PERSISTED);

    activeRuleParam.setActiveRuleId(activeRule.getId());
    mapper(dbSession).insertParameter(activeRuleParam);
    return activeRuleParam;
  }

  public void updateParam(DbSession dbSession, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRuleParam.getId(), ACTIVE_RULE_PARAM_IS_NOT_PERSISTED);
    mapper(dbSession).updateParameter(activeRuleParam);
  }

  public void deleteParam(DbSession dbSession, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRuleParam.getId(), ACTIVE_RULE_PARAM_IS_NOT_PERSISTED);
    deleteParamById(dbSession, activeRuleParam.getId());
  }

  public void deleteParamById(DbSession dbSession, int id) {
    mapper(dbSession).deleteParameter(id);
  }

  public void deleteParamsByRuleParamOfAllOrganizations(DbSession dbSession, RuleParamDto param) {
    List<ActiveRuleDto> activeRules = selectByRuleIdOfAllOrganizations(dbSession, param.getRuleId());
    for (ActiveRuleDto activeRule : activeRules) {
      for (ActiveRuleParamDto activeParam : selectParamsByActiveRuleId(dbSession, activeRule.getId())) {
        if (activeParam.getKey().equals(param.getName())) {
          deleteParam(dbSession, activeParam);
        }
      }
    }
  }

  public void deleteParamsByActiveRuleIds(DbSession dbSession, List<Integer> activeRuleIds) {
    ActiveRuleMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(activeRuleIds, mapper::deleteParamsByActiveRuleIds);
  }

  public Map<String, Long> countActiveRulesByQuery(DbSession dbSession, ActiveRuleCountQuery query) {
    return toMap(executeLargeInputs(query.getProfileUuids(),
      partition -> mapper(dbSession).countActiveRulesByQuery(query.getOrganization().getUuid(), partition, query.getRuleStatus(), query.getInheritance())));
  }

  public void scrollAllForIndexing(DbSession dbSession, Consumer<IndexedActiveRuleDto> consumer) {
    mapper(dbSession).scrollAllForIndexing(context -> {
      IndexedActiveRuleDto dto = context.getResultObject();
      consumer.accept(dto);
    });
  }

  public void scrollByIdsForIndexing(DbSession dbSession, Collection<Long> ids, Consumer<IndexedActiveRuleDto> consumer) {
    ActiveRuleMapper mapper = mapper(dbSession);
    executeLargeInputsWithoutOutput(ids,
      pageOfIds -> mapper
        .scrollByIdsForIndexing(pageOfIds, context -> {
          IndexedActiveRuleDto dto = context.getResultObject();
          consumer.accept(dto);
        }));
  }

  public void scrollByRuleProfileForIndexing(DbSession dbSession, String ruleProfileUuid, Consumer<IndexedActiveRuleDto> consumer) {
    mapper(dbSession).scrollByRuleProfileUuidForIndexing(ruleProfileUuid, context -> {
      IndexedActiveRuleDto dto = context.getResultObject();
      consumer.accept(dto);
    });
  }

  private static ActiveRuleMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(ActiveRuleMapper.class);
  }

}
