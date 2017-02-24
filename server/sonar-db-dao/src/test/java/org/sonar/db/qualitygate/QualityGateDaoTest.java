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

import java.util.Collection;
import java.util.Iterator;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();
  private QualityGateDao underTest = dbTester.getDbClient().qualityGateDao();

  @Test
  public void testInsert() throws Exception {
    dbTester.prepareDbUnit(getClass(), "insert.xml");
    QualityGateDto newQgate = new QualityGateDto().setName("My Quality Gate");

    underTest.insert(dbSession, newQgate);
    dbSession.commit();

    dbTester.assertDbUnitTable(getClass(), "insert-result.xml", "quality_gates", "name");
    assertThat(newQgate.getId()).isNotNull();
  }

  @Test
  public void testSelectAll() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");

    Collection<QualityGateDto> allQualityGates = underTest.selectAll(dbSession);

    assertThat(allQualityGates).hasSize(3);
    Iterator<QualityGateDto> gatesIterator = allQualityGates.iterator();
    assertThat(gatesIterator.next().getName()).isEqualTo("Balanced");
    assertThat(gatesIterator.next().getName()).isEqualTo("Lenient");
    assertThat(gatesIterator.next().getName()).isEqualTo("Very strict");
  }

  @Test
  public void testSelectByName() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");
    assertThat(underTest.selectByName(dbSession, "Balanced").getName()).isEqualTo("Balanced");
    assertThat(underTest.selectByName(dbSession, "Unknown")).isNull();
  }

  @Test
  public void testSelectById() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");
    assertThat(underTest.selectById(dbSession, 1L).getName()).isEqualTo("Very strict");
    assertThat(underTest.selectById(dbSession, 42L)).isNull();
  }

  @Test
  public void testDelete() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");

    underTest.delete(new QualityGateDto().setId(1L), dbSession);
    dbSession.commit();

    dbTester.assertDbUnitTable(getClass(), "delete-result.xml", "quality_gates", "id", "name");
  }

  @Test
  public void testUpdate() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");

    underTest.update(new QualityGateDto().setId(1L).setName("Not so strict"), dbSession);
    dbSession.commit();

    dbTester.assertDbUnitTable(getClass(), "update-result.xml", "quality_gates", "id", "name");
  }
}
