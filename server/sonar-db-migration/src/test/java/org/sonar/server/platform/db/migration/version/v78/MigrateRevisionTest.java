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
package org.sonar.server.platform.db.migration.version.v78;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrateRevisionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MigrateRevisionTest.class, "snapshots.sql");

  private MigrateRevision underTest = new MigrateRevision(db.database());

  @Test
  public void copies_revision_from_analysis_properties_to_snapshots() throws SQLException {
    insertSnapshot(1, "uuid1", "cuuid1");
    insertAnalysisProperty("uuid1", "sonar.analysis.scm_revision_id", "000b17c1db52814d41e4f1425292435b4261ef22");
    insertAnalysisProperty("uuid1", "sonar.pullrequest.base", "master");
    insertSnapshot(2, "uuid2", "cuuid2");
    insertSnapshot(3, "uuid3", "cuuid3");
    insertAnalysisProperty("uuid3", "sonar.analysis.scm_revision_id", "7e1071e0606e8c7c8181602f8263bc6f56509ac4");

    underTest.execute();

    Map<Long, String> result = db.select("select ID, REVISION from SNAPSHOTS")
      .stream()
      .collect(HashMap::new, (m, v) -> m.put((Long) v.get("ID"), (String) v.get("REVISION")), HashMap::putAll);
    assertThat(result.get(1L)).isEqualTo("000b17c1db52814d41e4f1425292435b4261ef22");
    assertThat(result.get(2L)).isNull();
    assertThat(result.get(3L)).isEqualTo("7e1071e0606e8c7c8181602f8263bc6f56509ac4");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertSnapshot(1, "uuid1", "cuuid1");
    insertAnalysisProperty("uuid1", "sonar.analysis.scm_revision_id", "7e1071e0606e8c7c8181602f8263bc6f56509ac4");

    underTest.execute();
    underTest.execute();

    Map<Long, String> result = db.select("select ID, REVISION from SNAPSHOTS")
      .stream()
      .collect(HashMap::new, (m, v) -> m.put((Long) v.get("ID"), (String) v.get("REVISION")), HashMap::putAll);
    assertThat(result.get(1L)).isEqualTo("7e1071e0606e8c7c8181602f8263bc6f56509ac4");
  }

  private void insertSnapshot(long id, String uuid, String componentUuid) {
    db.executeInsert(
      "snapshots",
      "id", id,
      "uuid", uuid,
      "component_uuid", componentUuid);
  }

  private void insertAnalysisProperty(String snapshotUuid, String name, String value) {
    db.executeInsert(
      "analysis_properties",
      "UUID", UUID.randomUUID(),
      "SNAPSHOT_UUID", snapshotUuid,
      "KEE", name,
      "TEXT_VALUE", value,
      "IS_EMPTY", false,
      "CREATED_AT", new Date().getTime());
  }
}
