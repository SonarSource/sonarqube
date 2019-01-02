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
package org.sonar.db.ce;

import java.io.InputStream;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CeTaskInputDaoTest {

  private static final String A_UUID = "U1";
  private static final String SOME_DATA = "this_is_a_report";
  private static final long NOW = 1_500_000_000_000L;
  private static final String TABLE_NAME = "ce_task_input";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system = mock(System2.class);
  private CeTaskInputDao underTest = new CeTaskInputDao(system);

  @Test
  public void insert_and_select_data_stream() throws Exception {
    when(system.now()).thenReturn(NOW);

    InputStream report = IOUtils.toInputStream(SOME_DATA);
    underTest.insert(dbTester.getSession(), A_UUID, report);

    Optional<CeTaskInputDao.DataStream> result = underTest.selectData(dbTester.getSession(), A_UUID);
    assertThat(result).isPresent();
    try {
      assertThat(IOUtils.toString(result.get().getInputStream())).isEqualTo(SOME_DATA);
    } finally {
      result.get().close();
    }
  }

  @Test
  public void fail_to_insert_invalid_row() {
    expectedException.expectMessage("Fail to insert data of CE task null");
    underTest.insert(dbTester.getSession(), null, IOUtils.toInputStream(SOME_DATA));
  }

  @Test
  public void selectData_returns_absent_if_uuid_not_found() {
    Optional<CeTaskInputDao.DataStream> result = underTest.selectData(dbTester.getSession(), A_UUID);
    assertThat(result).isNotPresent();
  }

  @Test
  public void selectData_returns_absent_if_uuid_exists_but_data_is_null() {
    insertData(A_UUID);
    dbTester.commit();

    Optional<CeTaskInputDao.DataStream> result = underTest.selectData(dbTester.getSession(), A_UUID);
    assertThat(result).isNotPresent();
  }

  @Test
  public void selectUuidsNotInQueue() {
    insertData("U1");
    insertData("U2");
    assertThat(underTest.selectUuidsNotInQueue(dbTester.getSession())).containsOnly("U1", "U2");

    CeQueueDto inQueue = new CeQueueDto().setUuid("U2").setTaskType(CeTaskTypes.REPORT).setStatus(CeQueueDto.Status.IN_PROGRESS);
    new CeQueueDao(system).insert(dbTester.getSession(), inQueue);
    assertThat(underTest.selectUuidsNotInQueue(dbTester.getSession())).containsOnly("U1");
  }

  @Test
  public void deleteByUuids() {
    insertData(A_UUID);
    assertThat(dbTester.countRowsOfTable(TABLE_NAME)).isEqualTo(1);

    underTest.deleteByUuids(dbTester.getSession(), singleton(A_UUID));
    dbTester.commit();
    assertThat(dbTester.countRowsOfTable(TABLE_NAME)).isEqualTo(0);
  }

  private void insertData(String uuid) {
    dbTester.executeInsert(TABLE_NAME, "task_uuid", uuid, "created_at", NOW, "updated_at", NOW);
    dbTester.commit();
  }
}
