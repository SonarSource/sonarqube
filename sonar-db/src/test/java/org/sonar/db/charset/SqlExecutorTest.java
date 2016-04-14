/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.charset;

import com.google.common.collect.ImmutableMap;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlExecutorTest {

  SqlExecutor underTest = new SqlExecutor();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Test
  public void executeSelect_executes_PreparedStatement() throws Exception {
    dbTester.executeInsert("users", ImmutableMap.of("login", "login1", "name", "name one"));
    dbTester.executeInsert("users", ImmutableMap.of("login", "login2", "name", "name two"));

    dbTester.commit();

    try (Connection connection = dbTester.openConnection()) {
      List<String[]> users = underTest.executeSelect(connection, "select login, name from users order by id", new SqlExecutor.StringsConverter(2));
      assertThat(users).hasSize(2);
      assertThat(users.get(0)[0]).isEqualTo("login1");
      assertThat(users.get(0)[1]).isEqualTo("name one");
      assertThat(users.get(1)[0]).isEqualTo("login2");
      assertThat(users.get(1)[1]).isEqualTo("name two");
    }
  }

  @Test
  public void executeUpdate_executes_PreparedStatement() throws Exception {
    dbTester.executeInsert("users", ImmutableMap.of("login", "the_login", "name", "the name"));
    dbTester.commit();

    try (Connection connection = dbTester.openConnection()) {
      underTest.executeUpdate(connection, "update users set name='new name' where login='the_login'");
      connection.commit();
    }
    Map<String, Object> row = dbTester.selectFirst("select name from users where login='the_login'");
    assertThat(row.get("NAME")).isEqualTo("new name");
  }

}
