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
package org.sonar.core.qualitygate.db;

import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;
import java.util.Date;

/**
 * @since 4.3
 */
public class QualityGateConditionDao implements DaoComponent {

  private final MyBatis myBatis;

  public QualityGateConditionDao(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  public void insert(QualityGateConditionDto newQualityGate) {
    SqlSession session = myBatis.openSession(false);
    try {
      insert(newQualityGate, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(QualityGateConditionDto newQualityGate, SqlSession session) {
    getMapper(session).insert(newQualityGate.setCreatedAt(new Date()));
  }

  public Collection<QualityGateConditionDto> selectForQualityGate(long qGateId) {
    SqlSession session = myBatis.openSession(false);
    try {
      return selectForQualityGate(qGateId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<QualityGateConditionDto> selectForQualityGate(long qGateId, SqlSession session) {
    return getMapper(session).selectForQualityGate(qGateId);
  }

  public QualityGateConditionDto selectById(long id) {
    SqlSession session = myBatis.openSession(false);
    try {
      return selectById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityGateConditionDto selectById(long id, SqlSession session) {
    return getMapper(session).selectById(id);
  }

  public void delete(QualityGateConditionDto qGate) {
    SqlSession session = myBatis.openSession(false);
    try {
      delete(qGate, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(QualityGateConditionDto qGate, SqlSession session) {
    getMapper(session).delete(qGate.getId());
  }

  public void update(QualityGateConditionDto qGate) {
    SqlSession session = myBatis.openSession(false);
    try {
      update(qGate, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(QualityGateConditionDto qGate, SqlSession session) {
    getMapper(session).update(qGate.setUpdatedAt(new Date()));
  }

  public void deleteConditionsWithInvalidMetrics() {
    SqlSession session = myBatis.openSession(false);
    try {
      deleteConditionsWithInvalidMetrics(session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteConditionsWithInvalidMetrics(SqlSession session) {
    getMapper(session).deleteConditionsWithInvalidMetrics();
  }

  private QualityGateConditionMapper getMapper(SqlSession session) {
    return session.getMapper(QualityGateConditionMapper.class);
  }
}
