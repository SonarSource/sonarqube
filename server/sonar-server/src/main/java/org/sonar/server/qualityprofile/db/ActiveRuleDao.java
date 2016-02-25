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
package org.sonar.server.qualityprofile.db;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.SqlActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleMapper;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.db.BaseDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexDefinition;

import static java.util.Collections.emptyList;

public class ActiveRuleDao extends BaseDao<ActiveRuleMapper, ActiveRuleDto, ActiveRuleKey> {

  private static final String QUALITY_PROFILE_IS_NOT_PERSISTED = "Quality profile is not persisted (missing id)";
  private static final String RULE_IS_NOT_PERSISTED = "Rule is not persisted";
  private static final String RULE_PARAM_IS_NOT_PERSISTED = "Rule param is not persisted";
  private static final String ACTIVE_RULE_KEY_CANNOT_BE_NULL = "ActiveRuleKey cannot be null";
  private static final String ACTIVE_RULE_IS_NOT_PERSISTED = "ActiveRule is not persisted";
  private static final String ACTIVE_RULE_IS_ALREADY_PERSISTED = "ActiveRule is already persisted";
  private static final String ACTIVE_RULE_PARAM_IS_NOT_PERSISTED = "ActiveRuleParam is not persisted";
  private static final String ACTIVE_RULE_PARAM_IS_ALREADY_PERSISTED = "ActiveRuleParam is already persisted";
  private static final String PARAMETER_NAME_CANNOT_BE_NULL = "ParameterName cannot be null";

  // TODO remove once key is finalized (used only to get id for SQL statement)
  private final RuleDao ruleDao;
  private final QualityProfileDao profileDao;

  public ActiveRuleDao(QualityProfileDao profileDao, RuleDao ruleDao, System2 system) {
    super(IndexDefinition.ACTIVE_RULE, ActiveRuleMapper.class, system);
    this.ruleDao = ruleDao;
    this.profileDao = profileDao;
  }

  @CheckForNull
  @Deprecated
  public ActiveRuleDto selectById(DbSession session, int activeRuleId) {
    ActiveRuleDto activeRule = mapper(session).selectById(activeRuleId);
    if (activeRule != null) {
      QualityProfileDto profile = profileDao.selectById(session, activeRule.getProfileId());
      RuleDto rule = ruleDao.selectById(session, activeRule.getRuleId());
      if (profile != null && rule != null) {
        activeRule.setKey(ActiveRuleKey.of(profile.getKey(), rule.getKey()));
        return activeRule;
      }
    }
    return null;
  }

  @Override
  protected ActiveRuleDto doGetNullableByKey(DbSession session, ActiveRuleKey key) {
    return mapper(session).selectByKey(key.qProfile(), key.ruleKey().repository(), key.ruleKey().rule());
  }

