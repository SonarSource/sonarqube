/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.measure;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class MeasureFilterExecutor implements ServerComponent {

  private MyBatis mybatis;
  private Database database;
  private ResourceDao resourceDao;

  public MeasureFilterExecutor(MyBatis mybatis, Database database, ResourceDao resourceDao) {
    this.mybatis = mybatis;
    this.database = database;
    this.resourceDao = resourceDao;
  }

  public List<MeasureFilterRow> execute(MeasureFilter filter, MeasureFilterContext context) throws SQLException {
    List<MeasureFilterRow> rows;
    SqlSession session = null;
    Connection connection = null;
    try {
      session = mybatis.openSession();
      prepareContext(context, filter, session);

      if (isValid(filter, context)) {
        MeasureFilterSql sql = new MeasureFilterSql(database, filter, context);
        context.setSql(sql.sql());
        connection = session.getConnection();
        rows = sql.execute(connection);
      } else {
        rows = Collections.emptyList();
      }
    } finally {
      MyBatis.closeQuietly(session);
      // connection is supposed to be closed by the session
      closeQuietly(connection);
    }

    return rows;
  }

  private void closeQuietly(@Nullable Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        LoggerFactory.getLogger(MeasureFilterExecutor.class).warn("Fail to close connection", e);
        // ignore
      }
    }
  }


  private void prepareContext(MeasureFilterContext context, MeasureFilter filter, SqlSession session) {
    if (filter.getBaseResourceKey() != null) {
      context.setBaseSnapshot(resourceDao.getLastSnapshot(filter.getBaseResourceKey(), session));
    }
  }

  static boolean isValid(MeasureFilter filter, MeasureFilterContext context) {
    boolean valid =
      !(filter.isOnBaseResourceChildren() && context.getBaseSnapshot() == null) &&
        !(filter.isOnFavourites() && context.getUserId() == null);
    for (MeasureFilterCondition condition : filter.getMeasureConditions()) {
      if (condition.period() != null && condition.period() < 1) {
        valid = false;
      }
      if (condition.metric() == null) {
        valid = false;
      }
    }
    if (filter.sort().getPeriod() != null && filter.sort().getPeriod() < 1) {
      valid = false;
    }
    if (filter.sort().onMeasures() && filter.sort().metric() == null) {
      valid = false;
    }
    return valid;
  }
}
