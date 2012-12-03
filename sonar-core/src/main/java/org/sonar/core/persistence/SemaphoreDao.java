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

  public Lock acquire(String name, int maxDurationInSeconds) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Semaphore name must not be empty");
    Preconditions.checkArgument(maxDurationInSeconds >= 0, "Semaphore max duration must be positive: " + maxDurationInSeconds);

    SqlSession session = mybatis.openSession();
    try {
      SemaphoreMapper mapper = session.getMapper(SemaphoreMapper.class);
      Date lockedAt = org.sonar.api.utils.DateUtils.parseDate("2001-01-01");
      createSemaphore(name, lockedAt, session, mapper);
      boolean isAcquired = doAcquire(name, maxDurationInSeconds, session, mapper);
      SemaphoreDto semaphore = selectSemaphore(name, session);
      return createLock(semaphore, mapper, isAcquired);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Lock acquire(String name) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Semaphore name must not be empty");

    SqlSession session = mybatis.openSession();
    try {
      SemaphoreMapper mapper = session.getMapper(SemaphoreMapper.class);
      SemaphoreDto semaphore = selectSemaphore(name, session);
      Date now = mapper.now();
      if (semaphore != null) {
        return createLock(semaphore, mapper, false);
      } else {
        semaphore = createSemaphore(name, now, session, mapper);
        return createLock(semaphore, mapper, true);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void release(String name) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "Semaphore name must not be empty");
    SqlSession session = mybatis.openSession();
    try {
      session.getMapper(SemaphoreMapper.class).release(name);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private boolean doAcquire(String name, int durationInSeconds, SqlSession session, SemaphoreMapper mapper) {
    Date lockedBefore = DateUtils.addSeconds(mapper.now(), -durationInSeconds);
    boolean ok = mapper.acquire(name, lockedBefore) == 1;
    session.commit();
    return ok;
  }

  private SemaphoreDto createSemaphore(String name, Date lockedAt, SqlSession session, SemaphoreMapper mapper) {
    try {
      SemaphoreDto semaphore = new SemaphoreDto()
          .setName(name)
          .setLockedAt(lockedAt);
      mapper.initialize(semaphore);
      session.commit();
      return semaphore;
    } catch (Exception e) {
      // probably because of the semaphore already exists
      session.rollback();
      return null;
    }
  }

  private Lock createLock(SemaphoreDto semaphore, SemaphoreMapper mapper, boolean acquired) {
    Lock lock = new Lock(semaphore.getName(), acquired, semaphore.getLockedAt(), semaphore.getCreatedAt(), semaphore.getUpdatedAt());
    if (!acquired) {
      lock.setDurationSinceLocked(getDurationSinceLocked(semaphore, mapper));
    }
    return lock;
  }

  private long getDurationSinceLocked(SemaphoreDto semaphore, SemaphoreMapper mapper) {
    long now = mapper.now().getTime();
    semaphore.getLockedAt();
    long locketAt = semaphore.getLockedAt().getTime();
    return now - locketAt;
  }

  protected SemaphoreDto selectSemaphore(String name, SqlSession session){
    SemaphoreMapper mapper = session.getMapper(SemaphoreMapper.class);
    return mapper.selectSemaphore(name);
  }

}
