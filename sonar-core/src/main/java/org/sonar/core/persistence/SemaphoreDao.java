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
package org.sonar.core.persistence;

import com.google.common.base.Strings;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.utils.Semaphores;
import org.sonar.api.utils.System2;

import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.utils.DateUtils.longToDate;

/**
 * @since 3.4
 */
public class SemaphoreDao {

  private static final String SEMAPHORE_NAME_MUST_NOT_BE_EMPTY = "Semaphore name must not be empty";
  private final MyBatis mybatis;
  private final System2 system;

  public SemaphoreDao(MyBatis mybatis, System2 system) {
    this.mybatis = mybatis;
    this.system = system;
  }

  public Semaphores.Semaphore acquire(String name, int maxAgeInSeconds) {
    checkArgument(!Strings.isNullOrEmpty(name), SEMAPHORE_NAME_MUST_NOT_BE_EMPTY);
    checkArgument(maxAgeInSeconds >= 0, "Semaphore max age must be positive: " + maxAgeInSeconds);

    try (SqlSession session = mybatis.openSession(false)) {
      SemaphoreDto semaphore = tryToInsert(name, system.now(), session);
      boolean isAcquired;
      if (semaphore == null) {
        semaphore = selectSemaphore(name, session);
        isAcquired = acquireIfOutdated(name, maxAgeInSeconds, session);
      } else {
        isAcquired = true;
      }
      return createLock(semaphore, isAcquired);
    }
  }

  public Semaphores.Semaphore acquire(String name) {
    checkArgument(!Strings.isNullOrEmpty(name), SEMAPHORE_NAME_MUST_NOT_BE_EMPTY);

    try (SqlSession session = mybatis.openSession(false)) {
      SemaphoreDto semaphore = tryToInsert(name, system.now(), session);
      if (semaphore == null) {
        semaphore = selectSemaphore(name, session);
        return createLock(semaphore, false);
      } else {
        return createLock(semaphore, true);
      }
    }
  }

  public void update(Semaphores.Semaphore semaphore) {
    checkArgument(semaphore != null, "Semaphore must not be null");

    try (SqlSession session = mybatis.openSession(false)) {
      mapper(session).update(semaphore.getName(), system.now());
      session.commit();
    }
  }

  public void release(String name) {
    checkArgument(!Strings.isNullOrEmpty(name), SEMAPHORE_NAME_MUST_NOT_BE_EMPTY);
    try (SqlSession session = mybatis.openSession(false)) {
      mapper(session).release(name);
      session.commit();
    }
  }

  private boolean acquireIfOutdated(String name, int maxAgeInSeconds, SqlSession session) {
    long now = system.now();
    long updatedBefore = now - (long) maxAgeInSeconds * 1000;

    boolean ok = mapper(session).acquire(name, updatedBefore, now) == 1;
    session.commit();
    return ok;
  }

  /**
   * Insert the semaphore and commit. Rollback and return null if the semaphore already exists in db (whatever
   * the lock date)
   */
  @CheckForNull
  private SemaphoreDto tryToInsert(String name, long lockedNow, SqlSession session) {
    try {
      long now = system.now();
      SemaphoreDto semaphore = new SemaphoreDto()
        .setName(name)
        .setCreatedAt(now)
        .setUpdatedAt(now)
        .setLockedAt(lockedNow);
      mapper(session).initialize(semaphore);
      session.commit();
      return semaphore;
    } catch (Exception e) {
      // probably because the semaphore already exists in db
      session.rollback();
      return null;
    }
  }

  private Semaphores.Semaphore createLock(SemaphoreDto dto, boolean acquired) {
    Semaphores.Semaphore semaphore = new Semaphores.Semaphore()
      .setName(dto.getName())
      .setLocked(acquired)
      .setLockedAt(longToDate(dto.getLockedAt()))
      .setCreatedAt(longToDate(dto.getCreatedAt()))
      .setUpdatedAt(longToDate(dto.getUpdatedAt()));
    if (!acquired) {
      semaphore.setDurationSinceLocked(lockedSince(dto));
    }
    return semaphore;
  }

  private long lockedSince(SemaphoreDto semaphore) {
    return system.now() - semaphore.getLockedAt();
  }

  protected SemaphoreDto selectSemaphore(String name, SqlSession session) {
    return mapper(session).selectSemaphore(name);
  }

  private SemaphoreMapper mapper(SqlSession session) {
    return session.getMapper(SemaphoreMapper.class);
  }
}
