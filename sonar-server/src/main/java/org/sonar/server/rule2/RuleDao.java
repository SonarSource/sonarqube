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
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.db.UnsuportedException;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleConstants;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleMapper;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.rule.RuleRuleTagDto;
import org.sonar.server.db.BaseDao;

import javax.annotation.CheckForNull;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class RuleDao extends BaseDao<RuleDto, RuleKey>
  implements BatchComponent, ServerComponent {

  public RuleDao(MyBatis mybatis) {
    super(mybatis);
  }

  @Override
  protected String getIndexName() {
    return RuleConstants.INDEX_NAME;
  }

  @Override
  @CheckForNull
  protected RuleDto doGetByKey(RuleKey key, SqlSession session) {
    return getMapper(session).selectByKey(key);
  }

  @Override
  protected RuleDto doInsert(RuleDto item, SqlSession session) {
    session.getMapper(RuleMapper.class).insert(item);
    return item;
  }

  @Override
  protected RuleDto doUpdate(RuleDto item, SqlSession session) {
    session.getMapper(RuleMapper.class).update(item);
    return item;
  }

  @Override
  protected void doDelete(RuleDto item, SqlSession session) {
    throw new UnsuportedException("Rules cannot be deleted");
  }

  @Override
  protected void doDeleteByKey(RuleKey key, SqlSession session) {
    throw new UnsuportedException("Rules cannot be deleted");
  }

  public List<RuleDto> selectAll() {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectAll(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectAll(SqlSession session) {
    return getMapper(session).selectAll();
  }

  public List<RuleDto> selectEnablesAndNonManual() {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectEnablesAndNonManual(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectEnablesAndNonManual(SqlSession session) {
    return getMapper(session).selectEnablesAndNonManual();
  }

  public List<RuleDto> selectNonManual(SqlSession session) {
    return getMapper(session).selectNonManual();
  }

  public List<RuleDto> selectBySubCharacteristicId(Integer characteristicOrSubCharacteristicId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectBySubCharacteristicId(characteristicOrSubCharacteristicId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Return all rules (even the REMOVED ones) linked on to a sub characteristic
   */
  public List<RuleDto> selectBySubCharacteristicId(Integer subCharacteristicId, SqlSession session) {
    return getMapper(session).selectBySubCharacteristicId(subCharacteristicId);
  }

  @CheckForNull
  public RuleDto selectById(Integer id, SqlSession session) {
    return getMapper(session).selectById(id);
  }

  @CheckForNull
  public RuleDto selectById(Integer id) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleDto selectByKey(RuleKey ruleKey, SqlSession session) {
    return getMapper(session).selectByKey(ruleKey);
  }


  @CheckForNull
  public RuleDto selectByKey(RuleKey ruleKey) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectByKey(ruleKey, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleDto selectByName(String name) {
    SqlSession session = mybatis.openSession(false);
    try {
      return getMapper(session).selectByName(name);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(Collection<RuleDto> rules) {
    DbSession session = mybatis.openSession(true);
    try {
      for (RuleDto rule : rules) {
        getMapper(session).batchInsert(rule);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  // ******************************
  // Methods for Rule Parameters
  // ******************************

  public List<RuleParamDto> selectParameters() {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectParameters(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParameters(SqlSession session) {
    return getMapper(session).selectAllParams();
  }

  public List<RuleParamDto> selectParametersByRuleId(Integer ruleId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectParametersByRuleId(ruleId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParametersByRuleId(Integer ruleId, SqlSession session) {
    return selectParametersByRuleIds(newArrayList(ruleId));
  }

  public List<RuleParamDto> selectParametersByRuleIds(List<Integer> ruleIds) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectParametersByRuleIds(ruleIds, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParametersByRuleIds(List<Integer> ruleIds, SqlSession session) {
    List<RuleParamDto> dtos = newArrayList();
    List<List<Integer>> partitionList = Lists.partition(newArrayList(ruleIds), 1000);
    for (List<Integer> partition : partitionList) {
      dtos.addAll(getMapper(session).selectParamsByRuleIds(partition));
    }
    return dtos;
  }

  public void insert(RuleParamDto param, SqlSession session) {
    getMapper(session).insertParameter(param);
  }

  public void insert(RuleParamDto param) {
    SqlSession session = mybatis.openSession(false);
    try {
      insert(param, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(RuleParamDto param, SqlSession session) {
    getMapper(session).updateParameter(param);
  }

  public void update(RuleParamDto param) {
    SqlSession session = mybatis.openSession(false);
    try {
      update(param, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleParamDto selectParamByRuleAndKey(Integer ruleId, String key, SqlSession session) {
    return getMapper(session).selectParamByRuleAndKey(ruleId, key);
  }

  private RuleMapper getMapper(SqlSession session) {
    return session.getMapper(RuleMapper.class);
  }

  // ***************************
  // Methods for Rule Tags
  // ***************************

  public void insert(RuleRuleTagDto newTag, SqlSession session) {
    getMapper(session).insertTag(newTag);
  }

  public void deleteParam(RuleParamDto persistedParam, SqlSession sqlSession) {
    getMapper(sqlSession).deleteParameter(persistedParam.getId());
  }

  public void deleteTag(RuleRuleTagDto tagToDelete, SqlSession session) {
    getMapper(session).deleteTag(tagToDelete.getId().intValue());
  }

  public void update(RuleRuleTagDto existingTag, SqlSession session) {
    getMapper(session).updateTag(existingTag);
  }

  public List<RuleRuleTagDto> selectTags(SqlSession session) {
    return getMapper(session).selectAllTags();
  }

  public List<RuleRuleTagDto> selectTagsByRuleId(Integer ruleId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectTagsByRuleIds(ruleId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleRuleTagDto> selectTagsByRuleIds(Integer ruleId, SqlSession session) {
    return selectTagsByRuleIds(newArrayList(ruleId), session);
  }

  public List<RuleRuleTagDto> selectTagsByRuleIds(List<Integer> ruleIds) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectTagsByRuleIds(ruleIds, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleRuleTagDto> selectTagsByRuleIds(List<Integer> ruleIds, SqlSession session) {
    List<RuleRuleTagDto> dtos = newArrayList();
    List<List<Integer>> partitionList = Lists.partition(newArrayList(ruleIds), 1000);
    for (List<Integer> partition : partitionList) {
      dtos.addAll(getMapper(session).selectTagsByRuleIds(partition));
    }
    return dtos;
  }

  @Override
  public Collection<RuleKey> keysOfRowsUpdatedAfter(long timestamp) {
    SqlSession session = mybatis.openSession(false);
    try {
      final List<RuleKey> keys = Lists.newArrayList();
      session.select("selectKeysOfRulesUpdatedSince", new Timestamp(timestamp), new ResultHandler() {
        @Override
        public void handleResult(ResultContext context) {
          Map<String, String> map = (Map) context.getResultObject();
          keys.add(RuleKey.of(map.get("repo"), map.get("rule")));
        }
      });
      return keys;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
