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

package org.sonar.db.version.v53;

import org.junit.Test;
import org.sonar.db.Database;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.version.DdlChange.Context;

public class FixMsSqlCollationTest {

  Database db = mock(Database.class);
  Context context = mock(Context.class);

  FixMsSqlCollation underTest = new FixMsSqlCollation(db);

  @Test
  public void execute_sql_on_mssql() throws Exception {
    when(db.getDialect()).thenReturn(new MsSql());

    underTest.execute(context);

    verify(context, times(42)).execute(anyListOf(String.class));
  }

  @Test
  public void nothing_to_do_on_mysql() throws Exception {
    when(db.getDialect()).thenReturn(new MySql());

    underTest.execute(context);

    verifyZeroInteractions(context);
  }

  @Test
  public void nothing_to_do_on_h2() throws Exception {
    when(db.getDialect()).thenReturn(new H2());

    underTest.execute(context);

    verifyZeroInteractions(context);
  }

  @Test
  public void nothing_to_do_on_oracle() throws Exception {
    when(db.getDialect()).thenReturn(new Oracle());

    underTest.execute(context);

    verifyZeroInteractions(context);
  }

  @Test
  public void update_collation() throws Exception {
    useMssql();
    new FixMsSqlCollation.UpdateTableCollation(context, db, "rules")
      .addVarcharColumn("plugin_rule_key", 200)
      .execute();

    verify(context).execute(singletonList(
      "ALTER TABLE rules ALTER COLUMN plugin_rule_key NVARCHAR (200) COLLATE Latin1_General_CS_AS"
      ));
  }

  @Test
  public void update_collation_with_not_nullable_column() throws Exception {
    useMssql();
    new FixMsSqlCollation.UpdateTableCollation(context, db, "rules")
      .addNotNullableVarcharColumn("plugin_rule_key", 200)
      .execute();

    verify(context).execute(singletonList(
      "ALTER TABLE rules ALTER COLUMN plugin_rule_key NVARCHAR (200) COLLATE Latin1_General_CS_AS NOT NULL"
      ));
  }

  @Test
  public void update_collation_with_text_column() throws Exception {
    useMssql();
    new FixMsSqlCollation.UpdateTableCollation(context, db, "rules")
      .addClobColumn("description")
      .execute();

    verify(context).execute(singletonList(
      "ALTER TABLE rules ALTER COLUMN description NVARCHAR (MAX) COLLATE Latin1_General_CS_AS"
      ));
  }

  @Test
  public void update_collation_remove_and_recreate_index() throws Exception {
    useMssql();
    new FixMsSqlCollation.UpdateTableCollation(context, db, "rules")
      .addIndex("rules_repo_key", "plugin_name", "plugin_rule_key")
      .addVarcharColumn("plugin_rule_key", 200)
      .execute();

    verify(context).execute("DROP INDEX rules_repo_key ON rules");
    verify(context).execute("CREATE INDEX rules_repo_key ON rules(plugin_name,plugin_rule_key)");
  }

  @Test
  public void update_collation_recreate_unique_index() throws Exception {
    useMssql();
    new FixMsSqlCollation.UpdateTableCollation(context, db, "rules")
      .addUniqueIndex("rules_repo_key", "plugin_name", "plugin_rule_key")
      .addVarcharColumn("plugin_rule_key", 200)
      .execute();

    verify(context).execute("DROP INDEX rules_repo_key ON rules");
    verify(context).execute("CREATE UNIQUE INDEX rules_repo_key ON rules(plugin_name,plugin_rule_key)");
  }

  private void useMssql() {
    when(db.getDialect()).thenReturn(new MsSql());
  }
}
