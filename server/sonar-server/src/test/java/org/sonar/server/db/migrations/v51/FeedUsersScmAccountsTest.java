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

package org.sonar.server.db.migrations.v51;

import org.apache.commons.dbutils.DbUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.migrations.DatabaseMigration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedUsersScmAccountsTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(FeedUsersScmAccountsTest.class, "schema.sql");

  @Test
  public void execute() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    System2 system = mock(System2.class);
    when(system.now()).thenReturn(3000000000000L);
    DatabaseMigration migration = new FeedUsersScmAccounts(db.database(), system);
    migration.execute();

    // Cannot use db.assertDbUnit with users table as there's a conflict with the h2 users table
    assertThat(db.countSql("select count(*) from users where updated_at='3000000000000'")).isEqualTo(3);
    assertThat(getScmAccountsFromUsers(1)).contains("user1,user1@mail.com");
    assertThat(getScmAccountsFromUsers(3)).contains("user3");
    assertThat(getScmAccountsFromUsers(4)).contains("user4,\"user4,@mail.com\"");
  }

  private String getScmAccountsFromUsers(int id) throws SQLException {
    PreparedStatement pstmt = db.openConnection().prepareStatement("select scm_accounts from users where id = ?");
    pstmt.setInt(1, id);
    ResultSet rs = pstmt.executeQuery();
    try {
      while (rs.next()) {
        return rs.getString(1);
      }
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(pstmt);
    }
    return null;
  }

}
