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
package org.sonar.server.startup;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.user.UserDismissedMessageDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class UpgradeSuggestionsCleanerIT {
  private static final String TASK_UUID = "b8d564dd-4ceb-4dba-8a3d-5fafa2d72cdf";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private final UpgradeSuggestionsCleaner underTest = new UpgradeSuggestionsCleaner(dbTester.getDbClient(), sonarRuntime);

  private UserDto user;

  @Before
  public void setup() {
    user = dbTester.users().insertUser();
  }

  @DataProvider
  public static Object[][] editionsWithCleanup() {
    return new Object[][] {
      {SonarEdition.DEVELOPER},
      {SonarEdition.ENTERPRISE},
      {SonarEdition.DATACENTER}
    };
  }

  @DataProvider
  public static Object[][] allEditions() {
    return new Object[][] {
      {SonarEdition.COMMUNITY},
      {SonarEdition.DEVELOPER},
      {SonarEdition.ENTERPRISE},
      {SonarEdition.DATACENTER}
    };
  }

  @Test
  @UseDataProvider("editionsWithCleanup")
  public void start_cleans_up_obsolete_upgrade_suggestions(SonarEdition edition) {
    when(sonarRuntime.getEdition()).thenReturn(edition);
    insertTask(TASK_UUID);
    insertCeTaskMessage("ctm1", MessageType.GENERIC, "msg1");
    insertCeTaskMessage("ctm2", MessageType.GENERIC, "msg2");
    insertCeTaskMessage("ctm3", MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE, "upgrade-msg-1");
    insertInUserDismissedMessages("u1", MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    insertInUserDismissedMessages("u2", MessageType.GENERIC);

    underTest.start();
    underTest.stop();

    assertThat(getTaskMessages(TASK_UUID))
      .extracting(CeTaskMessageDto::getUuid)
      .containsExactly("ctm1", "ctm2");
    assertThat(dbTester.getDbClient().userDismissedMessagesDao().selectByUser(dbTester.getSession(), user))
      .extracting(UserDismissedMessageDto::getUuid)
      .containsExactly("u2");
  }

  @Test
  public void start_does_nothing_in_community_edition() {
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.COMMUNITY);
    insertTask(TASK_UUID);
    insertCeTaskMessage("ctm1", MessageType.GENERIC, "msg1");
    insertCeTaskMessage("ctm2", MessageType.GENERIC, "msg2");
    insertCeTaskMessage("ctm3", MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE, "upgrade-msg-1");
    insertInUserDismissedMessages("u1", MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    insertInUserDismissedMessages("u2", MessageType.GENERIC);

    underTest.start();

    assertThat(getTaskMessages(TASK_UUID))
      .extracting(CeTaskMessageDto::getUuid)
      .containsExactly("ctm1", "ctm2", "ctm3");
    assertThat(dbTester.getDbClient().userDismissedMessagesDao().selectByUser(dbTester.getSession(), user))
      .extracting(UserDismissedMessageDto::getUuid)
      .containsExactlyInAnyOrder("u1", "u2");
  }

  @Test
  @UseDataProvider("allEditions")
  public void start_does_nothing_when_no_suggest_upgrade_messages(SonarEdition edition) {
    when(sonarRuntime.getEdition()).thenReturn(edition);

    underTest.start();

    assertThat(getTaskMessages(TASK_UUID)).isEmpty();
    assertThat(dbTester.getDbClient().userDismissedMessagesDao().selectByUser(dbTester.getSession(), user)).isEmpty();
  }

  private List<CeTaskMessageDto> getTaskMessages(String taskUuid) {
    return dbTester.getDbClient().ceActivityDao().selectByUuid(dbTester.getSession(), taskUuid).map(CeActivityDto::getCeTaskMessageDtos).orElse(List.of());
  }

  private void insertTask(String taskUuid) {
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(),
      new CeActivityDto(new CeQueueDto().setUuid(taskUuid).setTaskType("ISSUE_SYNC")).setStatus(CeActivityDto.Status.FAILED));
  }

  private void insertCeTaskMessage(String uuid, MessageType messageType, String msg) {
    CeTaskMessageDto dto = new CeTaskMessageDto()
      .setUuid(uuid)
      .setMessage(msg)
      .setType(messageType)
      .setTaskUuid(TASK_UUID);
    dbTester.getDbClient().ceTaskMessageDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
  }

  private void insertInUserDismissedMessages(String uuid, MessageType messageType) {
    UserDismissedMessageDto dto = new UserDismissedMessageDto()
      .setUuid(uuid)
      .setUserUuid(user.getUuid())
      .setProjectUuid("PROJECT_1")
      .setMessageType(messageType);
    dbTester.getDbClient().userDismissedMessagesDao().insert(dbTester.getSession(), dto);
    dbTester.getSession().commit();
  }
}
