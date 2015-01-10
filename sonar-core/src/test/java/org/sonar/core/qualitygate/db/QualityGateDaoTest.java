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
package org.sonar.core.qualitygate.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collection;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateDaoTest extends AbstractDaoTestCase {

  private static QualityGateDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new QualityGateDao(getMyBatis());
  }

  @Test
  public void testInsert() throws Exception {
    setupData("insert");
    QualityGateDto newQgate = new QualityGateDto().setName("My Quality Gate");
    dao.insert(newQgate);
    checkTable("insert", "quality_gates", "name");
    assertThat(newQgate.getId()).isNotNull();
  }

  @Test
  public void testSelectAll() throws Exception {
    setupData("selectAll");
    Collection<QualityGateDto> allQualityGates = dao.selectAll();
    assertThat(allQualityGates).hasSize(3);
    Iterator<QualityGateDto> gatesIterator = allQualityGates.iterator();
    assertThat(gatesIterator.next().getName()).isEqualTo("Balanced");
    assertThat(gatesIterator.next().getName()).isEqualTo("Lenient");
    assertThat(gatesIterator.next().getName()).isEqualTo("Very strict");
  }

  @Test
  public void testSelectByName() throws Exception {
    setupData("selectAll");
    assertThat(dao.selectByName("Balanced").getName()).isEqualTo("Balanced");
    assertThat(dao.selectByName("Unknown")).isNull();
  }

  @Test
  public void testSelectById() throws Exception {
    setupData("selectAll");
    assertThat(dao.selectById(1L).getName()).isEqualTo("Very strict");
    assertThat(dao.selectById(42L)).isNull();
  }

  @Test
  public void testDelete() throws Exception {
    setupData("selectAll");
    dao.delete(new QualityGateDto().setId(1L));
    checkTable("delete", "quality_gates", "id", "name");
  }

  @Test
  public void testUpdate() throws Exception {
    setupData("selectAll");
    dao.update(new QualityGateDto().setId(1L).setName("Not so strict"));
    checkTable("update", "quality_gates", "id", "name");
  }
}
