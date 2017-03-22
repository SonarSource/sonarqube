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
package org.sonar.db.rule;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class RuleDao implements Dao {

  public Optional<RuleDto> selectByKey(DbSession session, String organizationUuid, RuleKey key) {
    return Optional.fromNullable(mapper(session).selectByKey(key));
  }

  public Optional<RuleDefinitionDto> selectDefinitionByKey(DbSession session, RuleKey key) {
    return Optional.fromNullable(mapper(session).selectDefinitionByKey(key));
  }

  public RuleDto selectOrFailByKey(DbSession session, String organizationUuid, RuleKey key) {
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

  public Optional<RuleDto> selectById(long id, String organizationUuid, DbSession session) {
    return Optional.fromNullable(mapper(session).selectById(id));
  }

  public Optional<RuleDefinitionDto> selectDefinitionById(long id, DbSession session) {
    return Optional.fromNullable(mapper(session).selectDefinitionById(id));
  }

  public List<RuleDto> selectByIds(DbSession session, String organizationUuid, List<Integer> ids) {
    return executeLargeInputs(ids, mapper(session)::selectByIds);
  }

  public List<RuleDefinitionDto> selectDefinitionByIds(DbSession session, List<Integer> ids) {
    return executeLargeInputs(ids, mapper(session)::selectDefinitionByIds);
  }

  public List<RuleDto> selectByKeys(DbSession session, String organizationUuid, Collection<RuleKey> keys) {
    return executeLargeInputs(keys, mapper(session)::selectByKeys);
  }

  public List<RuleDefinitionDto> selectDefinitionByKeys(DbSession session, Collection<RuleKey> keys) {
    return executeLargeInputs(keys, mapper(session)::selectDefinitionByKeys);
  }

  public void selectEnabled(DbSession session, ResultHandler resultHandler) {
    mapper(session).selectEnabled(resultHandler);
  }

  public List<RuleDto> selectAll(DbSession session, String organizationUuid) {
    return mapper(session).selectAll();
  }

  public List<RuleDefinitionDto> selectAllDefinitions(DbSession session) {
    return mapper(session).selectAllDefinitions();
  }

  public List<RuleDto> selectByQuery(DbSession session, RuleQuery ruleQuery) {
    return mapper(session).selectByQuery(ruleQuery);
  }

  public void insert(DbSession session, RuleDefinitionDto dto) {
    mapper(session).insert(dto);
  }

  public void update(DbSession session, RuleDefinitionDto dto) {
    mapper(session).updateDefinition(dto);
  }

  public void update(DbSession session, RuleMetadataDto dto) {
    mapper(session).updateMetadata(dto);
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

  public List<RuleParamDto> selectRuleParamsByRuleKeys(DbSession session, List<RuleKey> ruleKeys) {
    return executeLargeInputs(ruleKeys, mapper(session)::selectParamsByRuleKeys);
  }

  public List<RuleParamDto> selectRuleParamsByRuleIds(DbSession dbSession, List<Integer> ruleIds) {
    return executeLargeInputs(ruleIds, mapper(dbSession)::selectParamsByRuleIds);
  }

  public void insertRuleParam(DbSession session, RuleDefinitionDto rule, RuleParamDto param) {
    checkNotNull(rule.getId(), "Rule id must be set");
    param.setRuleId(rule.getId());
    mapper(session).insertParameter(param);
  }

  public RuleParamDto updateRuleParam(DbSession session, RuleDefinitionDto rule, RuleParamDto param) {
    checkNotNull(rule.getId(), "Rule id must be set");
    checkNotNull(param.getId(), "Rule parameter is not yet persisted must be set");
    param.setRuleId(rule.getId());
    mapper(session).updateParameter(param);
    return param;
  }

  public void deleteRuleParam(DbSession session, int ruleParameterId) {
    mapper(session).deleteParameter(ruleParameterId);
  }

}
