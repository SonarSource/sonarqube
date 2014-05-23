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
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.BaseDao;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexDefinition;
import org.sonar.server.rule2.persistence.RuleDao;

import java.util.Date;
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
    super(new ActiveRuleIndexDefinition(), ActiveRuleMapper.class, system);
    this.ruleDao = ruleDao;
    this.profileDao = profileDao;
  }

  @Override
  public void synchronizeAfter(long timestamp, DbSession session) {
    session.select("selectAllKeysAfterTimestamp",new Date(timestamp), new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        Map<String, Object> resultObject = (Map<String, Object>) context.getResultObject();
        resultObject.get(|TOTO);
        //session.enqueue();
      }
    });
  }


  public ActiveRuleDto createActiveRule(QualityProfileKey profileKey, RuleKey ruleKey, String severity, DbSession session) {
    RuleDto ruleDto = ruleDao.getByKey(ruleKey, session);
    QualityProfileDto profileDto = profileDao.selectByNameAndLanguage(profileKey.name(), profileKey.lang(), session);
    ActiveRuleDto activeRule = ActiveRuleDto.createFor(profileDto, ruleDto)
      .setSeverity(severity);
    insert(activeRule, session);

//    List<RuleParamDto> ruleParams = ruleDao.findRuleParamsByRuleKey(ruleKey, session);
//    List<ActiveRuleParamDto> activeRuleParams = newArrayList();
//    for (RuleParamDto ruleParam : ruleParams) {
//      ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(ruleParam)
//        .setKey(ruleParam.getName())
//        .setValue(ruleParam.getDefaultValue());
//      activeRuleParams.add(activeRuleParam);
//      addParam(activeRule, activeRuleParam, session);
//    }
    return activeRule;
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
  protected ActiveRuleDto doGetByKey(ActiveRuleKey key, DbSession session) {
    return mapper(session).selectByKey(key.qProfile().name(), key.qProfile().lang(),
      key.ruleKey().repository(), key.ruleKey().rule());
  }

  @Override
  protected ActiveRuleDto doInsert(ActiveRuleDto item, DbSession session) {
    Preconditions.checkArgument(item.getProfileId() != null, "Quality profile is not persisted (missing id)");
    Preconditions.checkArgument(item.getRulId() != null, "Rule is not persisted (missing id)");
    Preconditions.checkArgument(item.getId() == null, "ActiveRule is already persisted");
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected ActiveRuleDto doUpdate(ActiveRuleDto item, DbSession session) {
    Preconditions.checkArgument(item.getProfileId() != null, "Quality profile is not persisted (missing id)");
    Preconditions.checkArgument(item.getRulId() != null, "Rule is not persisted (missing id)");
    Preconditions.checkArgument(item.getId() != null, "ActiveRule is not persisted");
    mapper(session).update(item);
    return item;
  }

  @Override
  protected void doDeleteByKey(ActiveRuleKey key, DbSession session) {
    ActiveRuleDto rule = this.getByKey(key, session);
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
    ActiveRuleDto activeRule = this.getByKey(key, session);
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
    this.removeParamByProfileKey(profileKey, session);
    for (ActiveRuleDto activeRule : this.findByProfileKey(profileKey, session)) {
      this.delete(activeRule, session);
    }
  }

  public List<ActiveRuleDto> findByProfileKey(QualityProfileKey profileKey, DbSession session) {
    //TODO try/catch due to legacy selectByProfileId SQL.
    try {
      int id = this.getQualityProfileId(profileKey, session);
      return mapper(session).selectByProfileId(id);
    } catch (Exception e) {
      throw new IllegalArgumentException("Quality profile not found: "+profileKey.toString());
    }
  }

  public void removeParamByProfileKey(QualityProfileKey profileKey, DbSession session) {
    int id = this.getQualityProfileId(profileKey, session);
    mapper(session).deleteParametersFromProfile(id);
  }

  @Deprecated
  //TODO Needed until SQL rewrite with KEY fields.
  private int getQualityProfileId(QualityProfileKey profileKey, DbSession session){
    return profileDao.selectByNameAndLanguage(profileKey.name(), profileKey.lang(), session).getId();
  }

  /**
   * Finder methods for ActiveRuleParams
   */

  public List<ActiveRuleParamDto> findParamsByKey(ActiveRuleKey key, DbSession session) {
    Preconditions.checkArgument(key != null, "ActiveRuleKey cannot be null");
    ActiveRuleDto activeRule = this.getByKey(key,session);
    return mapper(session).selectParamsByActiveRuleId(activeRule.getId());
  }

  public ActiveRuleParamDto getParamsByKeyAndName(ActiveRuleKey key, String name,  DbSession session) {
    Preconditions.checkArgument(key != null, "ActiveRuleKey cannot be null");
    Preconditions.checkArgument(name != null, "ParameterName cannot be null");
    ActiveRuleDto activeRule = this.getByKey(key,session);
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
