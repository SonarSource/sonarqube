/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.HashMap;
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
      field("MEASURE_DATA")
    ).containsExactlyInAnyOrder(generateLiveMeasures());
  }

  private Function<Map<String, Object>, Object> field(String name) {
    return m -> m.get(name);
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
      field("MEASURE_DATA")
    ).containsExactlyInAnyOrder(generateLiveMeasures());
  }

  private void generateProjectMeasures() {
    Map<String, Object> project = new HashMap<>();
    project.put("UUID", "PRJ1");
    project.put("ORGANIZATION_UUID", "ORG1");
    project.put("UUID_PATH", "X");
    project.put("ROOT_UUID", "X");
    project.put("PROJECT_UUID", "PRJ1");
    project.put("PRIVATE", "FALSE");
    db.executeInsert("PROJECTS", project);

    Map<String, Object> analysis1 = new HashMap<>();
    analysis1.put("UUID", "A1");
    analysis1.put("ISLAST", "FALSE");
    analysis1.put("COMPONENT_UUID", "PRJ1");
    db.executeInsert("SNAPSHOTS", analysis1);

    Map<String, Object> analysis2 = new HashMap<>();
    analysis2.put("UUID", "A2");
    analysis2.put("ISLAST", "TRUE");
    analysis2.put("COMPONENT_UUID", "PRJ1");
    db.executeInsert("SNAPSHOTS", analysis2);

    Map<String, Object> measure1 = new HashMap<>();
    measure1.put("COMPONENT_UUID", "PRJ1");
    measure1.put("ANALYSIS_UUID", "A1");
    measure1.put("METRIC_ID", "123");
    db.executeInsert("PROJECT_MEASURES", measure1);

    Map<String, Object> measure2 = new HashMap<>();
    measure2.put("COMPONENT_UUID", "PRJ1");
    measure2.put("ANALYSIS_UUID", "A2");
    measure2.put("METRIC_ID", "123");
    measure2.put("VALUE", "234");
    measure2.put("TEXT_VALUE", "TEXT_VALUEx");
    measure2.put("VARIATION_VALUE_1", "345");
    measure2.put("MEASURE_DATA", "FFFF");
    db.executeInsert("PROJECT_MEASURES", measure2);

    // measures with person_id not null are purged later
    // by another migration
    Map<String, Object> personMeasure = new HashMap<>();
    personMeasure.put("COMPONENT_UUID", "PRJ1");
    personMeasure.put("ANALYSIS_UUID", "A2");
    personMeasure.put("METRIC_ID", "200");
    personMeasure.put("VALUE", "234");
    personMeasure.put("TEXT_VALUE", "TEXT_VALUEx");
    personMeasure.put("VARIATION_VALUE_1", "345");
    personMeasure.put("MEASURE_DATA", "FFFF");
    personMeasure.put("PERSON_ID", "99");
    db.executeInsert("PROJECT_MEASURES", personMeasure);
  }

  private List<Map<String, Object>> getLiveMeasures() {
    return db.select("SELECT * FROM LIVE_MEASURES");
  }

  private Tuple[] generateLiveMeasures() {
    return new Tuple[] {
      tuple("PRJ1", "PRJ1", 123L, 234.0, "TEXT_VALUEx", 345.0, new byte[] {-1, -1})
    };
  }
}
