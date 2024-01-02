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
package org.sonar.db.issue;

import java.time.Instant;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class AnticipatedTransitionDaoIT {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final AnticipatedTransitionDao underTest = db.getDbClient().anticipatedTransitionDao();

  @Test
  public void select_anticipated_transition() {
    final String projectUuid = "project147852";
    String atUuid = "uuid_123456";
    String atUuid2 = "uuid_123457";
    // insert one
    generateAndInsertAnticipatedTransition(atUuid, projectUuid, "userUuid", "filePath1");
    generateAndInsertAnticipatedTransition(atUuid2, projectUuid, "userUuid", "filePath2");

    // select all
    var anticipatedTransitionDtos = underTest.selectByProjectUuid(db.getSession(), projectUuid);
    assertThat(anticipatedTransitionDtos).hasSize(2);
    assertThat(anticipatedTransitionDtos.get(0))
      .extracting("uuid").isEqualTo(atUuid);
    assertThat(anticipatedTransitionDtos.get(1))
      .extracting("uuid").isEqualTo(atUuid2);

    // delete one
    underTest.delete(db.getSession(), atUuid);

    // select all
    var anticipatedTransitionDtosDeleted = underTest.selectByProjectUuid(db.getSession(), projectUuid);
    assertThat(anticipatedTransitionDtosDeleted).hasSize(1);
  }

  @Test
  public void select_anticipated_transition_by_project_and_filepath() {
    final String projectUuid = "project147852";
    String atUuid = "uuid_123456";
    String atUuid2 = "uuid_123457";
    String filePath = "filePath1";

    // insert two
    generateAndInsertAnticipatedTransition(atUuid, projectUuid, "userUuid", filePath);
    generateAndInsertAnticipatedTransition(atUuid2, projectUuid, "userUuid", "filePath2");

    // select one by project filePath
    var anticipatedTransitionDtos = underTest.selectByProjectUuidAndFilePath(db.getSession(), projectUuid, filePath);
    assertThat(anticipatedTransitionDtos).hasSize(1);
    assertThat(anticipatedTransitionDtos.get(0))
      .extracting("uuid", "filePath").containsExactly(atUuid, filePath);

    // delete one
    underTest.delete(db.getSession(), atUuid);

    // select all
    var anticipatedTransitionDtosDeleted = underTest.selectByProjectUuid(db.getSession(), projectUuid);
    assertThat(anticipatedTransitionDtosDeleted).hasSize(1);
  }

  @Test
  public void deleteByProjectAndUser_shouldDeleteAllRelatedRecords() {
    // given
    final String projectUuid1 = "project1";
    final String projectUuid2 = "project2";
    String userUuid1 = "user1";
    String userUuid2 = "user2";

    generateAndInsertAnticipatedTransition("uuid1", projectUuid1, userUuid1); // should be deleted
    generateAndInsertAnticipatedTransition("uuid2", projectUuid1, userUuid1); // should be deleted
    generateAndInsertAnticipatedTransition("uuid3", projectUuid2, userUuid1);
    generateAndInsertAnticipatedTransition("uuid4", projectUuid1, userUuid2);
    generateAndInsertAnticipatedTransition("uuid5", projectUuid2, userUuid2);
    generateAndInsertAnticipatedTransition("uuid6", projectUuid1, userUuid1); // should be deleted

    // when
    underTest.deleteByProjectAndUser(db.getSession(), projectUuid1, userUuid1);

    // then
    assertThat(underTest.selectByProjectUuid(db.getSession(), projectUuid1)).hasSize(1);
    assertThat(underTest.selectByProjectUuid(db.getSession(), projectUuid2)).hasSize(2);
  }

  private void generateAndInsertAnticipatedTransition(String uuid, String projectUuid, String userUuid) {
    generateAndInsertAnticipatedTransition(uuid, projectUuid, userUuid, "filePath");
  }

  private void generateAndInsertAnticipatedTransition(String uuid, String projectUuid, String userUuid, String filePath) {
    AnticipatedTransitionDto transition = new AnticipatedTransitionDto(
      uuid,
      projectUuid,
      userUuid,
      "transition",
      "status",
      1,
      "message",
      "lineHash",
      "ruleKey",
      filePath,
      Instant.now().getEpochSecond());

    // insert one
    underTest.insert(db.getSession(), transition);
  }
}
