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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.measure.MeasureTesting.newMeasure;

class MeasureDaoTest {

  @RegisterExtension
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final MeasureDao underTest = db.getDbClient().measureDao();

  @Test
  void insert_measure() {
    MeasureDto dto = newMeasure();
    int count = underTest.insert(db.getSession(), dto);
    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);
  }

  @Test
  void update_measure() {
    MeasureDto dto = newMeasure();
    underTest.insert(db.getSession(), dto);

    dto.addValue("metric1", "value1");
    dto.computeJsonValueHash();
    int count = underTest.update(db.getSession(), dto);

    assertThat(count).isEqualTo(1);
    verifyTableSize(1);
    verifyPersisted(dto);
  }

  @Test
  void select_measure() {
    MeasureDto measure1 = newMeasure();
    MeasureDto measure2 = newMeasure();
    underTest.insert(db.getSession(), measure1);
    underTest.insert(db.getSession(), measure2);

    assertThat(underTest.selectMeasure(db.getSession(), measure1.getComponentUuid()))
      .hasValueSatisfying(selected -> assertThat(selected).usingRecursiveComparison().isEqualTo(measure1));
    assertThat(underTest.selectMeasure(db.getSession(), "unknown-component")).isEmpty();
  }

  @Test
  void select_branch_measure_hashes() {
    MeasureDto measure1 = new MeasureDto()
      .setComponentUuid("c1")
      .setBranchUuid("b1")
      .addValue("metric1", "value1");
    MeasureDto measure2 = new MeasureDto()
      .setComponentUuid("c2")
      .setBranchUuid("b1")
      .addValue("metric2", "value2");
    MeasureDto measure3 = new MeasureDto()
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
      .containsOnly(new MeasureHash("c1", hash1), new MeasureHash("c2", hash2));
  }

  private void verifyTableSize(int expectedSize) {
    assertThat(db.countRowsOfTable(db.getSession(), "measures")).isEqualTo(expectedSize);
  }

  private void verifyPersisted(MeasureDto dto) {
    assertThat(underTest.selectMeasure(db.getSession(), dto.getComponentUuid())).hasValueSatisfying(selected -> {
      assertThat(selected).usingRecursiveComparison().isEqualTo(dto);
    });
  }
}
