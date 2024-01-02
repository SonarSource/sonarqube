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
package org.sonar.server.platform.db.migration.version.v96;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteDuplicatedProjectBadgeTokensTest {
  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(DeleteDuplicatedProjectBadgeTokensTest.class, "schema.sql");

  private final DeleteDuplicatedProjectBadgeTokens deleteDuplicatedProjectBadgeTokens = new DeleteDuplicatedProjectBadgeTokens(db.database());

  @Test
  public void deleteDuplicatedProjectBadgeTokens_whenTokenForDifferentProjects_doesNothing() throws SQLException {
    insertProjectBadgeToken("uuid1", "proj1", 1);
    insertProjectBadgeToken("uuid2", "proj2", 1);

    deleteDuplicatedProjectBadgeTokens.execute();

    assertThat(db.countRowsOfTable("project_badge_token")).isEqualTo(2);
  }

  @Test
  public void deleteDuplicatedProjectBadgeTokens_whenSeveralTokensForSameProjectsAtTheSameTime_leavesOneToken() throws SQLException {
    insertProjectBadgeToken("uuid1", "proj1", 1);
    insertProjectBadgeToken("uuid2", "proj1", 1);
    insertProjectBadgeToken("uuid3", "proj1", 1);

    deleteDuplicatedProjectBadgeTokens.execute();

    assertThat(db.countRowsOfTable("project_badge_token")).isEqualTo(1);
  }

  @Test
  public void deleteDuplicatedProjectBadgeTokens_whenSeveralTokensForSameProjectsAtDifferentTime_leavesMostAncient() throws SQLException {
    insertProjectBadgeToken("uuid1", "proj1", 2);
    insertProjectBadgeToken("uuid2", "proj1", 1);
    insertProjectBadgeToken("uuid3", "proj1", 3);

    deleteDuplicatedProjectBadgeTokens.execute();

    assertThat(db.countRowsOfTable("project_badge_token")).isEqualTo(1);
    assertThat(db.selectFirst("select UUID from project_badge_token")).containsEntry("UUID", "uuid2");
  }

  @Test
  public void deleteDuplicatedProjectBadgeTokens_whenSeveralTokensForSameProjectsAtDifferentAndSameTime_leavesAnyMostAncient() throws SQLException {
    insertProjectBadgeToken("uuid1", "proj1", 2);
    insertProjectBadgeToken("uuid2", "proj1", 1);
    insertProjectBadgeToken("uuid3", "proj1", 3);
    insertProjectBadgeToken("uuid4", "proj1", 1);
    insertProjectBadgeToken("uuid5", "proj1", 2);

    deleteDuplicatedProjectBadgeTokens.execute();

    assertThat(db.countRowsOfTable("project_badge_token")).isEqualTo(1);
    assertThat(db.selectFirst("select UUID from project_badge_token")).containsEntry("UUID", "uuid2");
  }

  @Test
  public void deleteDuplicatedProjectBadgeTokens_whenSeveralTokensForSameProjectsAtDifferentAndSameTime_leavesAnyMostAncient2() throws SQLException {
    insertProjectBadgeToken("uuid1", "proj1", 2);
    insertProjectBadgeToken("uuid2", "proj1", 1);
    insertProjectBadgeToken("uuid3", "proj1", 3);
    insertProjectBadgeToken("uuid4", "proj2", 1);
    insertProjectBadgeToken("uuid5", "proj2", 1);

    deleteDuplicatedProjectBadgeTokens.execute();

    assertThat(db.countRowsOfTable("project_badge_token")).isEqualTo(2);
    assertThat(db.select("select UUID from project_badge_token")).extracting(e -> e.get("UUID")).containsOnly("uuid2", "uuid4");
  }

  @Test
  public void deleteDuplicatedProjectBadgeTokens_reentrantTest() throws SQLException {
    insertProjectBadgeToken("uuid1", "proj1", 1);
    insertProjectBadgeToken("uuid2", "proj2", 1);

    deleteDuplicatedProjectBadgeTokens.execute();
    deleteDuplicatedProjectBadgeTokens.execute();
    deleteDuplicatedProjectBadgeTokens.execute();

    assertThat(db.countRowsOfTable("project_badge_token")).isEqualTo(2);
  }

  private void insertProjectBadgeToken(String uuid, String projectUuid, long createdAt) {
    db.executeInsert("project_badge_token", "UUID", uuid,
      "token", "TEST_TOKEN",
      "project_uuid", projectUuid,
      "created_at", createdAt,
      "updated_at", createdAt
    );
  }

}
