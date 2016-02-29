/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;


public class QualityGateDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  QualityGateDao dao = dbTester.getDbClient().qualityGateDao();

  @Test
  public void testInsert() throws Exception {
    dbTester.prepareDbUnit(getClass(), "insert.xml");
    QualityGateDto newQgate = new QualityGateDto().setName("My Quality Gate");
    dao.insert(newQgate);
    dbTester.assertDbUnitTable(getClass(), "insert-result.xml", "quality_gates", "name");
    assertThat(newQgate.getId()).isNotNull();
  }

  @Test
  public void testSelectAll() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");
    Collection<QualityGateDto> allQualityGates = dao.selectAll();
    assertThat(allQualityGates).hasSize(3);
    Iterator<QualityGateDto> gatesIterator = allQualityGates.iterator();
    assertThat(gatesIterator.next().getName()).isEqualTo("Balanced");
    assertThat(gatesIterator.next().getName()).isEqualTo("Lenient");
    assertThat(gatesIterator.next().getName()).isEqualTo("Very strict");
  }

  @Test
  public void testSelectByName() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");
    assertThat(dao.selectByName("Balanced").getName()).isEqualTo("Balanced");
    assertThat(dao.selectByName("Unknown")).isNull();
  }

  @Test
  public void testSelectById() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");
    assertThat(dao.selectById(1L).getName()).isEqualTo("Very strict");
    assertThat(dao.selectById(42L)).isNull();
  }

  @Test
  public void testDelete() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");
    dao.delete(new QualityGateDto().setId(1L));
    dbTester.assertDbUnitTable(getClass(), "delete-result.xml", "quality_gates", "id", "name");
  }

  @Test
  public void testUpdate() throws Exception {
    dbTester.prepareDbUnit(getClass(), "selectAll.xml");
    dao.update(new QualityGateDto().setId(1L).setName("Not so strict"));
    dbTester.assertDbUnitTable(getClass(), "update-result.xml", "quality_gates", "id", "name");
  }
}
