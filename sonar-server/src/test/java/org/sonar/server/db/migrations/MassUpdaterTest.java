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

package org.sonar.server.db.migrations;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.core.persistence.dialect.Dialect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MassUpdaterTest {

  @ClassRule
  public static TestDatabase db = new TestDatabase().schema(MassUpdaterTest.class, "schema.sql");

  static class Row {
    private Long id;
  }

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_data.xml");

    new MassUpdater(db.database()).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return "SELECT i.id FROM issues i";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getLong(rs, 1);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return "UPDATE issues SET severity=? WHERE id=?";
        }

        @Override
        public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
          updateStatement.setString(1, "MAJOR");
          updateStatement.setLong(2, row.id);
          return true;
        }
      }
      );

    db.assertDbUnit(getClass(), "migrate_data_result.xml", "issues");
  }

  @Test
  public void fail_on_bad_sql_request() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_data.xml");

    try {
      new MassUpdater(db.database()).execute(
        new MassUpdater.InputLoader<Row>() {
          @Override
          public String selectSql() {
            return "<INVALID QUERY>";
          }

          @Override
          public Row load(ResultSet rs) throws SQLException {
            return new Row();
          }
        },
        new MassUpdater.InputConverter<Row>() {
          @Override
          public String updateSql() {
            return "<INVALID QUERY>";
          }

          @Override
          public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
            return true;
          }
        }
        );
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  public void fail_on_unknown_error() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_data.xml");

    try {
      new MassUpdater(db.database()).execute(
        new MassUpdater.InputLoader<Row>() {
          @Override
          public String selectSql() {
            return "SELECT i.id FROM issues i";
          }

          @Override
          public Row load(ResultSet rs) throws SQLException {
            Row row = new Row();
            row.id = SqlUtil.getLong(rs, 1);
            return row;
          }
        },
        new MassUpdater.InputConverter<Row>() {
          @Override
          public String updateSql() {
            throw new RuntimeException("Unknown error");
          }

          @Override
          public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
            return true;
          }
        }
        );
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  public void ignore_null_rows() throws Exception {
    db.prepareDbUnit(getClass(), "ignore_null_rows.xml");

    new MassUpdater(db.database()).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return "SELECT i.id FROM issues i";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          return null;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return "UPDATE issues SET severity=? WHERE id=?";
        }

        @Override
        public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
          updateStatement.setString(1, "BLOCKER");
          updateStatement.setLong(2, row.id);
          return true;
        }
      }
      );
    // no changes, do not set severity to BLOCKER
    db.assertDbUnit(getClass(), "ignore_null_rows.xml", "issues");
  }

  @Test
  public void convert_select_sql() throws Exception {
    Database db = mock(Database.class);

    Dialect dialect = mock(Dialect.class);
    when(dialect.getTrueSqlValue()).thenReturn("true");
    when(dialect.getFalseSqlValue()).thenReturn("false");

    when(db.getDialect()).thenReturn(dialect);

    String result = MassUpdater.convertSelectSql("SELECT * FROM projects WHERE enabled=${_true} AND used=${_true} AND deleted=${_false}", db);
    assertThat(result).isEqualTo("SELECT * FROM projects WHERE enabled=true AND used=true AND deleted=false");
  }
}
