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

package org.sonar.server.db.migrations.debt;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.AbstractListHandler;
import org.picocontainer.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.util.SqlUtil;

import javax.annotation.CheckForNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Used in the Active Record Migration 516
 */
public class DevelopmentCostMeasuresMigration implements DatabaseMigration {

  private static final Logger logger = LoggerFactory.getLogger(DevelopmentCostMeasuresMigration.class);

  private static final String ID = "id";
  private static final String VALUE = "value";

  private static final String FAILURE_MESSAGE = "Fail to migrate data";

  private static final String SQL_SELECT = "SELECT pm.id FROM project_measures pm INNER JOIN metrics m on m.id=pm.metric_id " +
    "WHERE (m.name='development_cost')";

  private static final String SQL_UPDATE = "UPDATE project_measures SET value=NULL,text_value=? WHERE id=?";

  private static final String SQL_SELECT_ALL;

  static {
    StringBuilder sb = new StringBuilder("SELECT pm.id AS " + ID + ", pm.value AS " + VALUE +
      " FROM project_measures pm " +
      " WHERE ");
    for (int i = 0; i < Referentials.GROUP_SIZE; i++) {
      if (i > 0) {
        sb.append(" OR ");
      }
      sb.append("pm.id=?");
    }
    SQL_SELECT_ALL = sb.toString();
  }

  private final DebtConvertor debtConvertor;
  private final Database db;

  public DevelopmentCostMeasuresMigration(Database database, Settings settings) {
    this.db = database;
    this.debtConvertor = new DebtConvertor(settings);
  }

  @Override
  public void execute() {
    try {
      logger.info("Initialize input");
      Referentials referentials = new Referentials(db, SQL_SELECT);
      if (referentials.size() > 0) {
        logger.info("Migrate {} rows", referentials.size());
        convert(referentials);
      }
    } catch (SQLException e) {
      logger.error(FAILURE_MESSAGE, e);
      SqlUtil.log(logger, e);
      throw MessageException.of(FAILURE_MESSAGE);

    } catch (Exception e) {
      logger.error(FAILURE_MESSAGE, e);
      throw MessageException.of(FAILURE_MESSAGE);
    }
  }

  public Object convert(Referentials referentials) throws SQLException {
    Long[] ids = referentials.pollGroupOfIds();
    while (ids.length > 0) {
      List<Map<String, Object>> rows = selectRows(ids);
      convert(rows, ids);

      ids = referentials.pollGroupOfIds();
    }
    return null;
  }

  private List<Map<String, Object>> selectRows(Long[] ids) throws SQLException {
    Connection readConnection = null;
    try {
      readConnection = db.getDataSource().getConnection();
      RowHandler rowHandler = new RowHandler();
      return new QueryRunner().query(readConnection, SQL_SELECT_ALL, rowHandler, ids);

    } finally {
      DbUtils.closeQuietly(readConnection);
    }
  }

  private void convert(List<Map<String, Object>> rows, Long[] ids) throws SQLException {
    Connection readConnection = null;
    Connection writeConnection = null;
    try {
      readConnection = db.getDataSource().getConnection();
      writeConnection = db.getDataSource().getConnection();
      writeConnection.setAutoCommit(false);

      List<Object[]> allParams = Lists.newArrayList();
      QueryRunner runner = new QueryRunner();
      for (Map<String, Object> row : rows) {
        Object[] params = new Object[2];
        params[0] = convertDebtForDays((Double) row.get(VALUE));
        params[1] = row.get(ID);
        allParams.add(params);
      }
      runner.batch(writeConnection, SQL_UPDATE, allParams.toArray(new Object[allParams.size()][]));
      writeConnection.commit();

    } finally {
      DbUtils.closeQuietly(readConnection);
      DbUtils.closeQuietly(writeConnection);
    }
  }

  @CheckForNull
  private String convertDebtForDays(@Nullable Double data) {
    if (data == null) {
      return null;
    }
    return Long.toString(debtConvertor.createFromDays(data));
  }

  private static class RowHandler extends AbstractListHandler<Map<String, Object>> {
    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
      Map<String, Object> map = Maps.newHashMap();
      map.put(ID, SqlUtil.getLong(rs, ID));
      map.put(VALUE, SqlUtil.getDouble(rs, VALUE));
      return map;
    }
  }

}
