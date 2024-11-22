/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.measure;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.platform.db.migration.adhoc.AddMeasuresMigratedColumnToPortfoliosTable;
import org.sonar.server.platform.db.migration.adhoc.AddMeasuresMigratedColumnToProjectBranchesTable;
import org.sonar.server.platform.db.migration.adhoc.CreateIndexOnPortfoliosMeasuresMigrated;
import org.sonar.server.platform.db.migration.adhoc.CreateIndexOnProjectBranchesMeasuresMigrated;
import org.sonar.server.platform.db.migration.adhoc.CreateMeasuresTable;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.measure.MeasureTesting.newJsonMeasure;

public class JsonMeasureDaoTest {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final JsonMeasureDao underTest = db.getDbClient().jsonMeasureDao();

  @Before
  public void setUp() throws Exception {
    new CreateMeasuresTable(db.getDbClient().getDatabase()).execute();
    new AddMeasuresMigratedColumnToProjectBranchesTable(db.getDbClient().getDatabase()).execute();
    new AddMeasuresMigratedColumnToPortfoliosTable(db.getDbClient().getDatabase()).execute();
    new CreateIndexOnProjectBranchesMeasuresMigrated(db.getDbClient().getDatabase()).execute();
    new CreateIndexOnPortfoliosMeasuresMigrated(db.getDbClient().getDatabase()).execute();
  }

  @Test
  public void insert_measure() {
    JsonMeasureDto dto = newJsonMeasure();
    int count = underTest.insert(db.getSession(), dto);
    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);
  }

  @Test
  public void update_measure() {
    JsonMeasureDto dto = newJsonMeasure();
    underTest.insert(db.getSession(), dto);

    dto.addValue("metric1", "value1");
    dto.computeJsonValueHash();
    int count = underTest.update(db.getSession(), dto);

    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);
  }

  @Test
  public void insertOrUpdate_inserts_or_updates_measure() {
    // insert
    JsonMeasureDto dto = newJsonMeasure();
    int count = underTest.insertOrUpdate(db.getSession(), dto);
    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);

    // update
    String key = dto.getMetricValues().keySet().stream().findFirst().orElseThrow();
    dto.addValue(key, 10d);
    count = underTest.insertOrUpdate(db.getSession(), dto);
    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);
  }

  @Test
  public void insertOrUpdate_merges_measures() {
    // insert
    Double value2 = 10d;
    JsonMeasureDto dto = newJsonMeasure();
    dto.getMetricValues().clear();
    dto.addValue("key1", 11d)
      .addValue("key2", value2);
    int count = underTest.insert(db.getSession(), dto);
    verifyPersisted(dto);
    verifyTableSize(1);
    assertThat(count).isEqualTo(1);

    // update key1 value, remove key2 (must not disappear from DB) and add key3
    Double value1 = 12d;
    Double value3 = 13d;
    dto.addValue("key1", value1)
      .addValue("key3", value3)
      .getMetricValues().remove("key2");
    count = underTest.insertOrUpdate(db.getSession(), dto);
    assertThat(count).isEqualTo(1);
    verifyTableSize(1);

    assertThat(underTest.selectByComponentUuid(db.getSession(), dto.getComponentUuid()))
      .hasValueSatisfying(selected -> {
        assertThat(selected.getComponentUuid()).isEqualTo(dto.getComponentUuid());
        assertThat(selected.getBranchUuid()).isEqualTo(dto.getBranchUuid());
        assertThat(selected.getMetricValues()).contains(
          entry("key1", value1),
          entry("key2", value2),
          entry("key3", value3));
        assertThat(selected.getJsonValueHash()).isEqualTo(dto.computeJsonValueHash());
      });
  }

  @Test
  public void select_measure() {
    JsonMeasureDto measure1 = newJsonMeasure();
    JsonMeasureDto measure2 = newJsonMeasure();
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    assertThat(underTest.selectByComponentUuid(db.getSession(), measure1.getComponentUuid()))
      .hasValueSatisfying(selected -> assertThat(selected).usingRecursiveComparison().isEqualTo(measure1));
    assertThat(underTest.selectByComponentUuid(db.getSession(), "unknown-component")).isEmpty();
  }

  @Test
  public void select_branch_measure_hashes() {
    JsonMeasureDto measure1 = new JsonMeasureDto()
      .setComponentUuid("c1")
      .setBranchUuid("b1")
      .addValue("metric1", "value1");
    JsonMeasureDto measure2 = new JsonMeasureDto()
      .setComponentUuid("c2")
      .setBranchUuid("b1")
      .addValue("metric2", "value2");
    JsonMeasureDto measure3 = new JsonMeasureDto()
      .setComponentUuid("c3")
      .setBranchUuid("b3")
      .addValue("metric3", "value3");
    long hash1 = measure1.computeJsonValueHash();
    long hash2 = measure2.computeJsonValueHash();
    measure3.computeJsonValueHash();

    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);
    underTest.insert(db.getSession(), measure3);

    assertThat(underTest.selectBranchMeasureHashes(db.getSession(), "b1"))
      .containsOnly(new JsonMeasureHash("c1", hash1), new JsonMeasureHash("c2", hash2));
  }

  private void verifyTableSize(int expectedSize) {
    assertThat(db.countRowsOfTable(db.getSession(), "measures")).isEqualTo(expectedSize);
  }

  private void verifyPersisted(JsonMeasureDto dto) {
    assertThat(underTest.selectByComponentUuid(db.getSession(), dto.getComponentUuid())).hasValueSatisfying(selected -> {
      assertThat(selected).usingRecursiveComparison().isEqualTo(dto);
    });
  }
}
