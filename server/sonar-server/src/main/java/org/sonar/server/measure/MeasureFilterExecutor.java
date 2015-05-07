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
package org.sonar.server.measure;

import com.google.common.base.Strings;
import org.apache.commons.dbutils.DbUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@ServerSide
public class MeasureFilterExecutor {

  private MyBatis mybatis;
  private Database database;
  private ResourceDao resourceDao;

  public MeasureFilterExecutor(MyBatis mybatis, Database database, ResourceDao resourceDao) {
    this.mybatis = mybatis;
    this.database = database;
    this.resourceDao = resourceDao;
  }

  public List<MeasureFilterRow> execute(MeasureFilter filter, MeasureFilterContext context) throws SQLException {
    if (filter.isEmpty()) {
      return Collections.emptyList();
    }

    List<MeasureFilterRow> rows;
    SqlSession session = null;
    Connection connection = null;
    try {
      session = mybatis.openSession(false);
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
      DbUtils.closeQuietly(connection);
    }

    return rows;
  }

  private void prepareContext(MeasureFilterContext context, MeasureFilter filter, SqlSession session) {
    if (filter.getBaseResourceKey() != null) {
      context.setBaseSnapshot(resourceDao.getLastSnapshot(filter.getBaseResourceKey(), session));
    }
  }

  static boolean isValid(MeasureFilter filter, MeasureFilterContext context) {
    boolean valid = Strings.isNullOrEmpty(filter.getBaseResourceKey()) || context.getBaseSnapshot() != null;
    valid &= !(filter.isOnBaseResourceChildren() && context.getBaseSnapshot() == null);
    valid &= !(filter.isOnFavourites() && context.getUserId() == null);
    valid &= validateMeasureConditions(filter);
    valid &= validateSort(filter);
    return valid;
  }

  private static boolean validateMeasureConditions(MeasureFilter filter) {
    boolean valid = true;
    for (MeasureFilterCondition condition : filter.getMeasureConditions()) {
      if (condition.period() != null && condition.period() < 1) {
        valid = false;
      }
      if (condition.metric() == null) {
        valid = false;
      }
    }
    return valid;
  }

  private static boolean validateSort(MeasureFilter filter) {
    boolean valid = true;
    if (filter.sort().period() != null && filter.sort().period() < 1) {
      valid = false;
    }
    if (filter.sort().onMeasures() && filter.sort().metric() == null) {
      valid = false;
    }
    return valid;
  }
}
