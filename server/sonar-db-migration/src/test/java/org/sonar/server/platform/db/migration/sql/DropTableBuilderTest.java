/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.db.migration.sql;

import org.junit.Test;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DropTableBuilderTest {
  @Test
  public void drop_tables_on_postgresql() {
    assertThat(new DropTableBuilder(new PostgreSql(), "issues")
      .build()).containsOnly("drop table if exists issues");
  }

  @Test
  public void drop_tables_on_mssql() {
    assertThat(new DropTableBuilder(new MsSql(), "issues")
      .build()).containsOnly("drop table issues");
  }

  @Test
  public void drop_tables_on_h2() {
    assertThat(new DropTableBuilder(new H2(), "issues")
      .build()).containsOnly("drop table if exists issues");
  }

  @Test
  public void drop_columns_on_oracle() {
    assertThat(new DropTableBuilder(new Oracle(), "issues")
      .build()).containsExactly("""
        BEGIN
        EXECUTE IMMEDIATE 'DROP SEQUENCE issues_seq';
        EXCEPTION
        WHEN OTHERS THEN
          IF SQLCODE != -2289 THEN
          RAISE;
          END IF;
        END;""", """
        BEGIN
        EXECUTE IMMEDIATE 'DROP TRIGGER issues_idt';
        EXCEPTION
        WHEN OTHERS THEN
          IF SQLCODE != -4080 THEN
          RAISE;
          END IF;
        END;""", """
        BEGIN
        EXECUTE IMMEDIATE 'DROP TABLE issues';
        EXCEPTION
        WHEN OTHERS THEN
          IF SQLCODE != -942 THEN
          RAISE;
          END IF;
        END;""");
  }

  @Test
  public void fail_when_dialect_is_null() {
    assertThatThrownBy(() -> new DropTableBuilder(null, "issues"))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void fail_when_table_is_null() {
    assertThatThrownBy(() -> new DropTableBuilder(new PostgreSql(), null))
      .isInstanceOf(NullPointerException.class);
  }
}
