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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.Semaphores;
import org.sonar.api.utils.System2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class SemaphoreDaoTest extends AbstractDaoTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private SemaphoreDao dao;
  private System2 system;

  @Before
  public void before() {
    system = System2.INSTANCE;
    dao = new SemaphoreDao(getMyBatis(), system);
  }

  @Test
  public void should_fail_to_acquire_if_null_semaphore_name() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Semaphore name must not be empty");

    SemaphoreDao dao = new SemaphoreDao(getMyBatis(), system);
    dao.acquire(null, 5000);
  }

  @Test
  public void should_fail_to_acquire_if_blank_semaphore_name() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Semaphore name must not be empty");

    SemaphoreDao dao = new SemaphoreDao(getMyBatis(), system);
    dao.acquire("", 5000);
  }

  @Test
  public void should_fail_to_acquire_if_negative_timeout() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Semaphore max age must be positive: -5000");

    SemaphoreDao dao = new SemaphoreDao(getMyBatis(), system);
    dao.acquire("foo", -5000);
  }

  @Test
  public void should_fail_to_release_if_blank_semaphore_name() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Semaphore name must not be empty");

    SemaphoreDao dao = new SemaphoreDao(getMyBatis(), system);
    dao.release(null);
  }

  @Test
  public void create_and_acquire_semaphore() throws Exception {
    Semaphores.Semaphore lock = dao.acquire("foo", 60);
    assertThat(lock.isLocked()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt())).isTrue();
    assertThat(isRecent(semaphore.getUpdatedAt())).isTrue();
    assertThat(isRecent(semaphore.getLockedAt())).isTrue();

    dao.release("foo");
    assertThat(selectSemaphore("foo")).isNull();
  }

  @Test
  public void create_and_acquire_and_update_semaphore() throws Exception {
    Semaphores.Semaphore lock = dao.acquire("foo", 60);
    assertThat(lock.isLocked()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore.getCreatedAt()).isEqualTo(semaphore.getUpdatedAt());

    Thread.sleep(1000);

    dao.update(lock);

    semaphore = selectSemaphore("foo");
    assertThat(semaphore.getCreatedAt()).isLessThan(semaphore.getUpdatedAt());

    dao.release("foo");
    assertThat(selectSemaphore("foo")).isNull();
  }

  @Test
  public void fail_to_update_null_semaphore() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Semaphore must not be null");

    dao.update(null);
  }

  @Test
  public void create_and_acquire_semaphore_when_maxage_is_zeo() throws Exception {
    Semaphores.Semaphore lock = dao.acquire("foo", 0);
    assertThat(lock.isLocked()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt())).isTrue();
    assertThat(isRecent(semaphore.getUpdatedAt())).isTrue();
    assertThat(isRecent(semaphore.getLockedAt())).isTrue();

    dao.release("foo");
    assertThat(selectSemaphore("foo")).isNull();
  }

  @Test
  public void create_and_acquire_semaphore_when_no_timeout() throws Exception {
    Semaphores.Semaphore lock = dao.acquire("foo");
    assertThat(lock.isLocked()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt())).isTrue();
    assertThat(isRecent(semaphore.getUpdatedAt())).isTrue();
    assertThat(isRecent(semaphore.getLockedAt())).isTrue();

    dao.release("foo");
    assertThat(selectSemaphore("foo")).isNull();
  }

  @Test
  public void fail_to_acquire_locked_semaphore() throws Exception {
    setupData("old_semaphore");
    Semaphores.Semaphore lock = dao.acquire("foo", Integer.MAX_VALUE);
    assertThat(lock.isLocked()).isFalse();
    assertThat(lock.getDurationSinceLocked()).isNotNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt())).isFalse();
    assertThat(isRecent(semaphore.getUpdatedAt())).isFalse();
    assertThat(isRecent(semaphore.getLockedAt())).isFalse();
  }

  @Test
  public void acquire_long_locked_semaphore() throws Exception {
    setupData("old_semaphore");
    Semaphores.Semaphore lock = dao.acquire("foo", 60);
    assertThat(lock.isLocked()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt())).isFalse();
    assertThat(isRecent(semaphore.getUpdatedAt())).isTrue();
    assertThat(isRecent(semaphore.getLockedAt())).isTrue();
  }

  @Test
  public void acquire_locked_semaphore_when_timeout_is_zero() throws Exception {
    setupData("old_semaphore");
    Semaphores.Semaphore lock = dao.acquire("foo", 0);
    assertThat(lock.isLocked()).isTrue();
    assertThat(lock.getDurationSinceLocked()).isNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt())).isFalse();
    assertThat(isRecent(semaphore.getUpdatedAt())).isTrue();
    assertThat(isRecent(semaphore.getLockedAt())).isTrue();

    dao.release("foo");
    assertThat(selectSemaphore("foo")).isNull();
  }

  @Test
  public void fail_to_acquire_locked_semaphore_when_no_timeout() throws Exception {
    setupData("old_semaphore");
    Semaphores.Semaphore lock = dao.acquire("foo");
    assertThat(lock.isLocked()).isFalse();
    assertThat(lock.getDurationSinceLocked()).isNotNull();

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(isRecent(semaphore.getCreatedAt())).isFalse();
    assertThat(isRecent(semaphore.getUpdatedAt())).isFalse();
    assertThat(isRecent(semaphore.getLockedAt())).isFalse();
  }

  @Test
  public void should_select_semaphore_return_current_semaphore_when_acquiring() throws Exception {
    dao.acquire("foo");

    SemaphoreDto semaphore = selectSemaphore("foo");
    assertThat(semaphore).isNotNull();
    assertThat(semaphore.getName()).isEqualTo("foo");
    assertThat(semaphore.getCreatedAt()).isNotNull();
    assertThat(semaphore.getUpdatedAt()).isNotNull();
    assertThat(semaphore.getLockedAt()).isNotNull();
  }

  @Test
  public void test_concurrent_locks() throws Exception {
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
      return dao.selectSemaphore(name, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private boolean isRecent(Long date) {
    int oneMinuteInMs = 60 * 1000;
    long future = system.now() + oneMinuteInMs;
    long past = system.now() - oneMinuteInMs;
    return date > past && date < future;
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

    @Override
    public void run() {
      try {
        barrier.await();
        for (int i = 0; i < 100; i++) {
          if (dao.acquire("my-lock", 60 * 60 * 24).isLocked()) {
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
