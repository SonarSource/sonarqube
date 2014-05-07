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

package org.sonar.server.rule2;

import com.google.common.base.Preconditions;
import org.sonar.api.rule.RuleKey;
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
import org.sonar.server.search.EmbeddedIndexAction;
import org.sonar.server.search.IndexAction;

import java.util.List;

public class ActiveRuleDao extends BaseDao<ActiveRuleMapper, ActiveRuleDto, ActiveRuleKey> {

  private final RuleDao ruleDao;
  private final QualityProfileDao profileDao;

  public ActiveRuleDao(QualityProfileDao profileDao, RuleDao ruleDao) {
    super(new ActiveRuleIndexDefinition(), ActiveRuleMapper.class);
    this.ruleDao = ruleDao;
    this.profileDao = profileDao;
  }

  @Override
  public ActiveRuleKey getKey(ActiveRuleDto item, DbSession session) {
    if (item.getKey() != null) {
      return item.getKey();
    }
    fixIdsAndKeys(item, session);
    return item.getKey();
  }

  @Override
  public Iterable<ActiveRuleKey> keysOfRowsUpdatedAfter(long timestamp, DbSession session) {
    throw new UnsupportedOperationException("Need to implement ActiveRuleDto.doGetByKey() method");
  }

  @Override
  protected ActiveRuleDto doGetByKey(ActiveRuleKey key, DbSession session) {
    QualityProfileDto qDto = profileDao.selectByNameAndLanguage(key.qProfile().name(), key.qProfile().lang(), session);
    RuleDto ruleDto = ruleDao.getByKey(key.ruleKey(), session);
    return mapper(session).selectByProfileAndRule(qDto.getId(), ruleDto.getId());
  }

  @Override
  protected ActiveRuleDto doInsert(ActiveRuleDto item, DbSession session) {
    Preconditions.checkArgument(item.getProfileId() != null, "Quality profile is not persisted (missing id)");
    Preconditions.checkArgument(item.getRulId() != null, "Rule is not persisted (missing id)");
    Preconditions.checkArgument(item.getId() == null, "ActiveRule is already persisted");
    mapper(session).insert(item);
    return item;
  }

  private void fixIdsAndKeys(ActiveRuleDto item, DbSession session) {
    if (item.getKey() != null) {
      if (item.getProfileId() == null) {
        QualityProfileDto profile = profileDao.selectByNameAndLanguage(item.getKey().qProfile().name(), item.getKey().qProfile().lang(), session);
        // TODO fail if profile not found
        item.setProfileId(profile.getId());
      }
      if (item.getRulId() == null) {
        RuleDto rule = ruleDao.getByKey(item.getKey().ruleKey(), session);
        // TODO fail if rule not found
        item.setRuleId(rule.getId());
      }
    } else {
      // key is null
      if (item.getProfileId() != null && item.getRulId() != null) {
        QualityProfileDto profile = profileDao.selectById(item.getProfileId(), session);
        RuleDto rule = ruleDao.getById(item.getRulId(), session);
        RuleKey ruleKey = ruleDao.getKey(rule, session);
        item.setKey(ActiveRuleKey.of(QualityProfileKey.of(profile.getName(), profile.getLanguage()), ruleKey));
      }
    }
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
    throw new UnsupportedOperationException("TODO");
  }

  public List<ActiveRuleParamDto> findParamsByActiveRule(ActiveRuleDto dto, DbSession session) {
    Preconditions.checkArgument(dto.getId()!=null, "ActiveRule is not persisted");
    return mapper(session).selectParamsByActiveRuleId(dto.getId());
  }

  public ActiveRuleParamDto addParam(ActiveRuleDto activeRule, ActiveRuleParamDto paramDto, DbSession session) {
    Preconditions.checkState(activeRule.getId() != null, "ActiveRule id is not yet persisted");
    Preconditions.checkState(paramDto.getId() == null, "ActiveRuleParam is already persisted");
    Preconditions.checkState(paramDto.getRulesParameterId() != null, "Rule param is not persisted");

    paramDto.setActiveRuleId(activeRule.getId());
    mapper(session).insertParameter(paramDto);
    session.enqueue(new EmbeddedIndexAction<ActiveRuleKey>(this.getIndexType(), IndexAction.Method.INSERT, paramDto, activeRule.getKey()));
    return paramDto;
  }

  public List<ActiveRuleDto> findByRule(RuleDto rule, DbSession dbSession) {
    Preconditions.checkArgument(rule.getId()!=null, "Rule is not persisted");
    return mapper(dbSession).selectByRuleId(rule.getId());
  }
}
