/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateLiveMeasuresTest {

  private System2 system2 = new TestSystem2().setNow(1_500_000_000_000L);

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateLiveMeasuresTest.class, "initial.sql");

  private PopulateLiveMeasures underTest = new PopulateLiveMeasures(db.database(), system2);

  @Test
  public void do_nothing_when_no_data() throws SQLException {
    assertThat(db.countRowsOfTable("PROJECT_MEASURES")).isEqualTo(0);
    underTest.execute();
    assertThat(db.countRowsOfTable("LIVE_MEASURES")).isEqualTo(0);
  }

  @Test
  public void execute_must_update_database() throws SQLException {
    generateProjectMeasures();

    underTest.execute();

    assertThat(getLiveMeasures()).extracting(
      field("COMPONENT_UUID"),
      field("PROJECT_UUID"),
      field("METRIC_ID"),
      field("VALUE"),
      field("TEXT_VALUE"),
      field("VARIATION"),
      field("MEASURE_DATA")).containsExactlyInAnyOrder(generateLiveMeasures());

    assertThat(db.select("select project_uuid as \"PROJECT_UUID\" from live_measures_p"))
      .extracting(t -> t.get("PROJECT_UUID"))
      .containsOnly("PRJ1", "PRJ2");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    generateProjectMeasures();

    underTest.execute();
    underTest.execute();

    assertThat(getLiveMeasures()).extracting(
      field("COMPONENT_UUID"),
      field("PROJECT_UUID"),
      field("METRIC_ID"),
      field("VALUE"),
      field("TEXT_VALUE"),
      field("VARIATION"),
      field("MEASURE_DATA")).containsExactlyInAnyOrder(generateLiveMeasures());
  }

  @Test
  public void do_not_fail_if_live_measure_of_component_already_partially_inserted() throws SQLException {
    generateProjectMeasures();

    db.executeInsert(
      "LIVE_MEASURES",
      "UUID", "foo",
      "COMPONENT_UUID", "PRJ1",
      "PROJECT_UUID", "PRJ1",
      "METRIC_ID", 1010,
      "CREATED_AT", 1L,
      "UPDATED_AT", 1L
    );

    underTest.execute();

  }

  private Function<Map<String, Object>, Object> field(String name) {
    return m -> m.get(name);
  }

  private void generateProjectMeasures() {
    db.executeInsert("PROJECTS",
      "UUID", "PRJ1",
      "PROJECT_UUID", "PRJ1",
      "ORGANIZATION_UUID", "ORG1",
      "UUID_PATH", "X",
      "ROOT_UUID", "X",
      "PRIVATE", "FALSE");
    db.executeInsert("PROJECTS",
      "UUID", "DIR1",
      "PROJECT_UUID", "PRJ1",
      "ORGANIZATION_UUID", "ORG1",
      "UUID_PATH", "X",
      "ROOT_UUID", "X",
      "PRIVATE", "FALSE");
    db.executeInsert("PROJECTS",
      "UUID", "FIL1",
      "PROJECT_UUID", "PRJ1",
      "ORGANIZATION_UUID", "ORG1",
      "UUID_PATH", "X",
      "ROOT_UUID", "X",
      "PRIVATE", "FALSE");
    db.executeInsert("PROJECTS",
      "UUID", "PRJ2",
      "PROJECT_UUID", "PRJ2",
      "ORGANIZATION_UUID", "ORG1",
      "UUID_PATH", "X",
      "ROOT_UUID", "X",
      "PRIVATE", "FALSE");
    db.executeInsert("PROJECTS",
      "UUID", "DIR2",
      "PROJECT_UUID", "PRJ2",
      "ORGANIZATION_UUID", "ORG1",
      "UUID_PATH", "X",
      "ROOT_UUID", "X",
      "PRIVATE", "FALSE");
    db.executeInsert("PROJECTS",
      "UUID", "FIL2",
      "PROJECT_UUID", "PRJ2",
      "ORGANIZATION_UUID", "ORG1",
      "UUID_PATH", "X",
      "ROOT_UUID", "X",
      "PRIVATE", "FALSE");
    db.executeInsert("PROJECTS",
      "UUID", "PRJ3",
      "PROJECT_UUID", "PRJ3",
      "ORGANIZATION_UUID", "ORG1",
      "UUID_PATH", "X",
      "ROOT_UUID", "X",
      "PRIVATE", "FALSE");
    db.executeInsert("PROJECTS",
      "UUID", "PRJ4",
      "PROJECT_UUID", "PRJ4",
      "ORGANIZATION_UUID", "ORG1",
      "UUID_PATH", "X",
      "ROOT_UUID", "X",
      "PRIVATE", "FALSE");

    // non last snapshot, none of its measures should be copied to live_measures
    db.executeInsert("SNAPSHOTS",
      "UUID", "1A1",
      "ISLAST", "FALSE",
      "COMPONENT_UUID", "PRJ1");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "PRJ1",
      "ANALYSIS_UUID", "1A1",
      "METRIC_ID", "100");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "DIR1",
      "ANALYSIS_UUID", "1A1",
      "METRIC_ID", "110",
      "VALUE", "11");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "FIL1",
      "ANALYSIS_UUID", "1A1",
      "METRIC_ID", "120",
      "VALUE", "12");
    db.executeInsert("SNAPSHOTS",
      "UUID", "1A2",
      "ISLAST", "FALSE",
      "COMPONENT_UUID", "PRJ2");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "PRJ2",
      "ANALYSIS_UUID", "1A2",
      "METRIC_ID", "200");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "DIR2",
      "ANALYSIS_UUID", "1A2",
      "METRIC_ID", "210",
      "VALUE", "21");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "FIL2",
      "ANALYSIS_UUID", "1A2",
      "METRIC_ID", "220",
      "VALUE", "22");
    // PRJ3 has only non-last snapshot??!!?? => won't go into live_measures_p
    db.executeInsert("SNAPSHOTS",
      "UUID", "1A3",
      "ISLAST", "FALSE",
      "COMPONENT_UUID", "PRJ3");

    // last snapshot, all measure should be copied to live_measures
    db.executeInsert("SNAPSHOTS",
      "UUID", "2A1",
      "ISLAST", "TRUE",
      "COMPONENT_UUID", "PRJ1");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "PRJ1",
      "ANALYSIS_UUID", "2A1",
      "METRIC_ID", "1010",
      "VALUE", "101",
      "TEXT_VALUE", "TEXT_VALUEx",
      "VARIATION_VALUE_1", "345",
      "MEASURE_DATA", "FFFF");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "PRJ1",
      "ANALYSIS_UUID", "2A1",
      "METRIC_ID", "1020",
      "VALUE", "102");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "DIR1",
      "ANALYSIS_UUID", "2A1",
      "METRIC_ID", "1030",
      "VALUE", "103");
    // FIL1 has no measure for this snapshot => will trigger infinite loop if not taken into account
    db.executeInsert("SNAPSHOTS",
      "UUID", "2A2",
      "ISLAST", "TRUE",
      "COMPONENT_UUID", "PRJ2");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "PRJ2",
      "ANALYSIS_UUID", "2A2",
      "METRIC_ID", "2010",
      "VALUE", "201",
      "TEXT_VALUE", "TEXT_VALUEx",
      "VARIATION_VALUE_1", "345",
      "MEASURE_DATA", "FFFF");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "PRJ2",
      "ANALYSIS_UUID", "2A2",
      "METRIC_ID", "2020",
      "VALUE", "202");
    db.executeInsert("PROJECT_MEASURES",
      "COMPONENT_UUID", "DIR2",
      "ANALYSIS_UUID", "2A2",
      "METRIC_ID", "2030",
      "VALUE", "203");
    // FIL2 has no measure for this snapshot => will trigger infinite loop if not taken into account
    // PRJ5 has last snapshot without measure => won't go into live_measures_p
    db.executeInsert("SNAPSHOTS",
      "UUID", "2A4",
      "ISLAST", "FALSE",
      "COMPONENT_UUID", "PRJ4");
  }

  private List<Map<String, Object>> getLiveMeasures() {
    return db.select("SELECT * FROM LIVE_MEASURES");
  }

  private Tuple[] generateLiveMeasures() {
    return new Tuple[] {
      tuple("PRJ1", "PRJ1", 1010L, 101.0, "TEXT_VALUEx", 345.0, new byte[] {-1, -1}),
      tuple("PRJ1", "PRJ1", 1020L, 102.0, null, null, null),
      tuple("DIR1", "PRJ1", 1030L, 103.0, null, null, null),
      tuple("PRJ2", "PRJ2", 2010L, 201.0, "TEXT_VALUEx", 345.0, new byte[] {-1, -1}),
      tuple("PRJ2", "PRJ2", 2020L, 202.0, null, null, null),
      tuple("DIR2", "PRJ2", 2030L, 203.0, null, null, null)
    };
  }
}
