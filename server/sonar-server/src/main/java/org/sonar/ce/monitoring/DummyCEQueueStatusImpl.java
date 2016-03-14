/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.ce.monitoring;

import java.util.concurrent.atomic.AtomicLong;

/**
 * FIXME fix this dummy CEQueueStatus implementation, probably by removing its use from
 */
public class DummyCEQueueStatusImpl implements CEQueueStatus {
  private final AtomicLong received = new AtomicLong(0);

  @Override
  public long initPendingCount(long initialPendingCount) {
    return notImplemented();
  }

  @Override
  public long addReceived() {
    return received.incrementAndGet();
  }

  @Override
  public long addReceived(long numberOfReceived) {
    return received.addAndGet(numberOfReceived);
  }

  @Override
  public long addInProgress() {
    return notImplemented();
  }

  @Override
  public long addSuccess(long processingTime) {
    return notImplemented();
  }

  @Override
  public long addError(long processingTime) {
    return notImplemented();
  }

  @Override
  public long getReceivedCount() {
    return received.get();
  }

  @Override
  public long getPendingCount() {
    return notImplemented();
  }

  @Override
  public long getInProgressCount() {
    return notImplemented();
  }

  @Override
  public long getErrorCount() {
    return notImplemented();
  }

  @Override
  public long getSuccessCount() {
    return notImplemented();
  }

  @Override
  public long getProcessingTime() {
    return notImplemented();
  }

  private static long notImplemented() {
    throw new UnsupportedOperationException("Not implemented!");
  }
}
