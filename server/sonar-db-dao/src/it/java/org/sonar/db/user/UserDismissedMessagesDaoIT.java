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
package org.sonar.db.user;

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.dismissmessage.MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE;

public class UserDismissedMessagesDaoIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final UserDismissedMessagesDao underTest = db.getDbClient().userDismissedMessagesDao();

  @Test
  public void insert_user_dismissed_message() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser();
    UserDismissedMessageDto dto = newDto(project, user);

    underTest.insert(db.getSession(), dto);

    Optional<UserDismissedMessageDto> dtoFromDb = underTest.selectByUserAndProjectAndMessageType(db.getSession(), user.getUuid(), project, dto.getMessageType());
    assertThat(dtoFromDb).isPresent();
    assertThat(dtoFromDb.get().getUuid()).isEqualTo(dto.getUuid());
    assertThat(dtoFromDb.get().getUserUuid()).isEqualTo(dto.getUserUuid());
    assertThat(dtoFromDb.get().getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(dtoFromDb.get().getMessageType()).isEqualTo(dto.getMessageType());
    assertThat(dtoFromDb.get().getMessageType().isDismissible()).isEqualTo(dto.getMessageType().isDismissible());
    assertThat(dtoFromDb.get().getCreatedAt()).isEqualTo(dto.getCreatedAt());
  }

  @Test
  public void selectByUserAndProjectAndMessageType_returns_object_if_record_found() {
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertUserDismissedMessageOnProject(user, project, MessageType.GENERIC);

    Optional<UserDismissedMessageDto> result = underTest.selectByUserAndProjectAndMessageType(db.getSession(), user.getUuid(), project, MessageType.GENERIC);

    assertThat(result).isPresent();
    assertThat(result.get().getUserUuid()).isEqualTo(user.getUuid());
    assertThat(result.get().getProjectUuid()).isEqualTo(project.getUuid());
    assertThat(result.get().getMessageType()).isEqualTo(MessageType.GENERIC);
  }

  @Test
  public void selectByUserAndMessageType_returns_object_if_record_found() {
    UserDto user = db.users().insertUser();
    db.users().insertUserDismissedMessageOnInstance(user, MessageType.GENERIC);

    Optional<UserDismissedMessageDto> result = underTest.selectByUserUuidAndMessageType(db.getSession(), user.getUuid(),MessageType.GENERIC);

    assertThat(result).isPresent();
    assertThat(result.get().getUserUuid()).isEqualTo(user.getUuid());
    assertThat(result.get().getProjectUuid()).isNull();
    assertThat(result.get().getMessageType()).isEqualTo(MessageType.GENERIC);
  }

  @Test
  public void selectByUserAndProjectAndMessageType_returns_absent_if_no_record_found() {
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertUserDismissedMessageOnProject(user, project, MessageType.GENERIC);

    Optional<UserDismissedMessageDto> result = underTest.selectByUserAndProjectAndMessageType(db.getSession(), user.getUuid(), project, SUGGEST_DEVELOPER_EDITION_UPGRADE);

    assertThat(result).isNotPresent();
  }

  @Test
  public void selectByUserUuid_returns_all_dismissed_messages_of_a_user() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    db.users().insertUserDismissedMessageOnProject(user1, project1, MessageType.GENERIC);
    db.users().insertUserDismissedMessageOnProject(user1, project2, MessageType.GENERIC);
    UserDismissedMessageDto dto1 = db.users().insertUserDismissedMessageOnProject(user2, project1, MessageType.GENERIC);
    UserDismissedMessageDto dto2 = db.users().insertUserDismissedMessageOnProject(user2, project2, MessageType.GENERIC);

    List<UserDismissedMessageDto> result = underTest.selectByUser(db.getSession(), user2);

    assertThat(result).hasSize(2);
    assertThat(result).extracting(UserDismissedMessageDto::getUuid, UserDismissedMessageDto::getUserUuid, UserDismissedMessageDto::getProjectUuid,
      UserDismissedMessageDto::getMessageType)
      .containsExactlyInAnyOrder(
        tuple(dto1.getUuid(), user2.getUuid(), project1.getUuid(), MessageType.GENERIC),
        tuple(dto2.getUuid(), user2.getUuid(), project2.getUuid(), MessageType.GENERIC));
  }

  @Test
  public void deleteByUserUuid_removes_dismiss_warning_data_of_a_user() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    db.users().insertUserDismissedMessageOnProject(user1, project1, MessageType.GENERIC);
    db.users().insertUserDismissedMessageOnProject(user1, project2, MessageType.GENERIC);
    db.users().insertUserDismissedMessageOnProject(user2, project1, MessageType.GENERIC);
    db.users().insertUserDismissedMessageOnProject(user2, project2, MessageType.GENERIC);

    underTest.deleteByUser(db.getSession(), user2);

    assertThat(underTest.selectByUser(db.getSession(), user1)).hasSize(2);
    assertThat(underTest.selectByUser(db.getSession(), user2)).isEmpty();
  }

  @Test
  public void deleteByUserUuid_removes_dismissed_messages_of_that_type() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    UserDismissedMessageDto dto1 = db.users().insertUserDismissedMessageOnProject(user1, project1, MessageType.GENERIC);
    db.users().insertUserDismissedMessageOnProject(user1, project2, SUGGEST_DEVELOPER_EDITION_UPGRADE);
    db.users().insertUserDismissedMessageOnProject(user2, project1, SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDismissedMessageDto dto2 = db.users().insertUserDismissedMessageOnProject(user2, project2, MessageType.GENERIC);

    underTest.deleteByType(db.getSession(), SUGGEST_DEVELOPER_EDITION_UPGRADE);

    assertThat(underTest.selectByUser(db.getSession(), user1))
      .extracting(UserDismissedMessageDto::getUuid)
      .containsExactly(dto1.getUuid());
    assertThat(underTest.selectByUser(db.getSession(), user2))
      .extracting(UserDismissedMessageDto::getUuid)
      .containsExactly(dto2.getUuid());
  }

  public static UserDismissedMessageDto newDto(ProjectDto project, UserDto user) {
    return new UserDismissedMessageDto()
      .setUuid(Uuids.createFast())
      .setMessageType(MessageType.GENERIC)
      .setUserUuid(user.getUuid())
      .setProjectUuid(project.getUuid());
  }
}
