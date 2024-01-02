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
package org.sonar.server.platform.db.migration.version.v99;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteAnalysisCacheTest {
  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(DeleteAnalysisCacheTest.class, "schema.sql");

  private final DeleteAnalysisCache underTest = new DeleteAnalysisCache(db.database());

  @Test
  public void no_op_if_no_data() throws SQLException {
    assertThatCacheIsEmpty();
    underTest.execute();
    assertThatCacheIsEmpty();
  }

  @Test
  public void deletes_all_data_in_table() throws SQLException {
    insertCache("b1", "d1");
    insertCache("b2", "d2");
    assertThatCacheIsNotEmpty();
    underTest.execute();
    assertThatCacheIsEmpty();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertCache("b1", "d1");
    insertCache("b2", "d2");
    assertThatCacheIsNotEmpty();
    underTest.execute();
    underTest.execute();
    assertThatCacheIsEmpty();
  }

  private void assertThatCacheIsEmpty() {
    assertThat(db.countRowsOfTable("scanner_analysis_cache")).isZero();
  }

  private void assertThatCacheIsNotEmpty() {
    assertThat(db.countRowsOfTable("scanner_analysis_cache")).isNotZero();
  }

  private void insertCache(String branch, String data) {
    db.executeInsert("scanner_analysis_cache",
      "branch_uuid", branch,
      "data", data);
  }
}
