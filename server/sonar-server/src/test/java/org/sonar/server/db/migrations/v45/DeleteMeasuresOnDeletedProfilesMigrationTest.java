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

package org.sonar.server.db.migrations.v45;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.MigrationStep;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteMeasuresOnDeletedProfilesMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(DeleteMeasuresOnDeletedProfilesMigrationTest.class, "schema.sql");

  MigrationStep migration;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = new DbClient(db.database(), db.myBatis());
    migration = new DeleteMeasuresOnDeletedProfilesMigrationStep(dbClient);
  }

  @Test
  public void delete_measures_with_no_json_data() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    migration.execute();

    Connection connection = db.openConnection();
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("select id from project_measures");
    try {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).isEqualTo(2);
      assertThat(rs.next()).isFalse();
    } finally {
      rs.close();
      stmt.close();
      connection.close();
    }
  }
}
