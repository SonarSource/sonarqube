/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.notification.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationQueueDaoTest extends AbstractDaoTestCase {

  NotificationQueueDao dao;

  @Before
  public void createDao() {
    dao = new NotificationQueueDao(getMyBatis());
  }

  @Test
  public void should_insert_new_notification_queue() throws Exception {
    NotificationQueueDto notificationQueueDto = NotificationQueueDto.toNotificationQueueDto(new Notification("email"));

    dao.insert(Arrays.asList(notificationQueueDto));

    assertThat(dao.count()).isEqualTo(1);
    assertThat(dao.findOldest(1).get(0).toNotification().getType()).isEqualTo("email");
  }

  @Test
  public void should_count_notification_queue() {
    NotificationQueueDto notificationQueueDto = NotificationQueueDto.toNotificationQueueDto(new Notification("email"));

    assertThat(dao.count()).isEqualTo(0);

    dao.insert(Arrays.asList(notificationQueueDto));

    assertThat(dao.count()).isEqualTo(1);
  }

  @Test
  public void should_delete_notification() {
    setupData("should_delete_notification");

    NotificationQueueDto dto1 = new NotificationQueueDto().setId(1L);
    NotificationQueueDto dto3 = new NotificationQueueDto().setId(3L);

    dao.delete(Arrays.asList(dto1, dto3));

    checkTables("should_delete_notification", "notifications");
  }

  @Test
  public void should_findOldest() {
    setupData("should_findOldest");

    Collection<NotificationQueueDto> result = dao.findOldest(3);
    assertThat(result).hasSize(3);
    assertThat(result).extracting("id").containsOnly(1L, 2L, 3L);

    result = dao.findOldest(6);
    assertThat(result).hasSize(4);
  }
}
