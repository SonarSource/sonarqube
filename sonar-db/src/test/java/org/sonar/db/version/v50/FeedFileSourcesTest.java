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

package org.sonar.db.version.v50;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import org.apache.commons.dbutils.DbUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedFileSourcesTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, FeedFileSourcesTest.class, "schema.sql");

  private static final long NOW = 1414770242000L;

  FeedFileSources migration;

  System2 system;

  @Before
  public void setUp() {
    db.executeUpdateSql("truncate table metrics");
    db.executeUpdateSql("truncate table snapshots");
    db.executeUpdateSql("truncate table snapshot_sources");
    db.executeUpdateSql("truncate table projects");
    db.executeUpdateSql("truncate table project_measures");
    db.executeUpdateSql("truncate table file_sources");

    system = mock(System2.class);
    when(system.now()).thenReturn(NOW);
    migration = new FeedFileSources(db.database(), system);
  }

  @Test
  public void migrate_empty_db() throws Exception {
    migration.execute();
  }

  @Test
  public void migrate_sources_with_no_scm_no_coverage() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    db.executeUpdateSql("insert into snapshot_sources " +
      "(snapshot_id, data, updated_at) " +
      "values " +
      "(6, 'class Foo {\r\n  // Empty\r\n}\r\n', '2014-10-31 16:44:02.000')");

    db.executeUpdateSql("insert into snapshot_sources " +
      "(snapshot_id, data, updated_at) " +
      "values " +
      "(7, '', '2014-10-31 16:44:02.000')");

    migration.execute();

    List<Map<String, Object>> results = db.select("select project_uuid as \"projectUuid\", file_uuid as \"fileUuid\", created_at as \"createdAt\", " +
      "updated_at as \"updatedAt\", data as \"data\", data as \"data\", line_hashes as \"lineHashes\", data_hash as \"dataHash\"  from file_sources");
    assertThat(results).hasSize(2);

    assertThat(results.get(0).get("projectUuid")).isEqualTo("uuid-MyProject");
    assertThat(results.get(0).get("fileUuid")).isEqualTo("uuid-Migrated.xoo");
    assertThat(results.get(0).get("data")).isEqualTo("");
    assertThat(results.get(0).get("lineHashes")).isEqualTo("");
    assertThat(results.get(0).get("dataHash")).isEqualTo("");
    assertThat(results.get(0).get("updatedAt")).isEqualTo(NOW);
    assertThat(results.get(0).get("createdAt")).isEqualTo(1416238020000L);

    assertThat(results.get(1).get("projectUuid")).isEqualTo("uuid-MyProject");
    assertThat(results.get(1).get("fileUuid")).isEqualTo("uuid-MyFile.xoo");
    assertThat(results.get(1).get("data")).isEqualTo(",,,,,,,,,,,,,,,class Foo {\r\n,,,,,,,,,,,,,,,  // Empty\r\n,,,,,,,,,,,,,,,}\r\n,,,,,,,,,,,,,,,\r\n");
    assertThat(results.get(1).get("lineHashes")).isEqualTo("6a19ce786467960a3a9b0d26383a464a\naab2dbc5fdeaa80b050b1d049ede357c\ncbb184dd8e05c9709e5dcaedaa0495cf\n\n");
    assertThat(results.get(1).get("dataHash")).isEqualTo("");
    assertThat(formatLongDate((long) results.get(1).get("updatedAt")).toString()).startsWith("2014-10-31");
    assertThat(results.get(1).get("createdAt")).isEqualTo(NOW);
  }

  @Test
  public void migrate_sources_with_scm_and_coverage_in_text_value() throws Exception {
    migrate_sources_with_scm_and_coverage_in("text_value");
  }

  @Test
  public void migrate_sources_with_scm_and_coverage_in_measure_data() throws Exception {
    migrate_sources_with_scm_and_coverage_in("measure_data");
  }

  private void migrate_sources_with_scm_and_coverage_in(String columnName) throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    Connection connection = null;
    try {
      connection = db.openConnection();

      connection.prepareStatement("insert into snapshot_sources " +
        "(snapshot_id, data, updated_at) " +
        "values " +
        "(6, 'class Foo {\r\n  // Empty\r\n}\r\n', '2014-10-31 16:44:02.000')")
        .executeUpdate();

      db.executeUpdateSql("insert into snapshot_sources " +
        "(snapshot_id, data, updated_at) " +
        "values " +
        "(7, '', '2014-10-31 16:44:02.000')");

      PreparedStatement revisionStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(1, 6, ?)");
      revisionStmt.setBytes(1, "1=aef12a;2=abe465;3=afb789;4=afb789".getBytes(StandardCharsets.UTF_8));
      revisionStmt.executeUpdate();

      PreparedStatement authorStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(2, 6, ?)");
      authorStmt.setBytes(1, "1=alice;2=bob;3=carol;4=carol".getBytes(StandardCharsets.UTF_8));
      authorStmt.executeUpdate();

      PreparedStatement dateStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(3, 6, ?)");
      dateStmt.setBytes(1, "1=2014-04-25T12:34:56+0100;2=2014-07-25T12:34:56+0100;3=2014-03-23T12:34:56+0100;4=2014-03-23T12:34:56+0100".getBytes(StandardCharsets.UTF_8));
      dateStmt.executeUpdate();

      PreparedStatement utHitsStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(4, 6, ?)");
      utHitsStmt.setBytes(1, "1=1;3=0".getBytes(StandardCharsets.UTF_8));
      utHitsStmt.executeUpdate();

      PreparedStatement utCondStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(5, 6, ?)");
      utCondStmt.setBytes(1, "1=4".getBytes(StandardCharsets.UTF_8));
      utCondStmt.executeUpdate();

      PreparedStatement utCoveredCondStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(6, 6, ?)");
      utCoveredCondStmt.setBytes(1, "1=2".getBytes(StandardCharsets.UTF_8));
      utCoveredCondStmt.executeUpdate();

      PreparedStatement itHitsStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(7, 6, ?)");
      itHitsStmt.setBytes(1, "1=2;3=0".getBytes(StandardCharsets.UTF_8));
      itHitsStmt.executeUpdate();

      PreparedStatement itCondStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(8, 6, ?)");
      itCondStmt.setBytes(1, "1=5".getBytes(StandardCharsets.UTF_8));
      itCondStmt.executeUpdate();

      PreparedStatement itCoveredCondStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(9, 6, ?)");
      itCoveredCondStmt.setBytes(1, "1=3".getBytes(StandardCharsets.UTF_8));
      itCoveredCondStmt.executeUpdate();

      PreparedStatement overallHitsStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(10, 6, ?)");
      overallHitsStmt.setBytes(1, "1=3;3=0".getBytes(StandardCharsets.UTF_8));
      overallHitsStmt.executeUpdate();

      PreparedStatement overallCondStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(11, 6, ?)");
      overallCondStmt.setBytes(1, "1=6".getBytes(StandardCharsets.UTF_8));
      overallCondStmt.executeUpdate();

      PreparedStatement overallCoveredCondStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(12, 6, ?)");
      overallCoveredCondStmt.setBytes(1, "1=4".getBytes(StandardCharsets.UTF_8));
      overallCoveredCondStmt.executeUpdate();

      PreparedStatement duplicationDataStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, " + columnName + ") " +
        "values " +
        "(13, 6, ?)");
      duplicationDataStmt
        .setBytes(
          1,
          "<duplications><g><b s=\"1\" l=\"1\" r=\"MyProject:src/main/xoo/prj/MyFile.xoo\"/><b s=\"2\" l=\"1\" r=\"MyProject:src/main/xoo/prj/MyFile.xoo\"/><b s=\"3\" l=\"1\" r=\"MyProject:src/main/xoo/prj/AnotherFile.xoo\"/></g></duplications>"
            .getBytes(StandardCharsets.UTF_8));
      duplicationDataStmt.executeUpdate();
    } finally {
      DbUtils.commitAndCloseQuietly(connection);
    }

    migration.execute();

    List<Map<String, Object>> results = db.select("select project_uuid as \"projectUuid\", file_uuid as \"fileUuid\", created_at as \"createdAt\", " +
      "updated_at as \"updatedAt\", data as \"data\", data as \"data\", line_hashes as \"lineHashes\", data_hash as \"dataHash\"  from file_sources");
    assertThat(results).hasSize(2);

    assertThat(results.get(0).get("projectUuid")).isEqualTo("uuid-MyProject");
    assertThat(results.get(0).get("fileUuid")).isEqualTo("uuid-Migrated.xoo");
    assertThat(results.get(0).get("data")).isEqualTo("");
    assertThat(results.get(0).get("lineHashes")).isEqualTo("");
    assertThat(results.get(0).get("dataHash")).isEqualTo("");
    assertThat(results.get(0).get("updatedAt")).isEqualTo(NOW);
    assertThat(results.get(0).get("createdAt")).isEqualTo(1416238020000L);

    assertThat(results.get(1).get("projectUuid")).isEqualTo("uuid-MyProject");
    assertThat(results.get(1).get("fileUuid")).isEqualTo("uuid-MyFile.xoo");
    assertThat(results.get(1).get("data")).isEqualTo(
      "aef12a,alice,2014-04-25T12:34:56+0100,1,4,2,2,5,3,3,6,4,,,1,class Foo {\r\nabe465,bob,2014-07-25T12:34:56+0100,,,,,,,,,,,,2,  " +
        "// Empty\r\nafb789,carol,2014-03-23T12:34:56+0100,0,,,0,,,0,,,,,,}\r\nafb789,carol,2014-03-23T12:34:56+0100,,,,,,,,,,,,,\r\n");
    assertThat(results.get(1).get("lineHashes")).isEqualTo("6a19ce786467960a3a9b0d26383a464a\naab2dbc5fdeaa80b050b1d049ede357c\ncbb184dd8e05c9709e5dcaedaa0495cf\n\n");
    assertThat(results.get(1).get("dataHash")).isEqualTo("");
    assertThat(formatLongDate((long) results.get(1).get("updatedAt")).toString()).startsWith("2014-10-31");
    assertThat(results.get(1).get("createdAt")).isEqualTo(NOW);
  }

  @Test
  public void migrate_sources_with_invalid_duplication() throws Exception {
    db.prepareDbUnit(getClass(), "before.xml");

    Connection connection = null;
    try {
      connection = db.openConnection();

      connection.prepareStatement("insert into snapshot_sources " +
        "(snapshot_id, data, updated_at) " +
        "values " +
        "(6, 'class Foo {\r\n  // Empty\r\n}\r\n', '2014-10-31 16:44:02.000')")
        .executeUpdate();

      db.executeUpdateSql("insert into snapshot_sources " +
        "(snapshot_id, data, updated_at) " +
        "values " +
        "(7, '', '2014-10-31 16:44:02.000')");

      PreparedStatement duplicationDataStmt = connection.prepareStatement("insert into project_measures " +
        "(metric_id, snapshot_id, text_value) " +
        "values " +
        "(13, 6, ?)");
      duplicationDataStmt
        .setBytes(
          1,
          "<duplications><g><b s=\"1\" l=\"1\" r=\"MyProject:src/main/xoo/prj/MyFile.xoo\"/><b s=\"2\" l=\"1\" r=\"MyProject:src/main/xoo/prj/MyFile.xoo\"/><b s=\"3\" l=\"1\" r=\"MyProject:src/main/xoo/prj/AnotherFile.xoo\"/"
            .getBytes(StandardCharsets.UTF_8));
      duplicationDataStmt.executeUpdate();
    } finally {
      DbUtils.commitAndCloseQuietly(connection);
    }

    migration.execute();

    // db.assertDbUnit(getClass(), "after-with-invalid-duplication.xml", "file_sources");

    List<Map<String, Object>> results = db.select("select project_uuid as \"projectUuid\", file_uuid as \"fileUuid\", created_at as \"createdAt\", " +
      "updated_at as \"updatedAt\", data as \"data\", data as \"data\", line_hashes as \"lineHashes\", data_hash as \"dataHash\"  from file_sources");
    assertThat(results).hasSize(2);

    assertThat(results.get(0).get("projectUuid")).isEqualTo("uuid-MyProject");
    assertThat(results.get(0).get("fileUuid")).isEqualTo("uuid-Migrated.xoo");
    assertThat(results.get(0).get("data")).isEqualTo("");
    assertThat(results.get(0).get("lineHashes")).isEqualTo("");
    assertThat(results.get(0).get("dataHash")).isEqualTo("");
    assertThat(results.get(0).get("updatedAt")).isEqualTo(NOW);
    assertThat(results.get(0).get("createdAt")).isEqualTo(1416238020000L);

    assertThat(results.get(1).get("projectUuid")).isEqualTo("uuid-MyProject");
    assertThat(results.get(1).get("fileUuid")).isEqualTo("uuid-MyFile.xoo");
    assertThat(results.get(1).get("data")).isEqualTo(",,,,,,,,,,,,,,,class Foo {\r\n,,,,,,,,,,,,,,,  // Empty\r\n,,,,,,,,,,,,,,,}\r\n,,,,,,,,,,,,,,,\r\n");
    assertThat(results.get(1).get("lineHashes")).isEqualTo("6a19ce786467960a3a9b0d26383a464a\naab2dbc5fdeaa80b050b1d049ede357c\ncbb184dd8e05c9709e5dcaedaa0495cf\n\n");
    assertThat(results.get(1).get("dataHash")).isEqualTo("");
    assertThat(formatLongDate((long) results.get(1).get("updatedAt")).toString()).startsWith("2014-10-31");
    assertThat(results.get(1).get("createdAt")).isEqualTo(NOW);
  }

  private String formatLongDate(long dateInMs) {
    return DateUtils.formatDateTime(DateUtils.longToDate(dateInMs));
  }
}
