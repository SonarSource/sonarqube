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
package org.sonar.db.notification;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.notification.NotificationQueueDto.toNotificationQueueDto;

public class NotificationQueueDaoIT {

  private final System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  private NotificationQueueDao dao = db.getDbClient().notificationQueueDao();

  @Test
  public void should_insert_new_notification_queue() throws Exception {
    NotificationQueueDto notificationQueueDto = toNotificationQueueDto(new Notification("email"));

    dao.insert(Arrays.asList(notificationQueueDto));

    assertThat(dao.count()).isOne();
    assertThat(dao.selectOldest(1).get(0).toNotification().getType()).isEqualTo("email");
  }

  @Test
  public void should_count_notification_queue() {
    NotificationQueueDto notificationQueueDto = toNotificationQueueDto(new Notification("email"));

    assertThat(dao.count()).isZero();

    dao.insert(Arrays.asList(notificationQueueDto));

    assertThat(dao.count()).isOne();
  }

  @Test
  public void should_delete_notification() {
    List<NotificationQueueDto> notifs = IntStream.range(0, 30)
      .mapToObj(i -> toNotificationQueueDto(new Notification("foo_" + i)))
      .collect(toList());
    dao.insert(notifs);
    db.commit();

    List<String> uuids = selectAllUuid();

    dao.delete(uuids.stream().limit(10).map(uuid -> new NotificationQueueDto().setUuid(uuid)).collect(toList()));

    assertThat(selectAllUuid()).containsOnly(uuids.stream().skip(10).toArray(String[]::new));
  }

  @Test
  public void should_findOldest() {
    when(system2.now()).thenAnswer(new Answer<Long>() {
      private long counter;

      @Override
      public Long answer(InvocationOnMock invocationOnMock) {
        counter++;
        return counter;
      }
    });

    List<NotificationQueueDto> notifs = IntStream.range(0, 5)
      .mapToObj(i -> toNotificationQueueDto(new Notification("foo_" + i)))
      .collect(toList());
    dao.insert(notifs);
    db.commit();

    assertThat(dao.selectOldest(3))
      .extracting(NotificationQueueDto::getUuid)
      .containsExactlyElementsOf(Arrays.asList("1", "2", "3"));
  }

  private List<String> selectAllUuid() {
    return db.select("select uuid as \"UUID\" from notifications order by created_at asc").stream()
      .map(t -> (String) t.get("UUID"))
      .collect(toList());
  }
}
