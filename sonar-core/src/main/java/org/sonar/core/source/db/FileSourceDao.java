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

package org.sonar.core.source.db;

import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

public class FileSourceDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public FileSourceDao(MyBatis myBatis) {
    this.mybatis = myBatis;
  }

  @CheckForNull
  public FileSourceDto select(String fileUuid) {
    DbSession session = mybatis.openSession(false);
    try {
      FileSourceMapper mapper = session.getMapper(FileSourceMapper.class);
      return mapper.select(fileUuid);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(FileSourceDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      session.getMapper(FileSourceMapper.class).insert(dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(FileSourceDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      session.getMapper(FileSourceMapper.class).update(dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public String selectLineHashes(String fileUuid, DbSession session) {
    return session.getMapper(FileSourceMapper.class).selectLineHashes(fileUuid);
  }
}
