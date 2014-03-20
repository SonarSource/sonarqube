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
package org.sonar.core.rule;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;

public class RuleDao implements BatchComponent, ServerComponent {

  private MyBatis mybatis;

  public RuleDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<RuleDto> selectAll() {
    SqlSession session = mybatis.openSession();
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
    SqlSession session = mybatis.openSession();
    try {
      return getMapper(session).selectEnablesAndNonManual();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectNonManual(SqlSession session) {
    return getMapper(session).selectNonManual();
  }

  public List<RuleDto> selectByCharacteristicOrSubCharacteristicId(Integer characteristicOrSubCharacteristicId) {
    SqlSession session = mybatis.openSession();
    try {
      return selectByCharacteristicOrSubCharacteristicId(characteristicOrSubCharacteristicId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectByCharacteristicOrSubCharacteristicId(Integer characteristicOrSubCharacteristicId, SqlSession session) {
    return getMapper(session).selectByCharacteristicOrSubCharacteristicId(characteristicOrSubCharacteristicId);
  }

  public List<RuleDto> selectOverridingDebt(List<String> repositories) {
    SqlSession session = mybatis.openSession();
    try {
      return selectOverridingDebt(repositories, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectOverridingDebt(List<String> repositories, SqlSession session) {
    return getMapper(session).selectOverridingDebt(repositories);
  }

  @CheckForNull
  public RuleDto selectById(Integer id, SqlSession session) {
    return getMapper(session).selectById(id);
  }

  @CheckForNull
  public RuleDto selectById(Integer id) {
    SqlSession session = mybatis.openSession();
    try {
      return selectById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public RuleDto selectByName(String name) {
    SqlSession session = mybatis.openSession();
    try {
      return getMapper(session).selectByName(name);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(RuleDto rule, SqlSession session) {
    getMapper(session).update(rule);
  }

  public void update(RuleDto rule) {
    SqlSession session = mybatis.openSession();
    try {
      update(rule, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(RuleDto ruleToInsert, SqlSession session) {
    getMapper(session).insert(ruleToInsert);
  }

  public void insert(RuleDto ruleToInsert) {
    SqlSession session = mybatis.openSession();
    try {
      insert(ruleToInsert, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(Collection<RuleDto> rules) {
    SqlSession session = mybatis.openBatchSession();
    try {
      for (RuleDto rule : rules) {
        getMapper(session).batchInsert(rule);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParameters() {
    SqlSession session = mybatis.openSession();
    try {
      return selectParameters(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParameters(SqlSession session) {
    return getMapper(session).selectAllParams();
  }

  public List<RuleParamDto> selectParameters(Integer id) {
    SqlSession session = mybatis.openSession();
    try {
      return selectParameters(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParameters(Integer ruleId, SqlSession session) {
    return getMapper(session).selectParamsForRule(ruleId);
  }

  public void insert(RuleParamDto param, SqlSession session) {
    getMapper(session).insertParameter(param);
  }

  public void insert(RuleParamDto param) {
    SqlSession session = mybatis.openSession();
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
    SqlSession session = mybatis.openSession();
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

  public List<RuleRuleTagDto> selectTags(SqlSession session) {
    return getMapper(session).selectAllTags();
  }

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

  public List<RuleRuleTagDto> selectTags(Integer id) {
    SqlSession session = mybatis.openSession();
    try {
      return selectTags(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleRuleTagDto> selectTags(Integer id, SqlSession session) {
    return getMapper(session).selectTagsForRule(id);
  }
}
