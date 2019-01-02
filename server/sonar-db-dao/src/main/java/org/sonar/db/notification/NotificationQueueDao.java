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

import java.util.Collections;
import java.util.List;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class NotificationQueueDao implements Dao {

  private final MyBatis mybatis;

  public NotificationQueueDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void insert(List<NotificationQueueDto> dtos) {
    try (DbSession session = mybatis.openSession(true)) {
      NotificationQueueMapper mapper = session.getMapper(NotificationQueueMapper.class);
      for (NotificationQueueDto dto : dtos) {
        mapper.insert(dto);
      }
      session.commit();
    }
  }

  public void delete(List<NotificationQueueDto> dtos) {
    try (DbSession session = mybatis.openSession(true)) {
      NotificationQueueMapper mapper = session.getMapper(NotificationQueueMapper.class);
      for (NotificationQueueDto dto : dtos) {
        mapper.delete(dto.getId());
      }
      session.commit();
    }
  }

  public List<NotificationQueueDto> selectOldest(int count) {
    if (count < 1) {
      return Collections.emptyList();
    }
    try (DbSession session = mybatis.openSession(false)) {
      return session.getMapper(NotificationQueueMapper.class).findOldest(count);
    }
  }

  public long count() {
    try (DbSession session = mybatis.openSession(false)) {
      return session.getMapper(NotificationQueueMapper.class).count();
    }
  }
}
