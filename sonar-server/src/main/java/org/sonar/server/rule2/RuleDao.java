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
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleConstants;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleMapper;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.rule.RuleRuleTagDto;
import org.sonar.server.db.BaseDao;
import org.sonar.server.search.DtoIndexAction;
import org.sonar.server.search.EmbeddedIndexAction;
import org.sonar.server.search.IndexAction;

import javax.annotation.CheckForNull;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class RuleDao extends BaseDao<RuleMapper, RuleDto, RuleKey>
  implements BatchComponent, ServerComponent {

  public RuleDao(MyBatis mybatis) {
    super(RuleMapper.class, mybatis);
  }

  @Override
  protected String getIndexName() {
    return RuleConstants.INDEX_NAME;
  }

  @Override
  @CheckForNull
  protected RuleDto doGetByKey(RuleKey key, DbSession session) {
    return getMapper(session).selectByKey(key);
  }

  @Override
  protected RuleDto doInsert(RuleDto item, DbSession session) {
    getMapper(session).insert(item);
    return item;
  }

  @Override
  protected RuleDto doUpdate(RuleDto item, DbSession session) {
    getMapper(session).update(item);
    return item;
  }

  @Override
  protected void doDelete(RuleDto item, DbSession session) {
    throw new UnsupportedOperationException("Rules cannot be deleted");
  }

  @Override
  protected void doDeleteByKey(RuleKey key, DbSession session) {
    throw new UnsupportedOperationException("Rules cannot be deleted");
  }

  public List<RuleDto> selectAll() {
    DbSession session = mybatis.openSession(false);
    try {
      return selectAll(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectAll(DbSession session) {
    return getMapper(session).selectAll();
  }

  public List<RuleDto> selectEnablesAndNonManual() {
    DbSession session = mybatis.openSession(false);
    try {
      return selectEnablesAndNonManual(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectEnablesAndNonManual(DbSession session) {
    return getMapper(session).selectEnablesAndNonManual();
  }

  public List<RuleDto> selectNonManual(DbSession session) {
    return getMapper(session).selectNonManual();
  }

  public List<RuleDto> selectBySubCharacteristicId(Integer characteristicOrSubCharacteristicId) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectBySubCharacteristicId(characteristicOrSubCharacteristicId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Return all rules (even the REMOVED ones) linked on to a sub characteristic
   */
  public List<RuleDto> selectBySubCharacteristicId(Integer subCharacteristicId, DbSession session) {
    return getMapper(session).selectBySubCharacteristicId(subCharacteristicId);
  }

  @CheckForNull
  public RuleDto selectById(Integer id, DbSession session) {
    return getMapper(session).selectById(id);
  }

  @CheckForNull
  public RuleDto selectById(Integer id) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleDto selectByKey(RuleKey ruleKey, DbSession session) {
    return getMapper(session).selectByKey(ruleKey);
  }


  @CheckForNull
  public RuleDto selectByKey(RuleKey ruleKey) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByKey(ruleKey, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleDto selectByName(String name) {
    DbSession session = mybatis.openSession(false);
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
        session.enqueue(new DtoIndexAction<RuleDto>(this.getIndexName(),
          IndexAction.Method.INSERT, rule));
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
    DbSession session = mybatis.openSession(false);
    try {
      return selectParameters(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParameters(DbSession session) {
    return getMapper(session).selectAllParams();
  }

  public List<RuleParamDto> selectParametersByRuleId(Integer ruleId) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectParametersByRuleId(ruleId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParametersByRuleId(Integer ruleId, DbSession session) {
    return selectParametersByRuleIds(newArrayList(ruleId));
  }

  public List<RuleParamDto> selectParametersByRuleIds(List<Integer> ruleIds) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectParametersByRuleIds(ruleIds, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParametersByRuleIds(List<Integer> ruleIds, DbSession session) {
    List<RuleParamDto> dtos = newArrayList();
    List<List<Integer>> partitionList = Lists.partition(newArrayList(ruleIds), 1000);
    for (List<Integer> partition : partitionList) {
      dtos.addAll(getMapper(session).selectParamsByRuleIds(partition));
    }
    return dtos;
  }

  public void insert(RuleParamDto param, DbSession session) {
    getMapper(session).insertParameter(param);
    RuleDto dto = this.selectById(param.getRuleId(), session);
    if(dto != null){
      session.enqueue(new EmbeddedIndexAction<RuleKey>(this.getIndexName(),
        IndexAction.Method.INSERT, param,
        dto.getKey()));
    }
  }

  public void insert(RuleParamDto param) {
    DbSession session = mybatis.openSession(false);
    try {
      insert(param, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(RuleParamDto param, DbSession session) {
    getMapper(session).updateParameter(param);
    RuleDto dto = this.selectById(param.getRuleId(), session);
    if(dto != null) {
      session.enqueue(new EmbeddedIndexAction<RuleKey>(this.getIndexName(),
        IndexAction.Method.UPDATE, param,
        dto.getKey()));
    }
  }

  public void update(RuleParamDto param) {
    DbSession session = mybatis.openSession(false);
    try {
      update(param, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleParamDto selectParamByRuleAndKey(Integer ruleId, String key, DbSession session) {
    return getMapper(session).selectParamByRuleAndKey(ruleId, key);
  }

  // ***************************
  // Methods for Rule Tags
  // ***************************

  public void insert(RuleRuleTagDto newTag, DbSession session) {
    System.out.println("newTag = [" + newTag + "], session = [" + session + "]");
    getMapper(session).insertTag(newTag);
    session.enqueue(new EmbeddedIndexAction<RuleKey>(this.getIndexName(),
      IndexAction.Method.INSERT, newTag,
      this.selectById(newTag.getRuleId(), session).getKey()));
  }

  public void deleteParam(RuleParamDto persistedParam, DbSession session) {
    getMapper(session).deleteParameter(persistedParam.getId());
    session.enqueue(new EmbeddedIndexAction<RuleKey>(this.getIndexName(),
      IndexAction.Method.DELETE, persistedParam,
      this.selectById(persistedParam.getRuleId(), session).getKey()));
  }

  public void deleteTag(RuleRuleTagDto tagToDelete, DbSession session) {
    getMapper(session).deleteTag(tagToDelete.getId().intValue());
    session.enqueue(new EmbeddedIndexAction<RuleKey>(this.getIndexName(),
      IndexAction.Method.DELETE, tagToDelete,
      this.selectById(tagToDelete.getRuleId(), session).getKey()));
  }

  public void update(RuleRuleTagDto existingTag, DbSession session) {
    getMapper(session).updateTag(existingTag);
    session.enqueue(new EmbeddedIndexAction<RuleKey>(this.getIndexName(),
      IndexAction.Method.UPDATE, existingTag,
      this.selectById(existingTag.getRuleId(), session).getKey()));
  }

  public List<RuleRuleTagDto> selectTags(DbSession session) {
    return getMapper(session).selectAllTags();
  }

  public List<RuleRuleTagDto> selectTagsByRuleId(Integer ruleId) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectTagsByRuleIds(ruleId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleRuleTagDto> selectTagsByRuleIds(Integer ruleId, DbSession session) {
    return selectTagsByRuleIds(newArrayList(ruleId), session);
  }

  public List<RuleRuleTagDto> selectTagsByRuleIds(List<Integer> ruleIds) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectTagsByRuleIds(ruleIds, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleRuleTagDto> selectTagsByRuleIds(List<Integer> ruleIds, DbSession session) {
    List<RuleRuleTagDto> dtos = newArrayList();
    List<List<Integer>> partitionList = Lists.partition(newArrayList(ruleIds), 1000);
    for (List<Integer> partition : partitionList) {
      dtos.addAll(getMapper(session).selectTagsByRuleIds(partition));
    }
    return dtos;
  }

  @Override
  public Collection<RuleKey> keysOfRowsUpdatedAfter(long timestamp) {
    DbSession session = mybatis.openSession(false);
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
