/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class QualityGateDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private QualityGateDbTester qualityGateDbTester = new QualityGateDbTester(db);
  private DbSession dbSession = db.getSession();
  private QualityGateDao underTest = db.getDbClient().qualityGateDao();

  @Test
  public void testInsert() {
    QualityGateDto newQgate = new QualityGateDto().setName("My Quality Gate");

    underTest.insert(dbSession, newQgate);
    dbSession.commit();

    assertThat(underTest.selectAll(dbSession)).extracting(QualityGateDto::getName, QualityGateDto::isBuiltIn)
      .containsExactlyInAnyOrder(
        // TODO : tuple("Sonar way", true),
        tuple("My Quality Gate", false));
    assertThat(newQgate.getId()).isNotNull();
  }

  @Test
  public void insert_built_in() {
    underTest.insert(db.getSession(), new QualityGateDto().setName("test").setBuiltIn(true));

    QualityGateDto reloaded = underTest.selectByName(db.getSession(), "test");

    assertThat(reloaded.isBuiltIn()).isTrue();
  }

  @Test
  public void testSelectAll() {
    insertQualityGates();

    assertThat(underTest.selectAll(dbSession)).extracting(QualityGateDto::getName, QualityGateDto::isBuiltIn)
      .containsExactlyInAnyOrder(
        // TODO : tuple("Sonar Way", true),
        tuple("Balanced", false),
        tuple("Lenient", false),
        tuple("Very strict", false));
  }

  @Test
  public void testSelectByName() {
    insertQualityGates();
    assertThat(underTest.selectByName(dbSession, "Balanced").getName()).isEqualTo("Balanced");
    assertThat(underTest.selectByName(dbSession, "Unknown")).isNull();
  }

  @Test
  public void testSelectById() {
    insertQualityGates();
    assertThat(underTest.selectById(dbSession, underTest.selectByName(dbSession, "Very strict").getId()).getName()).isEqualTo("Very strict");
    assertThat(underTest.selectById(dbSession, 42L)).isNull();
  }

  @Test
  public void testDelete() {
    insertQualityGates();

    underTest.delete(underTest.selectByName(dbSession, "Very strict"), dbSession);
    dbSession.commit();

    assertThat(underTest.selectAll(dbSession)).extracting(QualityGateDto::getName, QualityGateDto::isBuiltIn)
      .containsExactlyInAnyOrder(
        // TODO : tuple("Sonar Way", true),
        tuple("Balanced", false),
        tuple("Lenient", false));
  }

  @Test
  public void testUpdate() {
    insertQualityGates();

    underTest.update(underTest.selectByName(dbSession, "Very strict").setName("Not so strict"), dbSession);
    dbSession.commit();

    assertThat(underTest.selectAll(dbSession)).extracting(QualityGateDto::getName, QualityGateDto::isBuiltIn)
      .containsExactlyInAnyOrder(
        // TODO :  tuple("Sonar Way", true),
        tuple("Balanced", false),
        tuple("Lenient", false),
        tuple("Not so strict", false));
  }

  private void insertQualityGates() {
    qualityGateDbTester.insertQualityGate(g -> g.setName("Very strict").setBuiltIn(false));
    qualityGateDbTester.insertQualityGate(g -> g.setName("Balanced").setBuiltIn(false));
    qualityGateDbTester.insertQualityGate(g -> g.setName("Lenient").setBuiltIn(false));
  }
}
