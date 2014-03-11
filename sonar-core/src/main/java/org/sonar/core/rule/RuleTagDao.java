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
import org.sonar.api.ServerExtension;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

public class RuleTagDao implements ServerExtension {

  private final MyBatis myBatis;

  public RuleTagDao(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  public List<RuleTagDto> selectAll() {
    SqlSession session = myBatis.openSession();
    try {
      return selectAll(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleTagDto> selectAll(SqlSession session) {
    return getMapper(session).selectAll();
  }

  public void insert(RuleTagDto newRuleTag) {
    SqlSession session = myBatis.openSession();
    try {
      insert(newRuleTag, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(RuleTagDto newRuleTag, SqlSession session) {
    getMapper(session).insert(newRuleTag);
  }

  public void delete(Long tagId) {
    SqlSession session = myBatis.openSession();
    try {
      delete(tagId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(long tagId, SqlSession session) {
    getMapper(session).delete(tagId);
  }

  public Long selectId(String tag) {
    SqlSession session = myBatis.openSession();
    try {
      return selectId(tag, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Long selectId(String tag, SqlSession session) {
    return getMapper(session).selectId(tag);
  }

  public List<RuleTagDto> selectUnused() {
    SqlSession session = myBatis.openSession();
    try {
      return selectUnused(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleTagDto> selectUnused(SqlSession session) {
    return getMapper(session).selectUnused();
  }

  private RuleTagMapper getMapper(SqlSession session) {
    return session.getMapper(RuleTagMapper.class);
  }
}
