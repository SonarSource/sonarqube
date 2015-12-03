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
package org.sonar.server.computation.monitoring;

import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class CEQueueStatusImpl implements CEQueueStatus {
  private static final long PENDING_INITIAL_VALUE = Long.MIN_VALUE;

  private final AtomicLong received = new AtomicLong(0);
  private final AtomicLong pending = new AtomicLong(PENDING_INITIAL_VALUE);
  private final AtomicLong inProgress = new AtomicLong(0);
  private final AtomicLong error = new AtomicLong(0);
  private final AtomicLong success = new AtomicLong(0);
  private final AtomicLong processingTime = new AtomicLong(0);

  @Override
  public long initPendingCount(long initialPendingCount) {
    checkArgument(initialPendingCount >= 0, "Initial pending count must be >= 0");
    checkState(
      pending.compareAndSet(PENDING_INITIAL_VALUE, initialPendingCount),
      "Method initPendingCount must be used before any other method and can not be called twice");
    return initialPendingCount;
  }

  @Override
  public long addReceived() {
    ensurePendingInitialized("addReceived");

    pending.incrementAndGet();
    return received.incrementAndGet();
  }

  @Override
  public long addInProgress() {
    ensurePendingInitialized("addInProgress");

    pending.decrementAndGet();
    return inProgress.incrementAndGet();
  }

  private void ensurePendingInitialized(String methodName) {
    checkState(pending.get() != PENDING_INITIAL_VALUE, "Method initPendingCount must be used before %s can be called", methodName);
  }

  @Override
  public long addError(long processingTime) {
    addProcessingTime(processingTime);
    inProgress.decrementAndGet();
    return error.incrementAndGet();
  }

  @Override
  public long addSuccess(long processingTime) {
    addProcessingTime(processingTime);
    inProgress.decrementAndGet();
    return success.incrementAndGet();
  }

  private void addProcessingTime(long time) {
    checkArgument(time >= 0, "Processing time can not be < 0");
    processingTime.addAndGet(time);
  }

  @Override
  public long getReceivedCount() {
    return received.get();
  }

  @Override
  public long getPendingCount() {
    long currentValue = pending.get();
    return currentValue == PENDING_INITIAL_VALUE ? 0 : currentValue;
  }

  @Override
  public long getInProgressCount() {
    return inProgress.get();
  }

  @Override
  public long getErrorCount() {
    return error.get();
  }

  @Override
  public long getSuccessCount() {
    return success.get();
  }

  @Override
  public long getProcessingTime() {
    return processingTime.get();
  }
}
