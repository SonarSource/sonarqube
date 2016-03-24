/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class RuleDao implements Dao {

  public Optional<RuleDto> selectByKey(DbSession session, RuleKey key) {
    return Optional.fromNullable(mapper(session).selectByKey(key));
  }

  public RuleDto selectOrFailByKey(DbSession session, RuleKey key) {
    RuleDto rule = mapper(session).selectByKey(key);
    if (rule == null) {
      throw new RowNotFoundException(String.format("Rule with key '%s' does not exist", key));
    }
    return rule;
  }

  /**
   * Retrieves a Rule by its id.
   *
   * Used by Views.
   */
  public Optional<RuleDto> selectById(long id, DbSession session) {
    return Optional.fromNullable(mapper(session).selectById(id));
  }

  public List<RuleDto> selectByIds(DbSession session, List<Integer> ids) {
    return executeLargeInputs(ids, new IdToDto(mapper(session)));
  }

  /**
   * Select rules by keys, whatever their status. Returns an empty list
   * if the list of {@code keys} is empty, without any db round trip.
   */
  public List<RuleDto> selectByKeys(DbSession session, List<RuleKey> keys) {
    return executeLargeInputs(keys, new KeyToDto(mapper(session)));
  }

  public List<RuleDto> selectEnabled(DbSession session) {
    return mapper(session).selectEnabled();
  }

  public void selectEnabled(DbSession session, ResultHandler resultHandler) {
    mapper(session).selectEnabled(resultHandler);
  }

  public List<RuleDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public List<RuleDto> selectByQuery(DbSession session, RuleQuery ruleQuery){
    return mapper(session).selectByQuery(ruleQuery);
  }

  public void insert(DbSession session, RuleDto dto) {
    mapper(session).insert(dto);
  }

  public void update(DbSession session, RuleDto dto) {
    mapper(session).update(dto);
  }

  private static RuleMapper mapper(DbSession session) {
    return session.getMapper(RuleMapper.class);
  }

  private static class KeyToDto implements Function<List<RuleKey>, List<RuleDto>> {
    private final RuleMapper mapper;

    private KeyToDto(RuleMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<RuleDto> apply(@Nonnull List<RuleKey> partitionOfKeys) {
      return mapper.selectByKeys(partitionOfKeys);
    }
  }

  private static class IdToDto implements Function<List<Integer>, List<RuleDto>> {
    private final RuleMapper mapper;

    private IdToDto(RuleMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<RuleDto> apply(@Nonnull List<Integer> partitionOfIds) {
      return mapper.selectByIds(partitionOfIds);
    }
  }

  /**
   * RuleParams
   */

  public List<RuleParamDto> selectRuleParamsByRuleKey(DbSession session, RuleKey key) {
    return mapper(session).selectParamsByRuleKey(key);
  }

  public List<RuleParamDto> selectRuleParamsByRuleKeys(DbSession session, List<RuleKey> ruleKeys) {
    return executeLargeInputs(ruleKeys, new KeyToRuleParamDto(mapper(session)));
  }

  public List<RuleParamDto> selectRuleParamsByRuleIds(DbSession dbSession, List<Integer> ruleIds) {
    return DatabaseUtils.executeLargeInputs(ruleIds, new IdToRuleParamDto(mapper(dbSession)));
  }

  public void insertRuleParam(DbSession session, RuleDto rule, RuleParamDto param) {
    checkNotNull(rule.getId(), "Rule id must be set");
    param.setRuleId(rule.getId());
    mapper(session).insertParameter(param);
  }

  public RuleParamDto updateRuleParam(DbSession session, RuleDto rule, RuleParamDto param) {
    checkNotNull(rule.getId(), "Rule id must be set");
    checkNotNull(param.getId(), "Rule parameter is not yet persisted must be set");
    param.setRuleId(rule.getId());
    mapper(session).updateParameter(param);
    return param;
  }

  public void deleteRuleParam(DbSession session, int ruleParameterId) {
    mapper(session).deleteParameter(ruleParameterId);
  }

  private static class KeyToRuleParamDto implements Function<List<RuleKey>, List<RuleParamDto>> {
    private final RuleMapper mapper;

    private KeyToRuleParamDto(RuleMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<RuleParamDto> apply(@Nonnull List<RuleKey> partitionOfKeys) {
      return mapper.selectParamsByRuleKeys(partitionOfKeys);
    }
  }

  private static class IdToRuleParamDto implements Function<List<Integer>, List<RuleParamDto>> {
    private final RuleMapper mapper;

    private IdToRuleParamDto(RuleMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<RuleParamDto> apply(@Nonnull List<Integer> partitionOfIds) {
      return mapper.selectParamsByRuleIds(partitionOfIds);
    }
  }
}

