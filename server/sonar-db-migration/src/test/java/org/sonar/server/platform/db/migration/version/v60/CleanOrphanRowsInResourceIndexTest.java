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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class CleanOrphanRowsInResourceIndexTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, CleanOrphanRowsInResourceIndexTest.class,
    "in_progress_resourceindex.sql");

  private CleanOrphanRowsInResourceIndex underTest = new CleanOrphanRowsInResourceIndex(db.database());

  @Test
  public void migration_has_no_effect_on_empty_table() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("resource_index")).isEqualTo(0);
  }

  @Test
  public void migration_deletes_any_row_with_a_null_uuid() throws SQLException {
    insertResourceIndex(1, true, true);
    insertResourceIndex(2, false, false);
    insertResourceIndex(3, true, false);
    insertResourceIndex(4, false, true);
    insertResourceIndex(5, true, true);
    db.commit();

    underTest.execute();

    assertThat(idsOfRowsInResourceIndex()).containsOnly(1l, 5l);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertResourceIndex(1, true, true);
    insertResourceIndex(2, false, false);

    underTest.execute();

    assertThat(idsOfRowsInResourceIndex()).containsOnly(1l);

    underTest.execute();

    assertThat(idsOfRowsInResourceIndex()).containsOnly(1l);
  }

  private List<Long> idsOfRowsInResourceIndex() {
    return db.select("select ID from resource_index").stream().map(map -> (Long) map.get("ID")).collect(Collectors.toList());
  }

  private void insertResourceIndex(long id, boolean hasComponentUiid, boolean hasRootComponentUuid) {
    db.executeInsert(
      "resource_index",
      "ID", valueOf(id),
      "KEE", "key_" + id,
      "POSITION", valueOf(id + 100),
      "NAME_SIZE", valueOf(id + 1000),
      "RESOURCE_ID", valueOf(id + 300),
      "ROOT_PROJECT_ID", valueOf(id + 4000),
      "QUALIFIER", "PROJECT");

    if (hasComponentUiid) {
      db.executeUpdateSql("update resource_index set COMPONENT_UUID=? where id=?", "uuid_" + id, valueOf(id));
    }
    if (hasRootComponentUuid) {
      db.executeUpdateSql("update resource_index set ROOT_COMPONENT_UUID=? where id=?", "root_uuid_" + id, valueOf(id));
    }
  }
}
