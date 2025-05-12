/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v202503;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class BackfillRemoveAssigneeNameFromIssueReleaseChangesIT {
  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(BackfillRemoveAssigneeNameFromIssueReleaseChanges.class);
  private final DataChange underTest = new BackfillRemoveAssigneeNameFromIssueReleaseChanges(db.database());

  @Test
  void execute_withAssigneeName_modifiesRecords() throws SQLException {
    // Record with only assigneeName field - should result in empty object
    insertIssueReleaseChange("1", "{\"assigneeName\": [null, \"alice\"]}");

    // Record with mixed fields - should remove only assigneeName
    insertIssueReleaseChange("2",
      "{" +
        "\"assigneeUuid\":[\"user1-uuid\",\"user2-uuid\"]," +
        "\"assigneeName\":[\"alice\",\"bob\"]}");

    underTest.execute();

    assertThat(getChangeData("1")).isEqualTo("{}");
    assertThat(getChangeData("2")).isEqualTo("{\"assigneeUuid\":[\"user1-uuid\",\"user2-uuid\"]}");
  }

  @Test
  void execute_withoutAssigneeName_doesNotModifyRecords() throws SQLException {
    var changeData1 = "{\"assigneeUuid\":[\"user1-uuid\",\"user2-uuid\"]}";
    insertIssueReleaseChange("1", changeData1);

    underTest.execute();

    assertThat(getChangeData("1")).isEqualTo(changeData1);
  }

  @Test
  void execute_withNullChangeData_doesNotModifyRecords() throws SQLException {
    insertIssueReleaseChange("1", null);

    underTest.execute();

    assertThat(getChangeData("1")).isNull();
  }

  private void insertIssueReleaseChange(String suffix, @Nullable String changeData) {
    db.executeInsert("sca_issue_rels_changes",
      "uuid", "scaIssueReleaseChangeUuid" + suffix,
      "sca_issues_releases_uuid", "scaIssueReleaseUuid" + suffix,
      "user_uuid", "user1-uuid",
      "change_data", changeData,
      "created_at", 1L,
      "updated_at", 2L,
      "change_comment", "my comment");
  }

  private String getChangeData(String suffix) {
    var uuid = "scaIssueReleaseChangeUuid" + suffix;
    return (String) db.selectFirst("select change_data from sca_issue_rels_changes where uuid = '" + uuid + "'")
      .get("change_data");
  }
}
