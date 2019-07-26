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
package org.sonar.db.notification;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.notification.NotificationQueueDto.toNotificationQueueDto;

public class NotificationQueueDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private NotificationQueueDao dao = db.getDbClient().notificationQueueDao();

  @Test
  public void should_insert_new_notification_queue() throws Exception {
    NotificationQueueDto notificationQueueDto = toNotificationQueueDto(new Notification("email"));

    dao.insert(Arrays.asList(notificationQueueDto));

    assertThat(dao.count()).isEqualTo(1);
    assertThat(dao.selectOldest(1).get(0).toNotification().getType()).isEqualTo("email");
  }

  @Test
  public void should_count_notification_queue() {
    NotificationQueueDto notificationQueueDto = toNotificationQueueDto(new Notification("email"));

    assertThat(dao.count()).isEqualTo(0);

    dao.insert(Arrays.asList(notificationQueueDto));

    assertThat(dao.count()).isEqualTo(1);
  }

  @Test
  public void should_delete_notification() {
    List<NotificationQueueDto> notifs = IntStream.range(0, 30 + new Random().nextInt(20))
      .mapToObj(i -> toNotificationQueueDto(new Notification("foo_" + i)))
      .collect(toList());
    dao.insert(notifs);
    db.commit();

    List<Long> ids = selectAllIds();

    dao.delete(ids.stream().limit(10).map(id -> new NotificationQueueDto().setId(id)).collect(toList()));

    assertThat(selectAllIds()).containsOnly(ids.stream().skip(10).toArray(Long[]::new));
  }

  @Test
  public void should_findOldest() {
    List<NotificationQueueDto> notifs = IntStream.range(0, 20)
      .mapToObj(i -> toNotificationQueueDto(new Notification("foo_" + i)))
      .collect(toList());
    dao.insert(notifs);
    db.commit();

    List<Long> ids = selectAllIds();

    assertThat(dao.selectOldest(3))
      .extracting(NotificationQueueDto::getId)
      .containsOnly(ids.stream().limit(3).toArray(Long[]::new));

    assertThat(dao.selectOldest(22))
      .extracting(NotificationQueueDto::getId)
      .containsOnly(ids.toArray(new Long[0]));
  }

  private List<Long> selectAllIds() {
    return db.select("select id as \"ID\" from notifications").stream()
      .map(t -> (Long) t.get("ID"))
      .collect(toList());
  }
}
