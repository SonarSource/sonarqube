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

import org.apache.commons.lang.time.DateUtils;
import org.apache.ibatis.session.SqlSession;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.fest.assertions.Assertions.assertThat;

public class SemaphoreDaoTest extends AbstractDaoTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_fail_to_acquire_if_blank_semaphore_name() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Semaphore name must not be empty");

    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    dao.acquire(null, 5000);
  }

  @Test
  public void should_fail_to_acquire_if_negative_timeout() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Semaphore max duration must be positive: -5000");

    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    dao.acquire("foo", -5000);
  }

  @Test
  public void should_fail_to_release_if_blank_semaphore_name() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Semaphore name must not be empty");

    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    dao.release(null);
  }

  @Test
  public void create_and_acquire_semaphore() throws Exception {
    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    Lock lock = dao.acquire("foo", 60);
    assertThat(lock.isAcquired()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt(), 60)).isTrue();
    assertThat(isRecent(semaphore.getUpdatedAt(), 60)).isTrue();
    assertThat(isRecent(semaphore.getLockedAt(), 60)).isTrue();

    dao.release("foo");
    assertThat(selectSemaphore("foo")).isNull();
  }

  @Test
  public void create_and_acquire_semaphore_when_timeout_is_zeo() throws Exception {
    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    Lock lock = dao.acquire("foo", 0);
    assertThat(lock.isAcquired()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt(), 60)).isTrue();
    assertThat(isRecent(semaphore.getUpdatedAt(), 60)).isTrue();
    assertThat(isRecent(semaphore.getLockedAt(), 60)).isTrue();

    dao.release("foo");
    assertThat(selectSemaphore("foo")).isNull();
  }

  @Test
  public void create_and_acquire_semaphore_when_no_timeout() throws Exception {
    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    Lock lock = dao.acquire("foo");
    assertThat(lock.isAcquired()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt(), 60)).isTrue();
    assertThat(isRecent(semaphore.getUpdatedAt(), 60)).isTrue();
    assertThat(isRecent(semaphore.getLockedAt(), 60)).isTrue();

    dao.release("foo");
    assertThat(selectSemaphore("foo")).isNull();
  }

  @Test
  public void fail_to_acquire_locked_semaphore() throws Exception {
    setupData("old_semaphore");
    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    Lock lock = dao.acquire("foo", Integer.MAX_VALUE);
    assertThat(lock.isAcquired()).isFalse();
    assertThat(lock.getDurationSinceLocked()).isNotNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt(), 60)).isFalse();
    assertThat(isRecent(semaphore.getUpdatedAt(), 60)).isFalse();
    assertThat(isRecent(semaphore.getLockedAt(), 60)).isFalse();
  }

  @Test
  public void acquire_long_locked_semaphore() throws Exception {
    setupData("old_semaphore");
    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    Lock lock = dao.acquire("foo", 60);
    assertThat(lock.isAcquired()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt(), 60)).isFalse();
    assertThat(isRecent(semaphore.getUpdatedAt(), 60)).isTrue();
    assertThat(isRecent(semaphore.getLockedAt(), 60)).isTrue();
  }

  @Test
  public void acquire_locked_semaphore_when_timeout_is_zeo() throws Exception {
    setupData("old_semaphore");
    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    Lock lock = dao.acquire("foo", 0);
    assertThat(lock.isAcquired()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt(), 60)).isFalse();
    assertThat(isRecent(semaphore.getUpdatedAt(), 60)).isTrue();
    assertThat(isRecent(semaphore.getLockedAt(), 60)).isTrue();

    dao.release("foo");
    assertThat(selectSemaphore("foo")).isNull();
  }

  @Test
  public void fail_to_acquire_locked_semaphore_when_no_timeout() throws Exception {
    setupData("old_semaphore");
    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    Lock lock = dao.acquire("foo");
    assertThat(lock.isAcquired()).isFalse();
    assertThat(lock.getDurationSinceLocked()).isNotNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt(), 60)).isFalse();
    assertThat(isRecent(semaphore.getUpdatedAt(), 60)).isFalse();
    assertThat(isRecent(semaphore.getLockedAt(), 60)).isFalse();
  }

  @Test
  public void should_select_semaphore_return_current_semaphore_when_acquiring() throws Exception {
    SemaphoreDao dao = new SemaphoreDao(getMyBatis());
    dao.acquire("foo");

    SemaphoreDto semaphore = dao.selectSemaphore("foo", getMyBatis().openSession());
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(semaphore.getCreatedAt()).isNotNull();
    assertThat(semaphore.getUpdatedAt()).isNotNull();
    assertThat(semaphore.getLockedAt()).isNotNull();
  }

  @Test
  public void test_concurrent_locks() throws Exception {
    SemaphoreDao dao = new SemaphoreDao(getMyBatis());

    for (int tests = 0; tests < 5; tests++) {
      dao.release("my-lock");
      int size = 5;
      CyclicBarrier barrier = new CyclicBarrier(size);
      CountDownLatch latch = new CountDownLatch(size);

      AtomicInteger locks = new AtomicInteger(0);
      for (int i = 0; i < size; i++) {
        new Runner(dao, locks, barrier, latch).start();
      }
      latch.await();

      // semaphore was locked only 1 time
      assertThat(locks.get()).isEqualTo(1);
    }
  }

  private SemaphoreDto selectSemaphore(String name) throws Exception {
    SqlSession session = getMyBatis().openSession();
    try {
      SemaphoreDao dao = new SemaphoreDao(getMyBatis());
      return dao.selectSemaphore(name, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private boolean isRecent(Date date, int durationInSeconds) {
    SqlSession session = getMyBatis().openSession();
    try {
      SemaphoreDao dao = new SemaphoreDao(getMyBatis());
      Date now = dao.now(session);
      return date.before(now) && DateUtils.addSeconds(date, durationInSeconds).after(now);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static class Runner extends Thread {
    SemaphoreDao dao;
    AtomicInteger locks;
    CountDownLatch latch;
    CyclicBarrier barrier;

    Runner(SemaphoreDao dao, AtomicInteger atomicSeq, CyclicBarrier barrier, CountDownLatch latch) {
      this.dao = dao;
      this.locks = atomicSeq;
      this.latch = latch;
      this.barrier = barrier;
    }

    public void run() {
      try {
        barrier.await();
        for (int i = 0; i < 100; i++) {
          if (dao.acquire("my-lock", 60 * 5).isAcquired()) {
            locks.incrementAndGet();
          }
        }
        latch.countDown();

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
