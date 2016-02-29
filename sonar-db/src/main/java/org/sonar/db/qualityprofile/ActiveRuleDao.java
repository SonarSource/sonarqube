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
package org.sonar.db.qualityprofile;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.rule.RuleDto;

public class ActiveRuleDao implements Dao {

  private static final String QUALITY_PROFILE_IS_NOT_PERSISTED = "Quality profile is not persisted (missing id)";
  private static final String RULE_IS_NOT_PERSISTED = "Rule is not persisted";
  private static final String RULE_PARAM_IS_NOT_PERSISTED = "Rule param is not persisted";
  private static final String ACTIVE_RULE_KEY_CANNOT_BE_NULL = "ActiveRuleKey cannot be null";
  private static final String ACTIVE_RULE_IS_NOT_PERSISTED = "ActiveRule is not persisted";
  private static final String ACTIVE_RULE_IS_ALREADY_PERSISTED = "ActiveRule is already persisted";
  private static final String ACTIVE_RULE_PARAM_IS_NOT_PERSISTED = "ActiveRuleParam is not persisted";
  private static final String ACTIVE_RULE_PARAM_IS_ALREADY_PERSISTED = "ActiveRuleParam is already persisted";
  private static final String PARAMETER_NAME_CANNOT_BE_NULL = "ParameterName cannot be null";

  public Optional<ActiveRuleDto> selectByKey(DbSession session, ActiveRuleKey key) {
    return Optional.fromNullable(mapper(session).selectByKey(key.qProfile(), key.ruleKey().repository(), key.ruleKey().rule()));
  }

  public ActiveRuleDto selectOrFailByKey(DbSession session, ActiveRuleKey key) {
    Optional<ActiveRuleDto> activeRule = selectByKey(session, key);
    if (activeRule.isPresent()) {
      return activeRule.get();
    }
    throw new RowNotFoundException(String.format("Active rule with key '%s' does not exist", key));
  }

  public List<ActiveRuleDto> selectByKeys(DbSession dbSession, List<ActiveRuleKey> keys) {
    return DatabaseUtils.executeLargeInputs(keys, new KeyToDto(mapper(dbSession)));
  }

  public List<ActiveRuleDto> selectByRule(DbSession dbSession, RuleDto rule) {
    Preconditions.checkNotNull(rule.getId(), RULE_IS_NOT_PERSISTED);
    return mapper(dbSession).selectByRuleId(rule.getId());
  }

  public List<ActiveRuleDto> selectByRuleIds(DbSession dbSession, List<Integer> ids) {
    return DatabaseUtils.executeLargeInputs(ids, new RuleIdToDto(mapper(dbSession)));
  }

  // TODO As it's only used by MediumTest, it should be replaced by DbTester.countRowsOfTable()
  public List<ActiveRuleDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public List<ActiveRuleDto> selectByProfileKey(DbSession session, String profileKey) {
    return mapper(session).selectByProfileKey(profileKey);
  }

