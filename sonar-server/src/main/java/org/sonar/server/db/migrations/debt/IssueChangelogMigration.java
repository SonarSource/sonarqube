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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.AbstractListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.util.SqlUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used in the Active Record Migration 514
 */
public class IssueChangelogMigration implements DatabaseMigration {

  private Logger logger = LoggerFactory.getLogger(IssueChangelogMigration.class);

  private static final String ID = "id";
  private static final String CHANGE_DATA = "changeData";

  private static final String FAILURE_MESSAGE = "Fail to migrate data";

  private static final String SQL_SELECT = "SELECT * FROM issue_changes WHERE change_type = 'diff' and change_data LIKE '%technicalDebt%'";
  private static final String SQL_UPDATE = "UPDATE issue_changes SET change_data=?,updated_at=? WHERE id=?";

  static final String SQL_SELECT_ISSUES;

  static {
    StringBuilder sb = new StringBuilder("SELECT c.id AS "+ ID +", c.change_data AS " + CHANGE_DATA +
      " FROM issue_changes c " +
      " WHERE ");
    for (int i = 0; i < Referentials.GROUP_SIZE; i++) {
      if (i > 0) {
        sb.append(" OR ");
      }
      sb.append("c.id=?");
    }
    SQL_SELECT_ISSUES = sb.toString();
  }

  private final DebtConvertor debtConvertor;
  private final System2 system2;
  private final Database db;

  public IssueChangelogMigration(Database database, Settings settings) {
    this(database, settings, System2.INSTANCE);
  }

  @VisibleForTesting
  IssueChangelogMigration(Database database, Settings settings, System2 system2) {
    this.db = database;
    this.debtConvertor = new DebtConvertor(settings);
    this.system2 = system2;
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

  public Object convert(Referentials referentials) throws Exception {
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
      return new QueryRunner().query(readConnection, SQL_SELECT_ISSUES, rowHandler, ids);

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
        Object[] params = new Object[3];
        params[0] = convertChangelog((String) row.get(CHANGE_DATA));
        params[1] = new Date(system2.now());
        params[2] = row.get(ID);
        allParams.add(params);
      }
      runner.batch(writeConnection, SQL_UPDATE, allParams.toArray(new Object[allParams.size()][]));
      writeConnection.commit();

    } finally {
      DbUtils.closeQuietly(readConnection);
      DbUtils.closeQuietly(writeConnection);
    }
  }

  @VisibleForTesting
  String convertChangelog(String data){
    Pattern pattern = Pattern.compile("technicalDebt=(\\d*)\\|(\\d*)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(data);
    StringBuffer sb = new StringBuffer();
    if (matcher.find()) {
      String replacement = "technicalDebt=";
      String oldValue = matcher.group(1);
      if (!Strings.isNullOrEmpty(oldValue)) {
        long oldDebt = debtConvertor.createFromLong(Long.parseLong(oldValue));
        replacement += Long.toString(oldDebt);
      }
      replacement +=  "|";
      String newValue = matcher.group(2);
      if (!Strings.isNullOrEmpty(newValue)) {
        long newDebt = debtConvertor.createFromLong(Long.parseLong(newValue));
        replacement += Long.toString(newDebt);
      }
      matcher.appendReplacement(sb, replacement);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private static class RowHandler extends AbstractListHandler<Map<String, Object>> {
    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
      Map<String, Object> map = Maps.newHashMap();
      map.put(ID, SqlUtil.getLong(rs, ID));
      map.put(CHANGE_DATA, rs.getString(CHANGE_DATA));
      return map;
    }
  }

}
