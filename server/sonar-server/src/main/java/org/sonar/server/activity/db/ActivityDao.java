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
package org.sonar.server.activity.db;

import org.sonar.api.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.core.activity.db.ActivityMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import java.util.Date;

@ServerSide
public class ActivityDao implements DaoComponent {

  private final MyBatis mybatis;
  private final System2 system;

  public ActivityDao(MyBatis mybatis, System2 system) {
    this.mybatis = mybatis;
    this.system = system;
  }

  public void insert(ActivityDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      insert(session, dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(DbSession session, ActivityDto dto) {
    dto.setCreatedAt(new Date(system.now()));
    session.getMapper(ActivityMapper.class).insert(dto);
  }

}
