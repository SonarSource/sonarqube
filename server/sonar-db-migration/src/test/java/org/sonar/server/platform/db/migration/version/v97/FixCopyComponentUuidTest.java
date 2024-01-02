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
package org.sonar.server.platform.db.migration.version.v97;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

public class FixCopyComponentUuidTest {
  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(FixCopyComponentUuidTest.class, "schema.sql");

  private DataChange underTest;

  @Before
  public void before() {
    underTest = new FixCopyComponentUuid(db.database());
  }

  @Test
  public void updates_copy_component_uuid() throws SQLException {
    insert();

    underTest.execute();

    checkResults();
  }

  @Test
  public void should_be_reentrant() throws SQLException {
    insert();

    underTest.execute();
    underTest.execute();

    checkResults();
  }

  public void checkResults() {
    assertThat(getCopyComponentUuidByUuid())
      .containsOnly(
        entry("uuid1", ""),
        entry("uuid2", ""),
        entry("uuid3", ""),
        entry("uuid4", "uuid1"),
        entry("uuid5", ""),
        entry("uuid6", "uuid2")
      );
  }

  private void insert() {
    // project with a branch
    insertBranch("uuid1", "uuid1", "master");
    insertBranch("uuid2", "uuid1", "branch1");
    insertComponents("uuid1", "project1", null, null);
    insertComponents("uuid2", "project1:BRANCH:branch1", "uuid1", null);

    // app
    insertBranch("uuid3", "uuid3", "master");
    insertComponents("uuid3", "app", null, null);
    insertComponents("uuid4", "appb1project1", null, "uuid1");

    // app branch
    insertBranch("uuid5", "uuid3", "b1");
    insertComponents("uuid5", "app:BRANCH:b1", "uuid3", null);
    // this copyComponentUuid needs to be fixed to point to uuid2
    insertComponents("uuid6", "appb1project1:BRANCH:branch1", "uuid3", "uuid1");
  }

  private Map<String, String> getCopyComponentUuidByUuid() {
    return db.select("select uuid, copy_component_uuid from components").stream()
      .collect(Collectors.toMap(e -> (String) e.get("UUID"), e -> e.get("COPY_COMPONENT_UUID") == null ? "" : (String) e.get("COPY_COMPONENT_UUID")));
  }

  private void insertBranch(String uuid, String projectUuid, String key) {
    db.executeInsert("project_branches",
      "kee", key,
      "uuid", uuid,
      "project_uuid", projectUuid,
      "branch_type", "BRANCH",
      "need_issue_sync", "false",
      "created_at", "1",
      "updated_at", "1");
  }

  private void insertComponents(String uuid, String key, @Nullable String mainBranchUuid, @Nullable String copyComponentUuid) {
    db.executeInsert("components",
      "enabled", true,
      "uuid", uuid,
      "kee", key,
      "branch_uuid", "branch_uuid",
      "root_uuid", "root_uuid",
      "uuid_path", "uuid_path",
      "private", false,
      "copy_component_uuid", copyComponentUuid,
      "main_branch_project_uuid", mainBranchUuid);
  }
}
