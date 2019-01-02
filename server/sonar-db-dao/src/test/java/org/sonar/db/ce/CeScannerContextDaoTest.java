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

import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.lang.System.lineSeparator;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CeScannerContextDaoTest {

  private static final String TABLE_NAME = "ce_scanner_context";
  private static final String SOME_UUID = "some UUID";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system = mock(System2.class);
  private DbSession dbSession = dbTester.getSession();

  private CeScannerContextDao underTest = new CeScannerContextDao(system);

  @Test
  public void selectScannerContext_returns_empty_on_empty_table() {
    assertThat(underTest.selectScannerContext(dbSession, SOME_UUID)).isEmpty();
  }

  @Test
  public void selectScannerContext_returns_empty_when_no_row_exist_for_taskUuid() {
    String data = "some data";
    underTest.insert(dbSession, SOME_UUID, scannerContextInputStreamOf(data));
    dbSession.commit();

    assertThat(underTest.selectScannerContext(dbSession, "OTHER_uuid")).isEmpty();
    assertThat(underTest.selectScannerContext(dbSession, SOME_UUID)).contains(data);
  }

  @Test
  public void insert_fails_with_IAE_if_data_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Scanner context can not be empty");

    underTest.insert(dbSession, SOME_UUID, CloseableIterator.emptyCloseableIterator());
  }

  @Test
  public void insert_fails_with_IAE_if_data_is_fully_read() {
    CloseableIterator<String> iterator = scannerContextInputStreamOf("aa");
    iterator.next();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Scanner context can not be empty");

    underTest.insert(dbSession, SOME_UUID, iterator);
  }

  @Test
  public void insert_fails_if_row_already_exists_for_taskUuid() {
    underTest.insert(dbSession, SOME_UUID, scannerContextInputStreamOf("bla"));
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable(dbSession, TABLE_NAME)).isEqualTo(1);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to insert scanner context for task " + SOME_UUID);

    underTest.insert(dbSession, SOME_UUID, scannerContextInputStreamOf("blo"));
  }

  @Test
  public void insert_and_select_line_reader() {
    String scannerContext = "line 1" + lineSeparator() + "line 2" + lineSeparator() + "line 3";
    underTest.insert(dbSession, SOME_UUID, scannerContextInputStreamOf(scannerContext));
    dbSession.commit();

    assertThat(underTest.selectScannerContext(dbSession, SOME_UUID)).contains(scannerContext);
  }

  @Test
  public void deleteByUuids_does_not_fail_on_empty_table() {
    underTest.deleteByUuids(dbSession, singleton("some uuid"));
  }

  @Test
  public void deleteByUuids_deletes_specified_existing_uuids() {
    insertScannerContext(SOME_UUID);
    String data2 = insertScannerContext("UUID_2");
    insertScannerContext("UUID_3");

    underTest.deleteByUuids(dbSession, ImmutableSet.of(SOME_UUID, "UUID_3", "UUID_4"));

    assertThat(underTest.selectScannerContext(dbSession, SOME_UUID)).isEmpty();
    assertThat(underTest.selectScannerContext(dbSession, "UUID_2")).contains(data2);
    assertThat(underTest.selectScannerContext(dbSession, "UUID_3")).isEmpty();
  }

  @Test
  public void selectOlderThan() {
    insertWithCreationDate("TASK_1", 1_450_000_000_000L);
    insertWithCreationDate("TASK_2", 1_460_000_000_000L);
    insertWithCreationDate("TASK_3", 1_470_000_000_000L);

    assertThat(underTest.selectOlderThan(dbSession, 1_465_000_000_000L))
      .containsOnly("TASK_1", "TASK_2");
    assertThat(underTest.selectOlderThan(dbSession, 1_450_000_000_000L))
      .isEmpty();
  }

  private void insertWithCreationDate(String uuid, long createdAt) {
    dbTester.executeInsert(
      "CE_SCANNER_CONTEXT",
      "task_uuid", uuid,
      "created_at", createdAt,
      "updated_at", 1,
      "context_data", "YoloContent".getBytes());
    dbSession.commit();
  }

  private String insertScannerContext(String uuid) {
    String data = "data of " + uuid;
    underTest.insert(dbSession, uuid, scannerContextInputStreamOf(data));
    dbSession.commit();
    return data;
  }

  private static CloseableIterator<String> scannerContextInputStreamOf(String data) {
    return CloseableIterator.from(singleton(data).iterator());
  }

}
