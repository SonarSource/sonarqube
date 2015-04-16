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

package org.sonar.server.db.migrations.v44;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

public class ConvertProfileMeasuresMigrationTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(ConvertProfileMeasuresMigrationTest.class, "schema.sql");

  ConvertProfileMeasuresMigrationStep migration;

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = new DbClient(db.database(), db.myBatis());
    migration = new ConvertProfileMeasuresMigrationStep(dbClient);
  }

  @Test
  public void generate_profiles_measure_as_json() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    migration.execute();

    Connection connection = db.openConnection();
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("select text_value from project_measures where id=2");
    try {
      rs.next();
      // pb of comparison of timezones..., so using startsWith instead of equals
      assertThat(rs.getString(1)).startsWith("[{\"key\":\"java-sonar-way\",\"language\":\"java\",\"name\":\"Sonar way\",\"rulesUpdatedAt\":\"2014-01-04T");
    } finally {
      rs.close();
      stmt.close();
      connection.close();
    }
  }

  /**
   * http://jira.codehaus.org/browse/SONAR-5515
   * Version of quality profile was introduced in SQ 2.9. Migration must not fail
   * when there are still some projects which last analysis was done with SQ <= 2.8.
   */
  @Test
  public void missing_profile_version() throws Exception {
    db.prepareDbUnit(getClass(), "missing_profile_version.xml");

    migration.execute();

    Connection connection = db.openConnection();
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("select text_value from project_measures where id=2");
    try {
      rs.next();
      // pb of comparison of timezones..., so using startsWith instead of equals
      assertThat(rs.getString(1)).startsWith("[{\"key\":\"java-sonar-way\",\"language\":\"java\",\"name\":\"Sonar way\",\"rulesUpdatedAt\":");
    } finally {
      rs.close();
      stmt.close();
      connection.close();
    }
  }

  /**
   * http://jira.codehaus.org/browse/SONAR-5580
   */
  @Test
  public void delete_measure_when_profile_does_not_exist() throws Exception {
    db.prepareDbUnit(getClass(), "measure_on_deleted_profile.xml");

    migration.execute();

    Connection connection = db.openConnection();
    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery("select * from project_measures where id=2");
    try {
      // measure is deleted
      assertThat(rs.next()).isFalse();
    } finally {
      rs.close();
      stmt.close();
      connection.close();
    }
  }
}
