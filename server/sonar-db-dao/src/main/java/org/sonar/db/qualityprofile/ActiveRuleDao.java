/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
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

  private final UuidFactory uuidFactory;

  public ActiveRuleDao(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public Optional<ActiveRuleDto> selectByKey(DbSession dbSession, ActiveRuleKey key) {
    return Optional.ofNullable(mapper(dbSession).selectByKey(key.getRuleProfileUuid(), key.getRuleKey().repository(), key.getRuleKey().rule()));
  }

  public List<OrgActiveRuleDto> selectByOrgRuleUuid(DbSession dbSession, String ruleUuid) {
    return mapper(dbSession).selectOrgByRuleUuid(ruleUuid);
  }

  public List<ActiveRuleDto> selectByRuleUuid(DbSession dbSession, String ruleUuid) {
    return mapper(dbSession).selectByRuleUuid(ruleUuid);
  }

  public List<OrgActiveRuleDto> selectByRuleUuids(DbSession dbSession, List<String> uuids) {
    return executeLargeInputs(uuids, chunk -> mapper(dbSession).selectByRuleUuids(chunk));
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
    return mapper(dbSession).selectByRuleProfileUuid(ruleProfileDto.getUuid());
  }

  public Collection<ActiveRuleDto> selectByRulesAndRuleProfileUuids(DbSession dbSession, Collection<String> ruleUuids, Collection<String> ruleProfileUuids) {
    if (ruleUuids.isEmpty() || ruleProfileUuids.isEmpty()) {
      return emptyList();
    }
    ActiveRuleMapper mapper = mapper(dbSession);
    return executeLargeInputs(ruleUuids,
      ruleUuidsChunk -> executeLargeInputs(ruleProfileUuids, chunk -> mapper.selectByRuleUuidsAndRuleProfileUuids(ruleUuidsChunk, chunk)));
  }

  public ActiveRuleDto insert(DbSession dbSession, ActiveRuleDto item) {
    checkArgument(item.getProfileUuid() != null, QUALITY_PROFILE_IS_NOT_PERSISTED);
    checkArgument(item.getRuleUuid() != null, RULE_IS_NOT_PERSISTED);
    checkArgument(item.getUuid() == null, ACTIVE_RULE_IS_ALREADY_PERSISTED);

    item.setUuid(uuidFactory.create());
    mapper(dbSession).insert(item);
    return item;
  }

  public ActiveRuleDto update(DbSession dbSession, ActiveRuleDto item) {
    checkArgument(item.getProfileUuid() != null, QUALITY_PROFILE_IS_NOT_PERSISTED);
    checkArgument(item.getRuleUuid() != null, ActiveRuleDao.RULE_IS_NOT_PERSISTED);
    checkArgument(item.getUuid() != null, ACTIVE_RULE_IS_NOT_PERSISTED);
    mapper(dbSession).update(item);
    return item;
  }

  public Optional<ActiveRuleDto> delete(DbSession dbSession, ActiveRuleKey key) {
    Optional<ActiveRuleDto> activeRule = selectByKey(dbSession, key);
    if (activeRule.isPresent()) {
      mapper(dbSession).deleteParameters(activeRule.get().getUuid());
      mapper(dbSession).delete(activeRule.get().getUuid());
    }
    return activeRule;
  }

  public void deleteByRuleProfileUuids(DbSession dbSession, Collection<String> rulesProfileUuids) {
    ActiveRuleMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(rulesProfileUuids, mapper::deleteByRuleProfileUuids);
  }

  public void deleteByUuids(DbSession dbSession, List<String> activeRuleUuids) {
    ActiveRuleMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(activeRuleUuids, mapper::deleteByUuids);
  }

  public void deleteParametersByRuleProfileUuids(DbSession dbSession, Collection<String> rulesProfileUuids) {
    ActiveRuleMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(rulesProfileUuids, mapper::deleteParametersByRuleProfileUuids);
  }

  /**
   * Nested DTO ActiveRuleParams
   */
  public List<ActiveRuleParamDto> selectParamsByActiveRuleUuid(DbSession dbSession, String activeRuleUuid) {
    return mapper(dbSession).selectParamsByActiveRuleUuid(activeRuleUuid);
  }

  public List<ActiveRuleParamDto> selectParamsByActiveRuleUuids(final DbSession dbSession, List<String> activeRuleUuids) {
    return executeLargeInputs(activeRuleUuids, mapper(dbSession)::selectParamsByActiveRuleUuids);
  }

  public ActiveRuleParamDto insertParam(DbSession dbSession, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    checkArgument(activeRule.getUuid() != null, ACTIVE_RULE_IS_NOT_PERSISTED);
    checkArgument(activeRuleParam.getUuid() == null, ACTIVE_RULE_PARAM_IS_ALREADY_PERSISTED);
    Preconditions.checkNotNull(activeRuleParam.getRulesParameterUuid(), RULE_PARAM_IS_NOT_PERSISTED);

    activeRuleParam.setActiveRuleUuid(activeRule.getUuid());
    activeRuleParam.setUuid(uuidFactory.create());
    mapper(dbSession).insertParameter(activeRuleParam);
    return activeRuleParam;
  }

  public void updateParam(DbSession dbSession, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRuleParam.getUuid(), ACTIVE_RULE_PARAM_IS_NOT_PERSISTED);
    mapper(dbSession).updateParameter(activeRuleParam);
  }

  public void deleteParam(DbSession dbSession, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRuleParam.getUuid(), ACTIVE_RULE_PARAM_IS_NOT_PERSISTED);
    deleteParamByUuid(dbSession, activeRuleParam.getUuid());
  }

  public void deleteParamByUuid(DbSession dbSession, String uuid) {
    mapper(dbSession).deleteParameter(uuid);
  }

  public void deleteParamsByRuleParam(DbSession dbSession, RuleParamDto param) {
    List<ActiveRuleDto> activeRules = selectByRuleUuid(dbSession, param.getRuleUuid());
    for (ActiveRuleDto activeRule : activeRules) {
      for (ActiveRuleParamDto activeParam : selectParamsByActiveRuleUuid(dbSession, activeRule.getUuid())) {
        if (activeParam.getKey().equals(param.getName())) {
          deleteParam(dbSession, activeParam);
        }
      }
    }
  }

  public void deleteParamsByActiveRuleUuids(DbSession dbSession, List<String> activeRuleUuids) {
    ActiveRuleMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(activeRuleUuids, mapper::deleteParamsByActiveRuleUuids);
  }

  public Map<String, Long> countActiveRulesByQuery(DbSession dbSession, ActiveRuleCountQuery query) {
    return toMap(executeLargeInputs(query.getProfileUuids(),
      partition -> mapper(dbSession).countActiveRulesByQuery(partition, query.getRuleStatus(), query.getInheritance())));
  }

  public void scrollAllForIndexing(DbSession dbSession, Consumer<IndexedActiveRuleDto> consumer) {
    mapper(dbSession).scrollAllForIndexing(context -> {
      IndexedActiveRuleDto dto = context.getResultObject();
      consumer.accept(dto);
    });
  }

  public void scrollByUuidsForIndexing(DbSession dbSession, Collection<String> uuids, Consumer<IndexedActiveRuleDto> consumer) {
    ActiveRuleMapper mapper = mapper(dbSession);
    executeLargeInputsWithoutOutput(uuids,
      pageOfIds -> mapper
        .scrollByUuidsForIndexing(pageOfIds, context -> {
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

  public int countMissingRules(DbSession dbSession, String rulesProfileUuid, String compareToRulesProfileUuid) {
    return mapper(dbSession).countMissingRules(rulesProfileUuid, compareToRulesProfileUuid);
  }

  private static ActiveRuleMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(ActiveRuleMapper.class);
  }

}