  public ActiveRuleDto insert(DbSession session, ActiveRuleDto item) {
    Preconditions.checkArgument(item.getProfileId() != null, QUALITY_PROFILE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(item.getRuleId() != null, RULE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(item.getId() == null, ACTIVE_RULE_IS_ALREADY_PERSISTED);
    mapper(session).insert(item);
    return item;
  }

  public ActiveRuleDto update(DbSession session, ActiveRuleDto item) {
    Preconditions.checkArgument(item.getProfileId() != null, QUALITY_PROFILE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(item.getRuleId() != null, ActiveRuleDao.RULE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(item.getId() != null, ACTIVE_RULE_IS_NOT_PERSISTED);
    mapper(session).update(item);
    return item;
  }

  public void delete(DbSession session, ActiveRuleKey key) {
    Optional<ActiveRuleDto> activeRule = selectByKey(session, key);
    if (activeRule.isPresent()) {
      mapper(session).deleteParameters(activeRule.get().getId());
      mapper(session).delete(activeRule.get().getId());
    }
  }

  /**
   * Nested DTO ActiveRuleParams
   */

  public List<ActiveRuleParamDto> selectParamsByActiveRuleId(DbSession dbSession, Integer activeRuleId) {
    return mapper(dbSession).selectParamsByActiveRuleId(activeRuleId);
  }

  public List<ActiveRuleParamDto> selectParamsByActiveRuleIds(final DbSession dbSession, List<Integer> activeRuleIds) {
    return DatabaseUtils.executeLargeInputs(activeRuleIds, new ParamIdToDto(mapper(dbSession)));
  }

  /**
   * Warning ! This method is executing 2 queries : one to get the active rule from the key, and another one to get parameters
   */
  private List<ActiveRuleParamDto> selectParamsByActiveRuleKey(DbSession session, ActiveRuleKey key) {
    Preconditions.checkNotNull(key, ACTIVE_RULE_KEY_CANNOT_BE_NULL);
    ActiveRuleDto activeRule = selectOrFailByKey(session, key);
    return mapper(session).selectParamsByActiveRuleId(activeRule.getId());
  }

  @CheckForNull
  public ActiveRuleParamDto selectParamByKeyAndName(ActiveRuleKey key, String name, DbSession session) {
    Preconditions.checkNotNull(key, ACTIVE_RULE_KEY_CANNOT_BE_NULL);
    Preconditions.checkNotNull(name, PARAMETER_NAME_CANNOT_BE_NULL);
    Optional<ActiveRuleDto> activeRule = selectByKey(session, key);
    if (activeRule.isPresent()) {
      return mapper(session).selectParamByActiveRuleAndKey(activeRule.get().getId(), name);
    }
    return null;
  }

  public List<ActiveRuleParamDto> selectAllParams(DbSession dbSession) {
    return mapper(dbSession).selectAllParams();
  }

  public ActiveRuleParamDto insertParam(DbSession session, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkArgument(activeRule.getId() != null, ACTIVE_RULE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(activeRuleParam.getId() == null, ACTIVE_RULE_PARAM_IS_ALREADY_PERSISTED);
    Preconditions.checkNotNull(activeRuleParam.getRulesParameterId(), RULE_PARAM_IS_NOT_PERSISTED);

    activeRuleParam.setActiveRuleId(activeRule.getId());
    mapper(session).insertParameter(activeRuleParam);
    return activeRuleParam;
  }

  public void deleteParamByKeyAndName(DbSession session, ActiveRuleKey key, String param) {
    // TODO SQL rewrite to delete by key
    Optional<ActiveRuleDto> activeRule = selectByKey(session, key);
    if (activeRule.isPresent()) {
      ActiveRuleParamDto activeRuleParam = mapper(session).selectParamByActiveRuleAndKey(activeRule.get().getId(), param);
      if (activeRuleParam != null) {
        mapper(session).deleteParameter(activeRuleParam.getId());
      }
    }
  }

  public void updateParam(DbSession session, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRule.getId(), ACTIVE_RULE_IS_NOT_PERSISTED);
    Preconditions.checkNotNull(activeRuleParam.getId(), ACTIVE_RULE_PARAM_IS_NOT_PERSISTED);
    mapper(session).updateParameter(activeRuleParam);
  }

  public void deleteParam(DbSession session, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRule.getId(), ACTIVE_RULE_IS_NOT_PERSISTED);
    Preconditions.checkNotNull(activeRuleParam.getId(), ACTIVE_RULE_PARAM_IS_NOT_PERSISTED);
    mapper(session).deleteParameter(activeRuleParam.getId());
  }

  public void deleteParamsByRuleParam(DbSession dbSession, RuleDto rule, String paramKey) {
    List<ActiveRuleDto> activeRules = selectByRule(dbSession, rule);
    for (ActiveRuleDto activeRule : activeRules) {
      for (ActiveRuleParamDto activeParam : selectParamsByActiveRuleKey(dbSession, activeRule.getKey())) {
        if (activeParam.getKey().equals(paramKey)) {
          deleteParam(dbSession, activeRule, activeParam);
        }
      }
    }
  }

  private ActiveRuleMapper mapper(DbSession session) {
    return session.getMapper(ActiveRuleMapper.class);
  }

  private static class KeyToDto implements Function<List<ActiveRuleKey>, List<ActiveRuleDto>> {
    private final ActiveRuleMapper mapper;

    private KeyToDto(ActiveRuleMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<ActiveRuleDto> apply(@Nonnull List<ActiveRuleKey> partitionOfIds) {
      return mapper.selectByKeys(partitionOfIds);
    }
  }

  private static class ParamIdToDto implements Function<List<Integer>, List<ActiveRuleParamDto>> {
    private final ActiveRuleMapper mapper;

    private ParamIdToDto(ActiveRuleMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<ActiveRuleParamDto> apply(@Nonnull List<Integer> partitionOfIds) {
      return mapper.selectParamsByActiveRuleIds(partitionOfIds);
    }
  }

  private static class RuleIdToDto implements Function<List<Integer>, List<ActiveRuleDto>> {
    private final ActiveRuleMapper mapper;

    private RuleIdToDto(ActiveRuleMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<ActiveRuleDto> apply(@Nonnull List<Integer> partitionOfRuleIds) {
      return mapper.selectByRuleIds(partitionOfRuleIds);
    }
  }
}
