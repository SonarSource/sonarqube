/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.rule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class RuleDao implements Dao {

  private final UuidFactory uuidFactory;

  public RuleDao(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public Optional<RuleDto> selectByKey(DbSession session, RuleKey key) {
    RuleDto res = mapper(session).selectByKey(key);
    return ofNullable(res);
  }

  public Optional<RuleDefinitionDto> selectDefinitionByKey(DbSession session, RuleKey key) {
    return ofNullable(mapper(session).selectDefinitionByKey(key));
  }

  public Optional<RuleMetadataDto> selectMetadataByKey(DbSession session, RuleKey key) {
    return ofNullable(mapper(session).selectMetadataByKey(key));
  }

  public List<RuleMetadataDto> selectMetadataByKeys(DbSession session, Collection<RuleKey> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(keys, mapper(session)::selectMetadataByKeys);
  }

  public RuleDto selectOrFailByKey(DbSession session, RuleKey key) {
    RuleDto rule = mapper(session).selectByKey(key);
    if (rule == null) {
      throw new RowNotFoundException(String.format("Rule with key '%s' does not exist", key));
    }
    return rule;
  }

  public RuleDefinitionDto selectOrFailDefinitionByKey(DbSession session, RuleKey key) {
    RuleDefinitionDto rule = mapper(session).selectDefinitionByKey(key);
    if (rule == null) {
      throw new RowNotFoundException(String.format("Rule with key '%s' does not exist", key));
    }
    return rule;
  }

  public Optional<RuleDto> selectByUuid(String uuid, DbSession session) {
    RuleDto res = mapper(session).selectByUuid(uuid);
    return ofNullable(res);
  }

  public Optional<RuleDefinitionDto> selectDefinitionByUuid(String uuid, DbSession session) {
    return ofNullable(mapper(session).selectDefinitionByUuid(uuid));
  }

  public List<RuleDto> selectByUuids(DbSession session, List<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(uuids, chunk -> mapper(session).selectByUuids(chunk));
  }

  public List<RuleDefinitionDto> selectDefinitionByUuids(DbSession session, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(uuids, mapper(session)::selectDefinitionByUuids);
  }

  public List<RuleDto> selectByKeys(DbSession session, Collection<RuleKey> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(keys, chunk -> mapper(session).selectByKeys(chunk));
  }

  public List<RuleDefinitionDto> selectDefinitionByKeys(DbSession session, Collection<RuleKey> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(keys, mapper(session)::selectDefinitionByKeys);
  }

  public void selectEnabled(DbSession session, ResultHandler<RuleDefinitionDto> resultHandler) {
    mapper(session).selectEnabled(resultHandler);
  }

  public List<RuleDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public List<RuleDefinitionDto> selectAllDefinitions(DbSession session) {
    return mapper(session).selectAllDefinitions();
  }

  public List<RuleDto> selectByTypeAndLanguages(DbSession session, List<Integer> types, List<String> languages) {
    return executeLargeInputs(languages, chunk -> mapper(session).selectByTypeAndLanguages(types, chunk));
  }

  public List<RuleDto> selectByQuery(DbSession session, RuleQuery ruleQuery) {
    return mapper(session).selectByQuery(ruleQuery);
  }

  public void insert(DbSession session, RuleDefinitionDto dto) {
    checkNotNull(dto.getUuid(), "RuleDefinitionDto has no 'uuid'.");
    mapper(session).insertDefinition(dto);
  }

  public void insert(DbSession session, RuleMetadataDto dto) {
    checkNotNull(dto.getRuleUuid(), "RuleMetadataDto has no 'ruleUuid'.");
    mapper(session).insertMetadata(dto);
  }

  public void update(DbSession session, RuleDefinitionDto dto) {
    mapper(session).updateDefinition(dto);
  }

  public void insertOrUpdate(DbSession session, RuleMetadataDto dto) {
    if (mapper(session).countMetadata(dto) > 0) {
      mapper(session).updateMetadata(dto);
    } else {
      mapper(session).insertMetadata(dto);
    }
  }

  public void scrollIndexingRuleExtensionsByIds(DbSession dbSession, Collection<String> ruleExtensionIds, Consumer<RuleExtensionForIndexingDto> consumer) {
    RuleMapper mapper = mapper(dbSession);

    executeLargeInputsWithoutOutput(ruleExtensionIds,
      pageOfRuleExtensionIds -> mapper
        .selectIndexingRuleExtensionsByIds(pageOfRuleExtensionIds)
        .forEach(consumer));
  }

  public void scrollIndexingRulesByKeys(DbSession dbSession, Collection<String> ruleUuids, Consumer<RuleForIndexingDto> consumer) {
    RuleMapper mapper = mapper(dbSession);

    executeLargeInputsWithoutOutput(ruleUuids,
      pageOfRuleUuids -> mapper
        .selectIndexingRulesByUuids(pageOfRuleUuids)
        .forEach(consumer));
  }

  public void scrollIndexingRules(DbSession dbSession, Consumer<RuleForIndexingDto> consumer) {
    mapper(dbSession).scrollIndexingRules(context -> {
      RuleForIndexingDto dto = context.getResultObject();
      consumer.accept(dto);
    });
  }

  private static RuleMapper mapper(DbSession session) {
    return session.getMapper(RuleMapper.class);
  }

  /**
   * RuleParams
   */

  public List<RuleParamDto> selectRuleParamsByRuleKey(DbSession session, RuleKey key) {
    return mapper(session).selectParamsByRuleKey(key);
  }

  public List<RuleParamDto> selectRuleParamsByRuleKeys(DbSession session, Collection<RuleKey> ruleKeys) {
    return executeLargeInputs(ruleKeys, mapper(session)::selectParamsByRuleKeys);
  }

  public List<RuleParamDto> selectAllRuleParams(DbSession session) {
    return mapper(session).selectAllRuleParams();
  }

  public List<RuleParamDto> selectRuleParamsByRuleUuids(DbSession dbSession, Collection<String> ruleUuids) {
    return executeLargeInputs(ruleUuids, mapper(dbSession)::selectParamsByRuleUuids);
  }

  public void insertRuleParam(DbSession session, RuleDefinitionDto rule, RuleParamDto param) {
    checkNotNull(rule.getUuid(), "Rule uuid must be set");
    param.setRuleUuid(rule.getUuid());

    param.setUuid(uuidFactory.create());
    mapper(session).insertParameter(param);
  }

  public RuleParamDto updateRuleParam(DbSession session, RuleDefinitionDto rule, RuleParamDto param) {
    checkNotNull(rule.getUuid(), "Rule uuid must be set");
    checkNotNull(param.getUuid(), "Rule parameter is not yet persisted must be set");
    param.setRuleUuid(rule.getUuid());
    mapper(session).updateParameter(param);
    return param;
  }

  public void deleteRuleParam(DbSession session, String ruleParameterUuid) {
    mapper(session).deleteParameter(ruleParameterUuid);
  }

  public Set<DeprecatedRuleKeyDto> selectAllDeprecatedRuleKeys(DbSession session) {
    return mapper(session).selectAllDeprecatedRuleKeys();
  }

  public Set<DeprecatedRuleKeyDto> selectDeprecatedRuleKeysByRuleUuids(DbSession session, Collection<String> ruleUuids) {
    return mapper(session).selectDeprecatedRuleKeysByRuleUuids(ruleUuids);
  }

  public void deleteDeprecatedRuleKeys(DbSession dbSession, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return;
    }
    executeLargeUpdates(uuids, mapper(dbSession)::deleteDeprecatedRuleKeys);
  }

  public void insert(DbSession dbSession, DeprecatedRuleKeyDto deprecatedRuleKey) {
    mapper(dbSession).insertDeprecatedRuleKey(deprecatedRuleKey);
  }
}
