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
 * Used in the Active Record Migration 515
 */
public class TechnicalDebtMeasuresMigration implements DatabaseMigration {

  private static final Logger logger = LoggerFactory.getLogger(TechnicalDebtMeasuresMigration.class);

  private static final String ID = "id";
  private static final String VALUE = "value";
  private static final String VAR1 = "var1";
  private static final String VAR2 = "var2";
  private static final String VAR3 = "var3";
  private static final String VAR4 = "var4";
  private static final String VAR5 = "var5";

  private static final String FAILURE_MESSAGE = "Fail to migrate data";

  private static final String SQL_SELECT = "SELECT * FROM project_measures INNER JOIN metrics on metrics.id=project_measures.metric_id " +
    "WHERE (metrics.name='sqale_index' or metrics.name='new_technical_debt' " +
    // SQALE measures
    "or metrics.name='sqale_effort_to_grade_a' or metrics.name='sqale_effort_to_grade_b' or metrics.name='sqale_effort_to_grade_c' or metrics.name='sqale_effort_to_grade_d' " +
    "or metrics.name='blocker_remediation_cost' or metrics.name='critical_remediation_cost' or metrics.name='major_remediation_cost' or metrics.name='minor_remediation_cost' " +
    "or metrics.name='info_remediation_cost' " +
    ")";

  private static final String SQL_UPDATE = "UPDATE project_measures SET value=?," +
    "variation_value_1=?,variation_value_2=?,variation_value_3=?,variation_value_4=?,variation_value_5=? WHERE id=?";

  private static final String SQL_SELECT_ALL;

  static {
    StringBuilder sb = new StringBuilder("SELECT pm.id AS " + ID + ", pm.value AS " + VALUE +
      ", pm.variation_value_1 AS " + VAR1 + ", pm.variation_value_2 AS " + VAR2 + ", pm.variation_value_3 AS " + VAR3 +
      ", pm.variation_value_4 AS " + VAR4 + ", pm.variation_value_5 AS " + VAR5 +
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

  public TechnicalDebtMeasuresMigration(Database database, Settings settings) {
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
        Object[] params = new Object[7];
        params[0] = convertDebtForDays((Double) row.get(VALUE));
        params[1] = convertDebtForDays((Double) row.get(VAR1));
        params[2] = convertDebtForDays((Double) row.get(VAR2));
        params[3] = convertDebtForDays((Double) row.get(VAR3));
        params[4] = convertDebtForDays((Double) row.get(VAR4));
        params[5] = convertDebtForDays((Double) row.get(VAR5));
        params[6] = row.get(ID);
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
  private Long convertDebtForDays(@Nullable Double data) {
    if (data == null) {
      return null;
    }
    return debtConvertor.createFromDays(data);
  }

  private static class RowHandler extends AbstractListHandler<Map<String, Object>> {
    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
      Map<String, Object> map = Maps.newHashMap();
      map.put(ID, SqlUtil.getLong(rs, ID));
      map.put(VALUE, SqlUtil.getDouble(rs, VALUE));
      map.put(VAR1, SqlUtil.getDouble(rs, VAR1));
      map.put(VAR2, SqlUtil.getDouble(rs, VAR2));
      map.put(VAR3, SqlUtil.getDouble(rs, VAR3));
      map.put(VAR4, SqlUtil.getDouble(rs, VAR4));
      map.put(VAR5, SqlUtil.getDouble(rs, VAR5));
      return map;
    }
  }

}
