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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;

public class MeasureFilterExecutor implements ServerComponent {
  private static final Logger FILTER_LOG = LoggerFactory.getLogger("org.sonar.MEASURE_FILTER");

  private MyBatis mybatis;
  private Database database;
  private ResourceDao resourceDao;

  public MeasureFilterExecutor(MyBatis mybatis, Database database, ResourceDao resourceDao) {
    this.mybatis = mybatis;
    this.database = database;
    this.resourceDao = resourceDao;
  }

  public List<MeasureFilterRow> execute(MeasureFilter filter, @Nullable Long userId) {
    List<MeasureFilterRow> rows;
    SqlSession session = null;
    try {
      session = mybatis.openSession();
      MesasureFilterContext context = prepareContext(filter, userId, session);

      if (isValid(filter, context)) {
        MeasureFilterSql sql = new MeasureFilterSql(database, filter, context);
        Connection connection = session.getConnection();
        rows = sql.execute(connection);
      } else {
        rows = Collections.emptyList();
      }

    } catch (Exception e) {
      throw new IllegalStateException(e);

    } finally {
      MyBatis.closeQuietly(session);
    }

    return rows;
  }

  private MesasureFilterContext prepareContext(MeasureFilter filter, Long userId, SqlSession session) {
    MesasureFilterContext context = new MesasureFilterContext();
    context.setUserId(userId);
    if (filter.baseResourceKey() != null) {
      context.setBaseSnapshot(resourceDao.getLastSnapshot(filter.baseResourceKey(), session));
    }
    return context;
  }

  static boolean isValid(MeasureFilter filter, MesasureFilterContext context) {
    return
      !(filter.resourceQualifiers().isEmpty() && !filter.userFavourites()) &&
      !(filter.isOnBaseResourceChildren() && context.getBaseSnapshot() == null) &&
      !(filter.userFavourites() && context.getUserId() == null);
  }
}
