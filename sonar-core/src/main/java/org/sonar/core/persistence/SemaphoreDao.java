/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang.time.DateUtils;
import org.apache.ibatis.session.SqlSession;

import java.util.Date;

/**
 * @since 3.4
 */
public class SemaphoreDao {

  private final MyBatis mybatis;

  public SemaphoreDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public boolean lock(String name, int durationInSeconds) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Semaphore name must not be empty");
    Preconditions.checkArgument(durationInSeconds > 0, "Semaphore duration must be positive");

    SqlSession session = mybatis.openSession();
    try {
      SemaphoreMapper mapper = session.getMapper(SemaphoreMapper.class);
      initialize(name, session, mapper);
      return doLock(name, durationInSeconds, session, mapper);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void unlock(String name) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Semaphore name must not be empty");
    SqlSession session = mybatis.openSession();
    try {
      session.getMapper(SemaphoreMapper.class).unlock(name);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private boolean doLock(String name, int durationInSeconds, SqlSession session, SemaphoreMapper mapper) {
    Date lockedBefore = DateUtils.addSeconds(mapper.now(), -durationInSeconds);
    boolean ok = mapper.lock(name, lockedBefore) == 1;
    session.commit();
    return ok;
  }

  private void initialize(String name, SqlSession session, SemaphoreMapper mapper) {
    try {
      mapper.initialize(name, org.sonar.api.utils.DateUtils.parseDate("2001-01-01"));
      session.commit();

    } catch (Exception e) {
      // probably because of the semaphore already exists
    }
  }
}
