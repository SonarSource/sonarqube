/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

class RemoveDuplicatePermissionTemplatesIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RemoveDuplicatePermissionTemplates.class);

  private final DataChange underTest = new RemoveDuplicatePermissionTemplates(db.database());

  @Test
  void execute_shouldKeepOldestTemplateAndRemoveDependenciesOfCaseInsensitiveDuplicates() throws SQLException {
    insertTemplate("old-template", "Finance", 1_000L);
    insertTemplate("new-template", "finance", 2_000L);
    insertTemplate("other-template", "Engineering", 3_000L);
    insertDependencies("new-template");
    db.executeInsert("internal_properties",
      "kee", "defaultTemplate.prj",
      "is_empty", false,
      "text_value", "new-template",
      "created_at", 2_000L);

    underTest.execute();

    assertThat(select("SELECT uuid FROM permission_templates"))
      .extracting(row -> row.get("UUID"))
      .containsExactlyInAnyOrder("old-template", "other-template");
    assertThat(select("SELECT uuid FROM perm_templates_users WHERE template_uuid = 'new-template'")).isEmpty();
    assertThat(select("SELECT uuid FROM perm_templates_groups WHERE template_uuid = 'new-template'")).isEmpty();
    assertThat(select("SELECT uuid FROM perm_tpl_characteristics WHERE template_uuid = 'new-template'")).isEmpty();
    assertThat(select("SELECT text_value FROM internal_properties WHERE kee = 'defaultTemplate.prj'"))
      .extracting(row -> row.get("TEXT_VALUE"))
      .containsExactly("old-template");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    insertTemplate("old-template", "Finance", 1_000L);
    insertTemplate("new-template", "finance", 2_000L);

    underTest.execute();
    underTest.execute();

    assertThat(select("SELECT uuid FROM permission_templates"))
      .extracting(row -> row.get("UUID"))
      .containsExactly("old-template");
  }

  private void insertTemplate(String uuid, String name, long createdAt) {
    db.executeInsert("permission_templates",
      "uuid", uuid,
      "name", name,
      "created_at", new Timestamp(createdAt),
      "updated_at", new Timestamp(createdAt));
  }

  private void insertDependencies(String templateUuid) {
    db.executeInsert("perm_templates_users",
      "uuid", "user-permission",
      "template_uuid", templateUuid,
      "user_uuid", "user",
      "permission_reference", "user");
    db.executeInsert("perm_templates_groups",
      "uuid", "group-permission",
      "template_uuid", templateUuid,
      "group_uuid", "group",
      "permission_reference", "user");
    db.executeInsert("perm_tpl_characteristics",
      "uuid", "characteristic",
      "template_uuid", templateUuid,
      "permission_key", "user",
      "with_project_creator", false,
      "created_at", 2_000L,
      "updated_at", 2_000L);
  }

  private List<Map<String, Object>> select(String sql) {
    return db.select(sql);
  }
}
