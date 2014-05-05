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

import com.google.common.collect.Lists;
import org.hibernate.cfg.NotYetImplementedException;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
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

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ActiveRuleDao extends BaseDao<ActiveRuleMapper, ActiveRuleDto, ActiveRuleKey>
  implements ServerComponent {

  /** this is temporary to build RuleKey and QProfileKey */
  private RuleDao ruleDao;
  private QualityProfileDao qDao;

  public ActiveRuleDao(MyBatis mybatis, QualityProfileDao qDao, RuleDao ruleDao) {
    super(new ActiveRuleIndexDefinition(), ActiveRuleMapper.class, mybatis);
    this.ruleDao = ruleDao;
    this.qDao = qDao;
  }

  @Override
  public Iterable<ActiveRuleKey> keysOfRowsUpdatedAfter(long timestamp) {
    throw new NotYetImplementedException("Need to implement ActiveRuleDto.doGetByKey() method");
  }

  @Override
  protected ActiveRuleDto doGetByKey(ActiveRuleKey key, DbSession session) {
    QualityProfileDto qDto = qDao.selectByNameAndLanguage(key.qProfile().name(), key.qProfile().lang(), session);
    RuleDto ruleDto = ruleDao.selectByKey(key.ruleKey());
    return this.selectByProfileAndRule(qDto.getId(), ruleDto.getId(), session);
  }

  @Override
  protected ActiveRuleDto doInsert(ActiveRuleDto item, DbSession session) {
    getMapper(session).insert(item);
    return setActiveRuleKey(item, session);
  }

  @Override
  protected ActiveRuleDto doUpdate(ActiveRuleDto item, DbSession session) {
    getMapper(session).update(item);
    return setActiveRuleKey(item, session);
  }

  @Override
  protected void doDelete(ActiveRuleDto item, DbSession session) {
    getMapper(session).delete(item.getId());
  }

  @Override
  protected void doDeleteByKey(ActiveRuleKey key, DbSession session) {
    throw new NotYetImplementedException("Need to implement ActiveRuleDto.doDeleteByKey() method");
  }

  /** Helper methods to get the RuleKey and QualityProfileKey -- Temporary */

  private ActiveRuleDto setActiveRuleKey(ActiveRuleDto dto, DbSession session){
    RuleDto ruleDto = ruleDao.selectById(dto.getRulId(), session);
    QualityProfileDto qDto = qDao.selectById(dto.getProfileId(), session);
    if(qDto != null && ruleDto != null) {
      dto.setKey(QualityProfileKey.of(qDto.getName(), qDto.getLanguage()), ruleDto.getKey());
    } else {
      session.close();
      throw new IllegalStateException("ActiveRule needs to have a valid Key!!!");
    }
    return dto;
  }

  private List<ActiveRuleDto> setActiveRuleKey(List<ActiveRuleDto> dtos, DbSession session){
    for(ActiveRuleDto dto:dtos) {
      setActiveRuleKey(dto, session);
    }
    return dtos;
  }


  public void delete(int activeRuleId, DbSession session) {
    session.getMapper(ActiveRuleMapper.class).delete(activeRuleId);
  }

  public void delete(int activeRuleId) {
    DbSession session = mybatis.openSession(false);
    try {
      delete(activeRuleId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteFromRule(int ruleId, DbSession session) {
    session.getMapper(ActiveRuleMapper.class).deleteFromRule(ruleId);
  }

  public void deleteFromRule(int ruleId) {
    DbSession session = mybatis.openSession(false);
    try {
      deleteFromRule(ruleId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteFromProfile(int profileId, DbSession session) {
    session.getMapper(ActiveRuleMapper.class).deleteFromProfile(profileId);
  }

  public void deleteFromProfile(int profileId) {
    DbSession session = mybatis.openSession(false);
    try {
      deleteFromProfile(profileId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActiveRuleDto> selectByIds(List<Integer> ids) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByIds(ids, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActiveRuleDto> selectByIds(Collection<Integer> ids, DbSession session) {
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }
    List<ActiveRuleDto> dtosList = newArrayList();
    List<List<Integer>> idsPartitionList = Lists.partition(newArrayList(ids), 1000);
    for (List<Integer> idsPartition : idsPartitionList) {
      List<ActiveRuleDto> dtos = session.selectList("org.sonar.core.qualityprofile.db.ActiveRuleMapper.selectByIds", newArrayList(idsPartition));
      dtosList.addAll(dtos);
    }
    return setActiveRuleKey(dtosList, session);
  }

  public List<ActiveRuleDto> selectAll() {
    DbSession session = mybatis.openSession(false);
    try {
      return selectAll(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActiveRuleDto> selectAll(DbSession session) {
    return setActiveRuleKey(session.getMapper(ActiveRuleMapper.class).selectAll(), session);
  }

  public List<ActiveRuleDto> selectByRuleId(int ruleId) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByRuleId(ruleId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActiveRuleDto> selectByRuleId(int ruleId, DbSession session) {
    return setActiveRuleKey(session.getMapper(ActiveRuleMapper.class).selectByRuleId(ruleId), session);
  }

  public List<ActiveRuleDto> selectByProfileId(int profileId) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByProfileId(profileId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActiveRuleDto> selectByProfileId(int profileId, DbSession session) {
    return setActiveRuleKey(session.getMapper(ActiveRuleMapper.class).selectByProfileId(profileId), session);
  }


  @CheckForNull
  public ActiveRuleDto selectById(int id) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public ActiveRuleDto selectById(int id, DbSession session) {
    return setActiveRuleKey(session.getMapper(ActiveRuleMapper.class).selectById(id), session);
  }

  @CheckForNull
  public ActiveRuleDto selectByProfileAndRule(int profileId, int ruleId) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByProfileAndRule(profileId, ruleId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public ActiveRuleDto selectByProfileAndRule(int profileId, int ruleId, DbSession session) {
    return setActiveRuleKey(session.getMapper(ActiveRuleMapper.class).selectByProfileAndRule(profileId, ruleId), session);
  }

  public void insert(ActiveRuleParamDto param, DbSession session) {
    session.getMapper(ActiveRuleMapper.class).insertParameter(param);
    ActiveRuleDto dto = this.selectById(param.getActiveRuleId(), session);
    if(dto != null) {
      session.enqueue(new EmbeddedIndexAction<ActiveRuleKey>(this.getIndexType(),
        IndexAction.Method.UPDATE, param, dto.getKey()));
    }
  }

  public void insert(ActiveRuleParamDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      insert(dto, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(ActiveRuleParamDto param, DbSession session) {
    session.getMapper(ActiveRuleMapper.class).updateParameter(param);
    ActiveRuleDto dto = this.selectById(param.getActiveRuleId(), session);
    if(dto != null) {
      session.enqueue(new EmbeddedIndexAction<ActiveRuleKey>(this.getIndexType(),
        IndexAction.Method.UPDATE, param, dto.getKey()));
    }
  }

  public void update(ActiveRuleParamDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      update(dto, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }


  public void deleteParameter(int activeRuleParamId, DbSession session) {
    session.getMapper(ActiveRuleMapper.class).deleteParameter(activeRuleParamId);
  }

  public void deleteParameter(int activeRuleParamId) {
    DbSession session = mybatis.openSession(false);
    try {
      deleteParameter(activeRuleParamId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteParameters(int activeRuleId, DbSession session) {
    session.getMapper(ActiveRuleMapper.class).deleteParameters(activeRuleId);
  }

  public void deleteParameters(int activeRuleId) {
    DbSession session = mybatis.openSession(false);
    try {
      deleteParameters(activeRuleId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteParametersWithParamId(int id, DbSession session) {
    session.getMapper(ActiveRuleMapper.class).deleteParametersWithParamId(id);
  }

  public void deleteParametersFromProfile(int profileId) {
    DbSession session = mybatis.openSession(false);
    try {
      deleteParametersFromProfile(profileId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteParametersFromProfile(int profileId, DbSession session) {
    session.getMapper(ActiveRuleMapper.class).deleteParametersFromProfile(profileId);
  }

  public ActiveRuleParamDto selectParamById(Integer activeRuleParamId) {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ActiveRuleMapper.class).selectParamById(activeRuleParamId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ActiveRuleParamDto selectParamByActiveRuleAndKey(int activeRuleId, String key) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectParamByActiveRuleAndKey(activeRuleId, key, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public ActiveRuleParamDto selectParamByActiveRuleAndKey(int activeRuleId, String key, DbSession session) {
    return session.getMapper(ActiveRuleMapper.class).selectParamByActiveRuleAndKey(activeRuleId, key);
  }

  public List<ActiveRuleParamDto> selectParamsByActiveRuleId(int activeRuleId) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectParamsByActiveRuleId(activeRuleId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActiveRuleParamDto> selectParamsByActiveRuleId(int activeRuleId, DbSession session) {
    return session.getMapper(ActiveRuleMapper.class).selectParamsByActiveRuleId(activeRuleId);
  }

  public List<ActiveRuleParamDto> selectParamsByActiveRuleIds(List<Integer> activeRuleIds) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectParamsByActiveRuleIds(activeRuleIds, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActiveRuleParamDto> selectParamsByActiveRuleIds(Collection<Integer> activeRuleIds, DbSession session) {
    if (activeRuleIds.isEmpty()) {
      return Collections.emptyList();
    }
    List<ActiveRuleParamDto> dtosList = newArrayList();
    List<List<Integer>> idsPartitionList = Lists.partition(newArrayList(activeRuleIds), 1000);
    for (List<Integer> idsPartition : idsPartitionList) {
      List<ActiveRuleParamDto> dtos = session.selectList("org.sonar.core.qualityprofile.db.ActiveRuleMapper.selectParamsByActiveRuleIds", newArrayList(idsPartition));
      dtosList.addAll(dtos);
    }
    return dtosList;
  }

  public List<ActiveRuleParamDto> selectAllParams() {
    DbSession session = mybatis.openSession(false);
    try {
      return selectAllParams(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<ActiveRuleParamDto> selectAllParams(DbSession session) {
    return session.getMapper(ActiveRuleMapper.class).selectAllParams();
  }

  public List<ActiveRuleParamDto> selectParamsByProfileId(int profileId) {
    DbSession session = mybatis.openSession(false);
    try {
      return session.getMapper(ActiveRuleMapper.class).selectParamsByProfileId(profileId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
