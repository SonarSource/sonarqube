/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.qualityprofile.db;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleMapper;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.BaseDao;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.action.IndexAction;
import org.sonar.server.search.action.KeyIndexAction;

import javax.annotation.CheckForNull;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class ActiveRuleDao extends BaseDao<ActiveRuleMapper, ActiveRuleDto, ActiveRuleKey> {

  //TODO remove once key is finalized (used only to get id for SQL statement)
  private final RuleDao ruleDao;
  private final QualityProfileDao profileDao;

  public ActiveRuleDao(QualityProfileDao profileDao, RuleDao ruleDao) {
    this(profileDao, ruleDao, System2.INSTANCE);
  }

  @VisibleForTesting
  public ActiveRuleDao(QualityProfileDao profileDao, RuleDao ruleDao, System2 system) {
    super(IndexDefinition.ACTIVE_RULE, ActiveRuleMapper.class, system);
    this.ruleDao = ruleDao;
    this.profileDao = profileDao;
  }

  @Override
  public void synchronizeAfter(final DbSession session, long timestamp) {
    session.select("selectAllKeysAfterTimestamp", new Timestamp(timestamp), new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        Map<String, Object> fields = (Map<String, Object>) context.getResultObject();
        // "rule" is a reserved keyword in SQLServer, so "rulefield" is used
        ActiveRuleKey key = ActiveRuleKey.of(
          QualityProfileKey.of((String) fields.get("profile"), (String) fields.get("language")),
          RuleKey.of((String) fields.get("repository"), (String) fields.get("rulefield")));
        session.enqueue(new KeyIndexAction<ActiveRuleKey>(getIndexType(), IndexAction.Method.UPSERT, key));
      }
    });
    session.commit();
  }

  /**
   * @deprecated do not use ids but keys
   */
  @CheckForNull
  @Deprecated
  public ActiveRuleDto getById(DbSession session, int activeRuleId) {
    ActiveRuleDto activeRule = mapper(session).selectById(activeRuleId);

    if (activeRule != null) {
      activeRule.setKey(ActiveRuleKey.of(
        profileDao.selectById(activeRule.getProfileId(), session).getKey(),
        ruleDao.getById(session, activeRule.getRulId()).getKey()));
    }
    return activeRule;
  }

  @Override
  protected ActiveRuleDto doGetNullableByKey(DbSession session, ActiveRuleKey key) {
    return mapper(session).selectByKey(key.qProfile().name(), key.qProfile().lang(),
      key.ruleKey().repository(), key.ruleKey().rule());
  }

  @Override
  protected ActiveRuleDto doInsert(DbSession session, ActiveRuleDto item) {
    Preconditions.checkArgument(item.getProfileId() != null, "Quality profile is not persisted (missing id)");
    Preconditions.checkArgument(item.getRulId() != null, "Rule is not persisted (missing id)");
    Preconditions.checkArgument(item.getId() == null, "ActiveRule is already persisted");
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected ActiveRuleDto doUpdate(DbSession session, ActiveRuleDto item) {
    Preconditions.checkArgument(item.getProfileId() != null, "Quality profile is not persisted (missing id)");
    Preconditions.checkArgument(item.getRulId() != null, "Rule is not persisted (missing id)");
    Preconditions.checkArgument(item.getId() != null, "ActiveRule is not persisted");
    mapper(session).update(item);
    return item;
  }

  @Override
  protected void doDeleteByKey(DbSession session, ActiveRuleKey key) {
    ActiveRuleDto rule = this.getNullableByKey(session, key);
    mapper(session).deleteParameters(rule.getId());
    mapper(session).delete(rule.getId());
  }

  /**
   * Finder methods for Rules
   */

  public List<ActiveRuleDto> findByRule(DbSession dbSession, RuleDto rule) {
    Preconditions.checkNotNull(rule.getId(), "Rule is not persisted");
    return mapper(dbSession).selectByRuleId(rule.getId());
  }

  public List<ActiveRuleDto> findAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public List<ActiveRuleParamDto> findAllParams(DbSession dbSession) {
    return mapper(dbSession).selectAllParams();
  }

  /**
   * Nested DTO ActiveRuleParams
   */

  public ActiveRuleParamDto addParam(DbSession session, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRule.getId(), "ActiveRule id is not yet persisted");
    Preconditions.checkArgument(activeRuleParam.getId() == null, "ActiveRuleParam is already persisted");
    Preconditions.checkNotNull(activeRuleParam.getRulesParameterId(), "Rule param is not persisted");

    activeRuleParam.setActiveRuleId(activeRule.getId());
    mapper(session).insertParameter(activeRuleParam);
    this.enqueueInsert(activeRuleParam, activeRule.getKey(), session);
    return activeRuleParam;
  }

  public void removeParamByKeyAndName(DbSession session, ActiveRuleKey key, String param) {
    //TODO SQL rewrite to delete by key
    ActiveRuleDto activeRule = getNullableByKey(session, key);
    ActiveRuleParamDto activeRuleParam = mapper(session).selectParamByActiveRuleAndKey(activeRule.getId(), param);
    Preconditions.checkNotNull(activeRuleParam.getId(), "ActiveRuleParam does not exist");
    mapper(session).deleteParameter(activeRuleParam.getId());
  }

  public void updateParam(DbSession session, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRule.getId(), "ActiveRule is not persisted");
    Preconditions.checkNotNull(activeRuleParam.getId(), "ActiveRuleParam is not persisted");
    mapper(session).updateParameter(activeRuleParam);
    this.enqueueUpdate(activeRuleParam, activeRule.getKey(), session);
  }

  public void deleteParam(DbSession session, ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam) {
    Preconditions.checkNotNull(activeRule.getId(), "ActiveRule is not persisted");
    Preconditions.checkNotNull(activeRuleParam.getId(), "ActiveRuleParam is not persisted");
    mapper(session).updateParameter(activeRuleParam);
    this.enqueueUpdate(activeRuleParam, activeRule.getKey(), session);
  }

  public void deleteByProfileKey(DbSession session, QualityProfileKey profileKey) {
    /** Functional cascade for params */
    for (ActiveRuleDto activeRule : this.findByProfileKey(session, profileKey)) {
      delete(session, activeRule);
    }
  }

  public List<ActiveRuleDto> findByProfileKey(DbSession session, QualityProfileKey profileKey) {
    return mapper(session).selectByProfileKey(profileKey);
  }

  /**
   * Finder methods for ActiveRuleParams
   */

  public List<ActiveRuleParamDto> findParamsByActiveRuleKey(DbSession session, ActiveRuleKey key) {
    Preconditions.checkNotNull(key, "ActiveRuleKey cannot be null");
    ActiveRuleDto activeRule = this.getByKey(session, key);
    return mapper(session).selectParamsByActiveRuleId(activeRule.getId());
  }

  public ActiveRuleParamDto getParamByKeyAndName(ActiveRuleKey key, String name, DbSession session) {
    Preconditions.checkNotNull(key, "ActiveRuleKey cannot be null");
    Preconditions.checkNotNull(name, "ParameterName cannot be null");
    ActiveRuleDto activeRule = getNullableByKey(session, key);
    return mapper(session).selectParamByActiveRuleAndKey(activeRule.getId(), name);
  }

  @Deprecated
  public void removeParamByProfile(DbSession session, QProfile profile) {
    mapper(session).deleteParametersFromProfile(profile.id());
  }

  @Deprecated
  public void deleteByProfile(DbSession session, QProfile profile) {
    mapper(session).deleteFromProfile(profile.id());
  }
}
