/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.ce;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.sonar.ce.taskprocessor.CeWorkerFactory;

import static com.google.common.base.Preconditions.checkState;

/**
 * Provide the set of worker's UUID in a non clustered SonarQube instance
 */
public class StandaloneCeDistributedInformation implements CeDistributedInformation {
  private final CeWorkerFactory ceCeWorkerFactory;
  private Set<String> workerUUIDs;

  private Lock cleanJobLock = new NonConcurrentLock();

  public StandaloneCeDistributedInformation(CeWorkerFactory ceCeWorkerFactory) {
    this.ceCeWorkerFactory = ceCeWorkerFactory;
  }

  @Override
  public Set<String> getWorkerUUIDs() {
    checkState(workerUUIDs != null, "Invalid call, broadcastWorkerUUIDs() must be called first.");
    return workerUUIDs;
  }

  @Override
  public void broadcastWorkerUUIDs() {
    workerUUIDs = ceCeWorkerFactory.getWorkerUUIDs();
  }

  /**
   * Since StandaloneCeDistributedInformation in fact does not provide any distribution support, the lock returned by
   * this method provides no concurrency support at all.
   */
  @Override
  public Lock acquireCleanJobLock() {
    return cleanJobLock;
  }

  private static class NonConcurrentLock implements Lock {
    @Override
    public void lock() {
      // return immediately and never block
    }

    @Override
    public void lockInterruptibly() {
      // return immediately and never block
    }

    @Override
    public boolean tryLock() {
      // always succeed
      return true;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
      // always succeed
      return true;
    }

    @Override
    public void unlock() {
      // nothing to do
    }

    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException("newCondition not supported");
    }
  }
}
