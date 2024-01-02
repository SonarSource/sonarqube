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
import org.sonar.db.ce.CeTaskMessageType;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.ce.CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE;

public class UserDismissedMessagesDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final UserDismissedMessagesDao underTest = db.getDbClient().userDismissedMessagesDao();

  @Test
  public void insert_user_dismissed_message() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    UserDto user = db.users().insertUser();
    UserDismissedMessageDto dto = newDto(project, user);

    underTest.insert(db.getSession(), dto);

    Optional<UserDismissedMessageDto> dtoFromDb = underTest.selectByUserAndProjectAndMessageType(db.getSession(), user, project, dto.getCeMessageType());
    assertThat(dtoFromDb).isPresent();
    assertThat(dtoFromDb.get().getUuid()).isEqualTo(dto.getUuid());
    assertThat(dtoFromDb.get().getUserUuid()).isEqualTo(dto.getUserUuid());
    assertThat(dtoFromDb.get().getProjectUuid()).isEqualTo(dto.getProjectUuid());
    assertThat(dtoFromDb.get().getCeMessageType()).isEqualTo(dto.getCeMessageType());
    assertThat(dtoFromDb.get().getCeMessageType().isDismissible()).isEqualTo(dto.getCeMessageType().isDismissible());
    assertThat(dtoFromDb.get().getCreatedAt()).isEqualTo(dto.getCreatedAt());
  }

  @Test
  public void selectByUserAndProjectAndMessageType_returns_object_if_record_found() {
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProjectDto();
    db.users().insertUserDismissedMessage(user, project, CeTaskMessageType.GENERIC);

    Optional<UserDismissedMessageDto> result = underTest.selectByUserAndProjectAndMessageType(db.getSession(), user, project, CeTaskMessageType.GENERIC);

    assertThat(result).isPresent();
    assertThat(result.get().getUserUuid()).isEqualTo(user.getUuid());
    assertThat(result.get().getProjectUuid()).isEqualTo(project.getUuid());
    assertThat(result.get().getCeMessageType()).isEqualTo(CeTaskMessageType.GENERIC);
  }

  @Test
  public void selectByUserAndProjectAndMessageType_returns_absent_if_no_record_found() {
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProjectDto();
    db.users().insertUserDismissedMessage(user, project, CeTaskMessageType.GENERIC);

    Optional<UserDismissedMessageDto> result = underTest.selectByUserAndProjectAndMessageType(db.getSession(), user, project, SUGGEST_DEVELOPER_EDITION_UPGRADE);

    assertThat(result).isNotPresent();
  }

  @Test
  public void selectByUserUuid_returns_all_dismissed_messages_of_a_user() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    ProjectDto project2 = db.components().insertPrivateProjectDto();
    db.users().insertUserDismissedMessage(user1, project1, CeTaskMessageType.GENERIC);
    db.users().insertUserDismissedMessage(user1, project2, CeTaskMessageType.GENERIC);
    UserDismissedMessageDto dto1 = db.users().insertUserDismissedMessage(user2, project1, CeTaskMessageType.GENERIC);
    UserDismissedMessageDto dto2 = db.users().insertUserDismissedMessage(user2, project2, CeTaskMessageType.GENERIC);

    List<UserDismissedMessageDto> result = underTest.selectByUser(db.getSession(), user2);

    assertThat(result).hasSize(2);
    assertThat(result).extracting(UserDismissedMessageDto::getUuid, UserDismissedMessageDto::getUserUuid, UserDismissedMessageDto::getProjectUuid,
      UserDismissedMessageDto::getCeMessageType)
      .containsExactlyInAnyOrder(
        tuple(dto1.getUuid(), user2.getUuid(), project1.getUuid(), CeTaskMessageType.GENERIC),
        tuple(dto2.getUuid(), user2.getUuid(), project2.getUuid(), CeTaskMessageType.GENERIC));
  }

  @Test
  public void deleteByUserUuid_removes_dismiss_warning_data_of_a_user() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    ProjectDto project2 = db.components().insertPrivateProjectDto();
    db.users().insertUserDismissedMessage(user1, project1, CeTaskMessageType.GENERIC);
    db.users().insertUserDismissedMessage(user1, project2, CeTaskMessageType.GENERIC);
    db.users().insertUserDismissedMessage(user2, project1, CeTaskMessageType.GENERIC);
    db.users().insertUserDismissedMessage(user2, project2, CeTaskMessageType.GENERIC);

    underTest.deleteByUser(db.getSession(), user2);

    assertThat(underTest.selectByUser(db.getSession(), user1)).hasSize(2);
    assertThat(underTest.selectByUser(db.getSession(), user2)).isEmpty();
  }

  @Test
  public void deleteByUserUuid_removes_dismissed_messages_of_that_type() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    ProjectDto project2 = db.components().insertPrivateProjectDto();
    UserDismissedMessageDto dto1 = db.users().insertUserDismissedMessage(user1, project1, CeTaskMessageType.GENERIC);
    db.users().insertUserDismissedMessage(user1, project2, SUGGEST_DEVELOPER_EDITION_UPGRADE);
    db.users().insertUserDismissedMessage(user2, project1, SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDismissedMessageDto dto2 = db.users().insertUserDismissedMessage(user2, project2, CeTaskMessageType.GENERIC);

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
      .setCeMessageType(CeTaskMessageType.GENERIC)
      .setUserUuid(user.getUuid())
      .setProjectUuid(project.getUuid());
  }
}
