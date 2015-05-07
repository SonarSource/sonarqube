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

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import java.util.Collections;
import java.util.List;

/**
 * @since 3.7.1
 */
@BatchSide
@ServerSide
public class NotificationQueueDao {

  private final MyBatis mybatis;

  public NotificationQueueDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public void insert(List<NotificationQueueDto> dtos) {
    DbSession session = mybatis.openSession(true);
    NotificationQueueMapper mapper = session.getMapper(NotificationQueueMapper.class);
    try {
      for (NotificationQueueDto dto : dtos) {
        mapper.insert(dto);
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void delete(List<NotificationQueueDto> dtos) {
    DbSession session = mybatis.openSession(true);
    NotificationQueueMapper mapper = session.getMapper(NotificationQueueMapper.class);
    try {
      for (NotificationQueueDto dto : dtos) {
        mapper.delete(dto.getId());
      }
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<NotificationQueueDto> findOldest(int count) {
    if (count < 1) {
      return Collections.emptyList();
    }
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(NotificationQueueMapper.class).findOldest(count);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public long count() {
    SqlSession session = mybatis.openSession(false);
    try {
      return session.getMapper(NotificationQueueMapper.class).count();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
