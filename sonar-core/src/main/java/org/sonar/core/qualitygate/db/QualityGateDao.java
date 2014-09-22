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
import org.sonar.core.persistence.MyBatis;

import java.util.Collection;
import java.util.Date;

/**
 * @since 4.3
 */
public class QualityGateDao {

  private final MyBatis myBatis;

  public QualityGateDao(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  public void insert(QualityGateDto newQualityGate) {
    SqlSession session = myBatis.openSession(false);
    try {
      insert(newQualityGate, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(QualityGateDto newQualityGate, SqlSession session) {
    getMapper(session).insert(newQualityGate.setCreatedAt(new Date()));
  }

  public Collection<QualityGateDto> selectAll() {
    SqlSession session = myBatis.openSession(false);
    try {
      return selectAll(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<QualityGateDto> selectAll(SqlSession session) {
    return getMapper(session).selectAll();
  }

  public QualityGateDto selectByName(String name) {
    SqlSession session = myBatis.openSession(false);
    try {
      return selectByName(name, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityGateDto selectByName(String name, SqlSession session) {
    return getMapper(session).selectByName(name);
  }

  public QualityGateDto selectById(long id) {
    SqlSession session = myBatis.openSession(false);
    try {
      return selectById(id, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QualityGateDto selectById(long id, SqlSession session) {
    return getMapper(session).selectById(id);
  }

  public void delete(QualityGateDto qGate) {
    SqlSession session = myBatis.openSession(false);
    try {
      delete(qGate, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(QualityGateDto qGate, SqlSession session) {
    getMapper(session).delete(qGate.getId());
  }

  public void update(QualityGateDto qGate) {
    SqlSession session = myBatis.openSession(false);
    try {
      update(qGate, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(QualityGateDto qGate, SqlSession session) {
    getMapper(session).update(qGate.setUpdatedAt(new Date()));
  }

  private QualityGateMapper getMapper(SqlSession session) {
    return session.getMapper(QualityGateMapper.class);
  }
}
