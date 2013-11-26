/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
      return getMapper(session).selectAll();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectNonManual() {
    SqlSession session = mybatis.openSession();
    try {
      return getMapper(session).selectNonManual();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public RuleDto selectById(Long id) {
    SqlSession session = mybatis.openSession();
    try {
      return getMapper(session).selectById(id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(RuleDto rule) {
    SqlSession session = mybatis.openSession();
    try {
      getMapper(session).update(rule);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private RuleMapper getMapper(SqlSession session) {
    RuleMapper mapper = session.getMapper(RuleMapper.class);
    return mapper;
  }

  public void insert(RuleDto ruleToInsert) {
    SqlSession session = mybatis.openSession();
    try {
      getMapper(session).insert(ruleToInsert);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(Collection<RuleDto> rules) {
    SqlSession session = mybatis.openBatchSession();
    try {
      for (RuleDto rule: rules) {
        getMapper(session).insert(rule);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParameters() {
    SqlSession session = mybatis.openSession();
    try {
      return getMapper(session).selectAllParams();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleParamDto> selectParameters(Long id) {
    SqlSession session = mybatis.openSession();
    try {
      return getMapper(session).selectParamsForRule(id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
