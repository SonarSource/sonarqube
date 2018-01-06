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
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class DeletePersonAndFileMeasuresTest {
  private static final AtomicInteger GENERATOR = new AtomicInteger();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeletePersonAndFileMeasuresTest.class, "initial.sql");

  @Test
  public void delete_file_and_person_measures() throws SQLException {
    insertComponent("P1", "PRJ", "TRK");
    insertComponent("D1", "DIR", "DIR");
    insertComponent("F1", "FIL", "FIL");
    insertComponent("F2", "FIL", "UTS");
    insertSnapshot("S1", "P1", false);
    insertSnapshot("S2", "P1", true);
    // past measures
    long m1 = insertMeasure("P1", "S1");
    long m2 = insertMeasure("D1", "S1");
    long m3 = insertMeasure("F1", "S1");
    long m4 = insertMeasure("F2", "S1");
    long m5 = insertPersonMeasure("P1", "S1");
    long m6 = insertPersonMeasure("F1", "S1");
    // last measures
    long m7 = insertMeasure("P1", "S2");
    long m8 = insertMeasure("D1", "S2");
    long m9 = insertMeasure("F1", "S2");
    long m10 = insertMeasure("F2", "S2");
    long m11 = insertPersonMeasure("P1", "S2");
    long m12 = insertPersonMeasure("F1", "S2");

    run(false);

    assertThat(db.countRowsOfTable("PROJECTS")).isEqualTo(4);
    assertThat(db.countRowsOfTable("SNAPSHOTS")).isEqualTo(2);
    assertThatMeasuresAreExactly(m1, m2, m7, m8);

    // migration is re-entrant
    run(false);
    assertThat(db.countRowsOfTable("PROJECTS")).isEqualTo(4);
    assertThat(db.countRowsOfTable("SNAPSHOTS")).isEqualTo(2);
    assertThatMeasuresAreExactly(m1, m2, m7, m8);
  }

  @Test
  public void migration_is_disabled_on_sonarcloud() throws SQLException {
    insertComponent("F1", "FIL", "FIL");
    insertSnapshot("S1", "P1", false);
    insertMeasure("F1", "S1");
    insertPersonMeasure("F1", "S1");

    run(true);

    assertThat(db.countRowsOfTable("PROJECTS")).isEqualTo(1);
    assertThat(db.countRowsOfTable("SNAPSHOTS")).isEqualTo(1);
    assertThat(db.countRowsOfTable("PROJECT_MEASURES")).isEqualTo(2);
  }

  private void run(boolean sonarCloud) throws SQLException {
    MapSettings settings = new MapSettings();
    if (sonarCloud) {
      settings.setProperty("sonar.sonarcloud.enabled", true);
    }
    DataChange underTest = new DeletePersonAndFileMeasures(db.database(), settings.asConfig());
    underTest.execute();
  }

  private void assertThatMeasuresAreExactly(long... expectedMeasureIds) {
    long[] ids = db.select("select id as \"id\" from project_measures")
      .stream()
      .mapToLong(m -> (Long) m.get("id"))
      .toArray();
    assertThat(ids).containsOnly(expectedMeasureIds);
  }

  private void insertComponent(String uuid, String scope, String qualifier) {
    db.executeInsert("PROJECTS",
      "ORGANIZATION_UUID", "O1",
      "KEE", "" + GENERATOR.incrementAndGet(),
      "UUID", uuid,
      "PROJECT_UUID", "" + GENERATOR.incrementAndGet(),
      "MAIN_BRANCH_PROJECT_UUID", "" + GENERATOR.incrementAndGet(),
      "UUID_PATH", ".",
      "ROOT_UUID", "" + GENERATOR.incrementAndGet(),
      "PRIVATE", "true",
      "QUALIFIER", qualifier,
      "SCOPE", scope);
  }

  private void insertSnapshot(String uuid, String projectUuid, boolean last) {
    db.executeInsert("SNAPSHOTS",
      "UUID", uuid,
      "COMPONENT_UUID", projectUuid,
      "STATUS", "P",
      "ISLAST", last);
  }

  private long insertMeasure(String componentUuid, String analysisUuid) {
    long id = GENERATOR.incrementAndGet();
    db.executeInsert("PROJECT_MEASURES",
      "ID", id,
      "METRIC_ID", "42",
      "COMPONENT_UUID", componentUuid,
      "ANALYSIS_UUID", analysisUuid);
    return id;
  }

  private long insertPersonMeasure(String componentUuid, String analysisUuid) {
    long id = GENERATOR.incrementAndGet();
    db.executeInsert("PROJECT_MEASURES",
      "ID", id,
      "METRIC_ID", "42",
      "COMPONENT_UUID", componentUuid,
      "ANALYSIS_UUID", analysisUuid,
      "PERSON_ID", RandomUtils.nextInt(100));
    return id;
  }
}
