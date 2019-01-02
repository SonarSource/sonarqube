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
package org.sonar.ce.task.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTask;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CeTaskMessagesImplTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private String taskUuid = randomAlphabetic(12);

  private CeTask ceTask = new CeTask.Builder()
    .setUuid(taskUuid)
    .setOrganizationUuid(randomAlphabetic(10))
    .setType(randomAlphabetic(5))
    .build();

  private CeTaskMessagesImpl underTest = new CeTaskMessagesImpl(dbClient, uuidFactory, ceTask);

  @Test
  public void add_fails_with_NPE_if_arg_is_null() {
    expectMessageCantBeNullNPE();

    underTest.add(null);
  }

  @Test
  public void add_persist_message_to_DB() {
    CeTaskMessages.Message message = new CeTaskMessages.Message(randomAlphabetic(20), 2_999L);
    String uuid = randomAlphanumeric(40);
    when(uuidFactory.create()).thenReturn(uuid);

    underTest.add(message);

    assertThat(dbTester.select("select uuid as \"UUID\", task_uuid as \"TASK_UUID\", message as \"MESSAGE\", created_at as \"CREATED_AT\" from ce_task_message"))
      .extracting(t -> t.get("UUID"), t -> t.get("TASK_UUID"), t -> t.get("MESSAGE"), t -> t.get("CREATED_AT"))
      .containsOnly(tuple(uuid, taskUuid, message.getText(), message.getTimestamp()));
  }

  @Test
  public void addAll_fails_with_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.addAll(null);
  }

  @Test
  public void addAll_fails_with_NPE_if_any_message_in_list_is_null() {
    Random random = new Random();
    List<CeTaskMessages.Message> messages = Stream.of(
      // some (or none) non null Message before null one
      IntStream.range(0, random.nextInt(5)).mapToObj(i -> new CeTaskMessages.Message(randomAlphabetic(3) + "_i", 1_999L + i)),
      Stream.of((CeTaskMessages.Message) null),
      // some (or none) non null Message after null one
      IntStream.range(0, random.nextInt(5)).mapToObj(i -> new CeTaskMessages.Message(randomAlphabetic(3) + "_i", 1_999L + i)))
      .flatMap(t -> t)
      .collect(toList());

    expectMessageCantBeNullNPE();

    underTest.addAll(messages);
  }

  @Test
  public void addAll_has_no_effect_if_arg_is_empty() {
    DbClient dbClientMock = mock(DbClient.class);
    UuidFactory uuidFactoryMock = mock(UuidFactory.class);
    CeTask ceTaskMock = mock(CeTask.class);
    CeTaskMessagesImpl underTest = new CeTaskMessagesImpl(dbClientMock, uuidFactoryMock, ceTaskMock);

    underTest.addAll(Collections.emptyList());

    verifyZeroInteractions(dbClientMock, uuidFactoryMock, ceTaskMock);
  }

  @Test
  public void addAll_persists_all_messages_to_DB() {
    int messageCount = 5;
    String[] uuids = IntStream.range(0, messageCount).mapToObj(i -> "UUId_" + i).toArray(String[]::new);
    CeTaskMessages.Message[] messages = IntStream.range(0, messageCount)
      .mapToObj(i -> new CeTaskMessages.Message("message_" + i, 2_999L + i))
      .toArray(CeTaskMessages.Message[]::new);
    when(uuidFactory.create()).thenAnswer(new Answer<String>() {
      int i = 0;

      @Override
      public String answer(InvocationOnMock invocation) {
        return uuids[i++];
      }
    });

    underTest.addAll(Arrays.stream(messages).collect(Collectors.toList()));

    assertThat(dbTester.select("select uuid as \"UUID\", task_uuid as \"TASK_UUID\", message as \"MESSAGE\", created_at as \"CREATED_AT\" from ce_task_message"))
      .extracting(t -> t.get("UUID"), t -> t.get("TASK_UUID"), t -> t.get("MESSAGE"), t -> t.get("CREATED_AT"))
      .containsOnly(
        tuple(uuids[0], taskUuid, messages[0].getText(), messages[0].getTimestamp()),
        tuple(uuids[1], taskUuid, messages[1].getText(), messages[1].getTimestamp()),
        tuple(uuids[2], taskUuid, messages[2].getText(), messages[2].getTimestamp()),
        tuple(uuids[3], taskUuid, messages[3].getText(), messages[3].getTimestamp()),
        tuple(uuids[4], taskUuid, messages[4].getText(), messages[4].getTimestamp()));
  }

  private void expectMessageCantBeNullNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("message can't be null");
  }
}
