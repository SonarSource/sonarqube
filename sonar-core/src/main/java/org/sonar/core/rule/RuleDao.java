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
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

/**
 * @deprecated in 4.4 moved to org.sonar.server.rule.db.RuleDao.
 */
@Deprecated
@BatchSide
@ServerSide
public class RuleDao {

  private MyBatis mybatis;

  public RuleDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public List<RuleDto> selectEnablesAndNonManual() {
    SqlSession session = mybatis.openSession();
    try {
      return selectEnablesAndNonManual(session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<RuleDto> selectEnablesAndNonManual(SqlSession session) {
    return getMapper(session).selectEnablesAndNonManual();
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

  private RuleMapper getMapper(SqlSession session) {
    return session.getMapper(RuleMapper.class);
  }
}
