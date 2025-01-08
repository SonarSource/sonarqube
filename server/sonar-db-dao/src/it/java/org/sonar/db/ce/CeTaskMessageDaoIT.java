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
package org.sonar.db.ce;

import java.util.List;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;

class CeTaskMessageDaoIT {
  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final CeTaskMessageDao underTest = new CeTaskMessageDao();

  @Test
  void insert() {
    underTest.insert(dbTester.getSession(), new CeTaskMessageDto()
      .setUuid("uuid_1")
      .setTaskUuid("task_uuid_1")
      .setMessage("message_1")
      .setType(MessageType.GENERIC)
      .setCreatedAt(1_222_333L));
    dbTester.getSession().commit();

    assertThat(
      dbTester.select("select uuid as \"UUID\", task_uuid as \"TASK_UUID\", message as \"MESSAGE\", message_type as \"TYPE\", " +
        "created_at as \"CREATED_AT\" from ce_task_message"))
      .hasSize(1)
      .extracting(t -> t.get("UUID"), t -> t.get("TASK_UUID"), t -> t.get("MESSAGE"), t -> MessageType.valueOf((String) t.get("TYPE")),
        t -> t.get("CREATED_AT"))
      .containsOnly(Tuple.tuple("uuid_1", "task_uuid_1", "message_1", MessageType.GENERIC, 1_222_333L));
  }

  @Test
  void selectByUuid_returns_object_if_found() {
    CeTaskMessageDto dto = insertMessage("526787a4-e8af-46c0-b340-8c48188646a5", 1, 1_222_333L);

    Optional<CeTaskMessageDto> result = underTest.selectByUuid(dbTester.getSession(), dto.getUuid());

    assertThat(result).isPresent();
    assertThat(result.get().getUuid()).isEqualTo(dto.getUuid());
  }

  @Test
  void selectByUuid_returns_empty_if_no_record_found() {
    Optional<CeTaskMessageDto> result = underTest.selectByUuid(dbTester.getSession(), "e2a71626-1f07-402a-aac7-dd4e0bbb4394");

    assertThat(result).isNotPresent();
  }

  @Test
  void deleteByType_deletes_messages_of_given_type() {
    String task1 = "task1";
    CeTaskMessageDto[] messages = {
      insertMessage(task1, 0, 1_222_333L, MessageType.GENERIC),
      insertMessage(task1, 1, 2_222_333L, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE),
      insertMessage(task1, 2, 1_111_333L, MessageType.GENERIC),
      insertMessage(task1, 3, 1_222_111L, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE)
    };

    underTest.deleteByType(dbTester.getSession(), MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);

    assertThat(underTest.selectByUuid(dbTester.getSession(), messages[0].getUuid())).isPresent();
    assertThat(underTest.selectByUuid(dbTester.getSession(), messages[1].getUuid())).isEmpty();
    assertThat(underTest.selectByUuid(dbTester.getSession(), messages[2].getUuid())).isPresent();
    assertThat(underTest.selectByUuid(dbTester.getSession(), messages[3].getUuid())).isEmpty();
  }

  @Test
  void selectNonDismissedByUserAndTask_returns_empty_on_empty_table() {
    UserDto user = dbTester.users().insertUser();
    String taskUuid = "17ae66e6-fe83-4c80-b704-4b04e9c5abe8";

    List<CeTaskMessageDto> dto = underTest.selectNonDismissedByUserAndTask(dbTester.getSession(), taskUuid, user.getUuid());

    assertThat(dto).isEmpty();
  }

  @Test
  void selectNonDismissedByUserAndTask_returns_non_dismissed_messages() {
    UserDto user = dbTester.users().insertUser();
    ProjectDto project = dbTester.components().insertPrivateProject().getProjectDto();
    dbTester.users().insertUserDismissedMessageOnProject(user, project, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    String taskUuid = "17ae66e6-fe83-4c80-b704-4b04e9c5abe8";
    CeTaskMessageDto msg1 = insertMessage(taskUuid, 1, 1_222_333L);
    insertMessage(taskUuid, 2, 1_222_334L, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    CeTaskMessageDto msg3 = insertMessage(taskUuid, 3, 1_222_335L);
    List<CeTaskMessageDto> messages = underTest.selectNonDismissedByUserAndTask(dbTester.getSession(), taskUuid, user.getUuid());

    assertThat(messages).hasSize(2);
    assertThat(messages).extracting(CeTaskMessageDto::getUuid).containsExactlyInAnyOrder(msg1.getUuid(), msg3.getUuid());
  }

  private CeTaskMessageDto insertMessage(String taskUuid, int i, long createdAt) {
    return insertMessage(taskUuid, i, createdAt, MessageType.GENERIC);
  }

  private CeTaskMessageDto insertMessage(String taskUuid, int i, long createdAt, MessageType messageType) {
    CeTaskMessageDto res = new CeTaskMessageDto()
      .setUuid("message_" + i)
      .setTaskUuid(taskUuid)
      .setMessage("test_" + i)
      .setType(messageType)
      .setCreatedAt(createdAt);
    DbSession dbSession = dbTester.getSession();
    underTest.insert(dbSession, res);
    dbSession.commit();
    return res;
  }
}
