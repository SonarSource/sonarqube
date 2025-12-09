/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.process.cluster.hz;

import com.hazelcast.map.IMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistributedLockTest {

  private static final String LOCK_NAME = "lockName";

  private final IMap<String, UUID> map = new MockIMap<>();
  private final DistributedLock underTest = new DistributedLock(map, LOCK_NAME);


  @Test
  void lock() {
    underTest.lock();

    assertThat(map.get(LOCK_NAME)).isNotNull();

    underTest.unlock();
  }

  @Test
  void lock_whenAlreadyLocked_WaitsUntilUnlocked() throws InterruptedException {

    Thread thread1 = new Thread(() -> {
      underTest.lock();
      try {
        // Simulate some work
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        underTest.unlock();
      }
    });

    Thread thread2 = new Thread(() -> {
      underTest.lock();
      try {
        // Simulate some work
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        underTest.unlock();
      }
    });

    thread1.start();
    thread2.start();

    thread1.join();
    thread2.join();
    assertThat(map.get(LOCK_NAME)).isNull();
  }

  @Test
  void lock_InterruptedThread_throwsException() throws InterruptedException {

    Thread thread1 = new Thread(() -> {
      underTest.lock();
      assertThat(map.tryLock(LOCK_NAME)).isFalse();
      try {
        // Simulate some work
        Thread.sleep(300);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        underTest.unlock();
      }
    });

    Thread thread2 = new Thread(() -> {
      try {
        underTest.lock();
        assertThat(map.tryLock(LOCK_NAME)).isFalse();
        // Simulate some work
      } catch (Exception e) {
        assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Thread interrupted while acquiring lock");
      } finally {
        underTest.unlock();
        assertThat(map.get(LOCK_NAME)).isNull();
      }
    });

    thread1.start();
    thread2.start();
    thread2.interrupt();

    thread1.join();
    thread2.join();
  }

  @Test
  void unlock() {
    underTest.lock();

    assertThat(map.get(LOCK_NAME)).isNotNull();

    underTest.unlock();

    assertThat(map.get(LOCK_NAME)).isNull();
  }

  @Test
  void tryLock() {

    underTest.lock();

    assertThat(underTest.tryLock()).isFalse();

    underTest.unlock();

    assertThat(underTest.tryLock()).isTrue();
    assertThat(map.get(LOCK_NAME)).isNotNull();

    underTest.unlock();
  }

  @Test
  void tryLock_WithTimeout() throws InterruptedException {
    underTest.lock();

    assertThat(map.get(LOCK_NAME)).isNotNull();
    assertThat(underTest.tryLock(1, TimeUnit.SECONDS)).isFalse();

    underTest.unlock();

    assertThat(underTest.tryLock(1, TimeUnit.SECONDS)).isTrue();
    assertThat(map.get(LOCK_NAME)).isNotNull();

    underTest.unlock();
  }

  @Test
  void newCondition() {
    try {
      underTest.newCondition();
    } catch (UnsupportedOperationException e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  void lockInterruptibly() {
    try {
      underTest.lockInterruptibly();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
