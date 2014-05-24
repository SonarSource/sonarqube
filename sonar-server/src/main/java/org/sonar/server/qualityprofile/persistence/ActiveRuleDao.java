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

package org.sonar.server.qualityprofile.persistence;

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

  @Deprecated
  public ActiveRuleDto getById(int activeRuleId, DbSession session) {
    ActiveRuleDto rule = mapper(session).selectById(activeRuleId);

    rule.setKey(ActiveRuleKey.of(
      profileDao.selectById(rule.getProfileId(), session).getKey(),
      ruleDao.getById(rule.getRulId(), session).getKey()));

    return rule;
  }

  @Override
  protected ActiveRuleDto doGetByKey(DbSession session, ActiveRuleKey key) {
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
    ActiveRuleDto rule = this.getByKey(session, key);
    mapper(session).deleteParameters(rule.getId());
    mapper(session).delete(rule.getId());
  }

  /**
   * Finder methods for Rules
   */

  public List<ActiveRuleDto> findByRule(RuleDto rule, DbSession dbSession) {
    Preconditions.checkArgument(rule.getId() != null, "Rule is not persisted");
    return mapper(dbSession).selectByRuleId(rule.getId());
  }

  /**
   * Nested DTO ActiveRuleParams
   */

  public ActiveRuleParamDto addParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, DbSession session) {
    Preconditions.checkState(activeRule.getId() != null, "ActiveRule id is not yet persisted");
    Preconditions.checkState(activeRuleParam.getId() == null, "ActiveRuleParam is already persisted");
    Preconditions.checkState(activeRuleParam.getRulesParameterId() != null, "Rule param is not persisted");

    activeRuleParam.setActiveRuleId(activeRule.getId());
    mapper(session).insertParameter(activeRuleParam);
    this.enqueueInsert(activeRuleParam, activeRule.getKey(), session);
    return activeRuleParam;
  }

  public void removeAllParam(ActiveRuleDto activeRule, DbSession session) {
    Preconditions.checkArgument(activeRule.getId() != null, "ActiveRule is not persisted");
    //TODO Optimize this
    for (ActiveRuleParamDto activeRuleParam : this.findParamsByActiveRule(activeRule, session)) {
      this.enqueueDelete(activeRuleParam, activeRule.getKey(), session);
    }
    mapper(session).deleteParameters(activeRule.getId());
  }

  public void removeParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, DbSession session) {
    Preconditions.checkArgument(activeRule.getId() != null, "ActiveRule is not persisted");
    Preconditions.checkArgument(activeRuleParam.getId() != null, "ActiveRuleParam is not persisted");
    mapper(session).deleteParameter(activeRuleParam.getId());
    this.enqueueDelete(activeRuleParam, activeRule.getKey(), session);
  }

  public void removeParamByKeyAndName(ActiveRuleKey key, String param, DbSession session) {
    //TODO SQL rewrite to delete by key
    ActiveRuleDto activeRule = this.getByKey(session, key);
    ActiveRuleParamDto activeRuleParam = mapper(session).selectParamByActiveRuleAndKey(activeRule.getId(), param);
    Preconditions.checkArgument(activeRuleParam.getId() != null, "ActiveRuleParam does not exist");
    mapper(session).deleteParameter(activeRuleParam.getId());
  }

  public void updateParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, DbSession session) {
    Preconditions.checkArgument(activeRule.getId() != null, "ActiveRule is not persisted");
    Preconditions.checkArgument(activeRuleParam.getId() != null, "ActiveRuleParam is not persisted");
    mapper(session).updateParameter(activeRuleParam);
    this.enqueueUpdate(activeRuleParam, activeRule.getKey(), session);
  }

  public ActiveRuleParamDto getParamsByActiveRuleAndKey(ActiveRuleDto activeRule, String key, DbSession session) {
    Preconditions.checkArgument(activeRule.getId() != null, "ActiveRule is not persisted");
    Preconditions.checkArgument(key != null, "Param key cannot be null");
    return mapper(session).selectParamByActiveRuleAndKey(activeRule.getId(), key);
  }

  public void deleteByProfileKey(QualityProfileKey profileKey, DbSession session) {
    /** Functional cascade for params */
    for (ActiveRuleDto activeRule : this.findByProfileKey(profileKey, session)) {
      delete(session, activeRule);
    }
  }

  public List<ActiveRuleDto> findByProfileKey(QualityProfileKey profileKey, DbSession session) {
    return mapper(session).selectByProfileKey(profileKey);
  }

  public void removeParamByProfileKey(QualityProfileKey profileKey, DbSession session) {
    int id = this.getQualityProfileId(profileKey, session);
    mapper(session).deleteParametersFromProfile(id);
  }

  @Deprecated
  //TODO Needed until SQL rewrite with KEY fields.
  private int getQualityProfileId(QualityProfileKey profileKey, DbSession session) {
    return profileDao.selectByNameAndLanguage(profileKey.name(), profileKey.lang(), session).getId();
  }

  /**
   * Finder methods for ActiveRuleParams
   */

  public List<ActiveRuleParamDto> findParamsByKey(ActiveRuleKey key, DbSession session) {
    Preconditions.checkArgument(key != null, "ActiveRuleKey cannot be null");
    ActiveRuleDto activeRule = this.getByKey(session, key);
    return mapper(session).selectParamsByActiveRuleId(activeRule.getId());
  }

  public ActiveRuleParamDto getParamsByKeyAndName(ActiveRuleKey key, String name, DbSession session) {
    Preconditions.checkArgument(key != null, "ActiveRuleKey cannot be null");
    Preconditions.checkArgument(name != null, "ParameterName cannot be null");
    ActiveRuleDto activeRule = this.getByKey(session, key);
    return mapper(session).selectParamByActiveRuleAndKey(activeRule.getId(), name);
  }


  public List<ActiveRuleParamDto> findParamsByActiveRule(ActiveRuleDto dto, DbSession session) {
    Preconditions.checkArgument(dto.getId() != null, "ActiveRule is not persisted");
    return mapper(session).selectParamsByActiveRuleId(dto.getId());
  }

  @Deprecated
  public void removeParamByProfile(QProfile profile, DbSession session) {
    mapper(session).deleteParametersFromProfile(profile.id());
  }

  @Deprecated
  public void deleteByProfile(QProfile profile, DbSession session) {
    mapper(session).deleteFromProfile(profile.id());
  }
}
