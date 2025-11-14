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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class DistributedLock implements Lock {
  private final IMap<String, UUID> lockMap;
  private final String lockName;
  private final UUID ownerId;

  public DistributedLock(IMap<String, UUID> lockMap, String lockName) {
    this.lockMap = lockMap;
    this.lockName = lockName;
    // Unique ID for this lock owner
    this.ownerId = UUID.randomUUID();
  }

  @Override
  public void lock() {
    while (!tryLock()) {
      try {
        // Retry after a short delay
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Thread interrupted while acquiring lock");
      }
    }
  }

  @Override
  public void unlock() {
    lockMap.computeIfPresent(lockName, (key, value) -> value.equals(ownerId) ? null : value);
  }

  @Override
  public boolean tryLock() {
    return lockMap.putIfAbsent(lockName, ownerId) == null;
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    long deadline = System.currentTimeMillis() + unit.toMillis(time);
    while (System.currentTimeMillis() < deadline) {
      if (tryLock()) {
        return true;
      }
      // Retry after a short delay
      Thread.sleep(10);
    }
    return false;
  }

  @Override
  public Condition newCondition() {
    throw new UnsupportedOperationException("Conditions are not supported in DistributedLock");
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    throw new UnsupportedOperationException("Interruptible locking is not supported in DistributedLock");
  }
}
