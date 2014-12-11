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
package org.sonar.server.user.index;

import com.google.common.collect.Maps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.ResultSetIterator;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Scrolls over table USERS and reads documents to populate the user index
 */
class UserResultSetIterator extends ResultSetIterator<UserDoc> {

  private static final String[] FIELDS = {
    // column 1
    "u.login",
    "u.name",
    "u.email",
    "u.active",
    "u.scm_accounts",
    "u.created_at",
    "u.updated_at",
  };

  private static final String SQL_ALL = "select " + StringUtils.join(FIELDS, ",") + " from users u ";

  private static final String SQL_AFTER_DATE = SQL_ALL + " where u.updated_at>?";

  static UserResultSetIterator create(DbClient dbClient, Connection connection, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      PreparedStatement stmt = dbClient.newScrollingSelectStatement(connection, sql);
      if (afterDate > 0L) {
        stmt.setLong(1, afterDate);
      }
      return new UserResultSetIterator(stmt);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all users", e);
    }
  }

  private UserResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  @Override
  protected UserDoc read(ResultSet rs) throws SQLException {
    UserDoc doc = new UserDoc(Maps.<String, Object>newHashMapWithExpectedSize(30));

    String login = rs.getString(1);

    // all the keys must be present, even if value is null
    doc.setLogin(login);
    doc.setName(rs.getString(2));
    doc.setEmail(rs.getString(3));
    doc.setActive(rs.getBoolean(4));
    doc.setScmAccounts(getScmAccounts(rs.getString(5), login));
    doc.setCreatedAt(rs.getLong(6));
    doc.setUpdatedAt(rs.getLong(7));
    return doc;
  }

  private List<String> getScmAccounts(@Nullable String csv, String login) {
    List<String> result = newArrayList();
    if (csv == null) {
      return result;
    }
    CSVParser csvParser = null;
    StringReader reader = null;
    try {
      reader = new StringReader(csv);
      csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
      for (CSVRecord csvRecord : csvParser) {
        for (Iterator<String> iter = csvRecord.iterator(); iter.hasNext();) {
          result.add(iter.next());
        }
      }
      return result;
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to read scm accounts for user '%s'", login));
    } finally {
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(csvParser);
    }
  }
}
