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
package org.sonar.db.ce;

import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.lang.System.lineSeparator;
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
    dbSession.commit(true);

    assertThat(underTest.selectScannerContext(dbSession, SOME_UUID)).contains(scannerContext);
  }

  private static CloseableIterator<String> scannerContextInputStreamOf(String data) {
    return CloseableIterator.from(Collections.singleton(data).iterator());
  }

}