  @Override
  protected ActiveRuleDto doInsert(DbSession session, ActiveRuleDto item) {
    Preconditions.checkArgument(item.getProfileId() != null, QUALITY_PROFILE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(item.getRuleId() != null, RULE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(item.getId() == null, ACTIVE_RULE_IS_ALREADY_PERSISTED);
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected ActiveRuleDto doUpdate(DbSession session, ActiveRuleDto item) {
    Preconditions.checkArgument(item.getProfileId() != null, QUALITY_PROFILE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(item.getRuleId() != null, ActiveRuleDao.RULE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(item.getId() != null, ACTIVE_RULE_IS_NOT_PERSISTED);
    mapper(session).update(item);
    return item;
  }

  @Override
  protected void doDeleteByKey(DbSession session, ActiveRuleKey key) {
    ActiveRuleDto activeRule = getNullableByKey(session, key);
    if (activeRule != null) {
      mapper(session).deleteParameters(activeRule.getId());
      mapper(session).delete(activeRule.getId());
    }
  }

  /**
   * Finder methods for Rules
   */

  public List<ActiveRuleDto> selectByRule(DbSession dbSession, RuleDto rule) {
    Preconditions.checkNotNull(rule.getId(), RULE_IS_NOT_PERSISTED);
    return mapper(dbSession).selectByRuleId(rule.getId());
  }

  public List<ActiveRuleDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public List<ActiveRuleParamDto> selectAllParams(DbSession dbSession) {
    return mapper(dbSession).selectAllParams();
  }

  public Optional<ActiveRuleDto> selectByActiveRuleKey(DbSession dbSession, ActiveRuleKey key) {
    return Optional.fromNullable(mapper(dbSession).selectByKey(key.qProfile(), key.ruleKey().repository(), key.ruleKey().rule()));
  }

  public List<ActiveRuleDto> selectByActiveRuleKeys(final DbSession dbSession, final List<ActiveRuleKey> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }

    List<SqlActiveRuleKey> sqlKeys = new ArrayList<>();
    for (ActiveRuleKey key : keys) {
      sqlKeys.add(new SqlActiveRuleKey(key));
    }

    return DatabaseUtils.executeLargeInputs(sqlKeys, new Function<List<SqlActiveRuleKey>, List<ActiveRuleDto>>() {
      @Override
      public List<ActiveRuleDto> apply(@Nonnull List<SqlActiveRuleKey> input) {
        return mapper(dbSession).selectByKeys(input);
      }
    });
  }

  /**
   * Nested DTO ActiveRuleParams
   */

  public ActiveRuleParamDto insertParam(DbSession session, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkArgument(activeRule.getId() != null, ACTIVE_RULE_IS_NOT_PERSISTED);
    Preconditions.checkArgument(activeRuleParam.getId() == null, ACTIVE_RULE_PARAM_IS_ALREADY_PERSISTED);
    Preconditions.checkNotNull(activeRuleParam.getRulesParameterId(), RULE_PARAM_IS_NOT_PERSISTED);

    activeRuleParam.setActiveRuleId(activeRule.getId());
    mapper(session).insertParameter(activeRuleParam);
    this.enqueueInsert(activeRuleParam, activeRule.getKey(), session);
    return activeRuleParam;
  }

  public void deleteParamByKeyAndName(DbSession session, ActiveRuleKey key, String param) {
    // TODO SQL rewrite to delete by key
    ActiveRuleDto activeRule = getNullableByKey(session, key);
    if (activeRule != null) {
      ActiveRuleParamDto activeRuleParam = mapper(session).selectParamByActiveRuleAndKey(activeRule.getId(), param);
      if (activeRuleParam != null) {
        mapper(session).deleteParameter(activeRuleParam.getId());
      }
    }
  }

  public void updateParam(DbSession session, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRule.getId(), ACTIVE_RULE_IS_NOT_PERSISTED);
    Preconditions.checkNotNull(activeRuleParam.getId(), ACTIVE_RULE_PARAM_IS_NOT_PERSISTED);
    mapper(session).updateParameter(activeRuleParam);
    this.enqueueUpdate(activeRuleParam, activeRule.getKey(), session);
  }

  public void deleteParam(DbSession session, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRule.getId(), ACTIVE_RULE_IS_NOT_PERSISTED);
    Preconditions.checkNotNull(activeRuleParam.getId(), ACTIVE_RULE_PARAM_IS_NOT_PERSISTED);
    mapper(session).deleteParameter(activeRuleParam.getId());
    this.enqueueDelete(activeRuleParam, activeRule.getKey(), session);
  }

  public void deleteByProfileKey(DbSession session, String profileKey) {
    /** Functional cascade for params */
    for (ActiveRuleDto activeRule : selectByProfileKey(session, profileKey)) {
      delete(session, activeRule);
    }
  }

  public List<ActiveRuleDto> selectByProfileKey(DbSession session, String profileKey) {
    return mapper(session).selectByProfileKey(profileKey);
  }

  /**
   * Finder methods for ActiveRuleParams
   */

  public List<ActiveRuleParamDto> selectParamsByActiveRuleKey(DbSession session, ActiveRuleKey key) {
    Preconditions.checkNotNull(key, ACTIVE_RULE_KEY_CANNOT_BE_NULL);
    ActiveRuleDto activeRule = this.getByKey(session, key);
    return mapper(session).selectParamsByActiveRuleId(activeRule.getId());
  }

  public List<ActiveRuleParamDto> selectParamsByActiveRuleId(DbSession dbSession, Integer activeRuleId) {
    return mapper(dbSession).selectParamsByActiveRuleId(activeRuleId);
  }

  public List<ActiveRuleParamDto> selectParamsByActiveRuleIds(final DbSession dbSession, List<Integer> activeRuleIds) {
    if (activeRuleIds.isEmpty()) {
      return emptyList();
    }

    return DatabaseUtils.executeLargeInputs(activeRuleIds, new Function<List<Integer>, List<ActiveRuleParamDto>>() {
      @Override
      public List<ActiveRuleParamDto> apply(@Nullable List<Integer> input) {
        return mapper(dbSession).selectParamsByActiveRuleIds(input);
      }
    });
  }

  @CheckForNull
  public ActiveRuleParamDto selectParamByKeyAndName(ActiveRuleKey key, String name, DbSession session) {
    Preconditions.checkNotNull(key, ACTIVE_RULE_KEY_CANNOT_BE_NULL);
    Preconditions.checkNotNull(name, PARAMETER_NAME_CANNOT_BE_NULL);
    ActiveRuleDto activeRule = getNullableByKey(session, key);
    if (activeRule != null) {
      return mapper(session).selectParamByActiveRuleAndKey(activeRule.getId(), name);
    }
    return null;
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
}
