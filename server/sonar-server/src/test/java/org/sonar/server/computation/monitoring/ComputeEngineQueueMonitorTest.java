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

import org.junit.Test;
import org.sonar.server.computation.queue.CeQueueImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

public class ComputeEngineQueueMonitorTest {
  private static final long RECEIVED_COUNT = 30;
  private static final long PENDING_COUNT = 2;
  private static final long IN_PROGRESS_COUNT = 5;
  private static final long ERROR_COUNT = 10;
  private static final long SUCCESS_COUNT = 13;
  private static final long PROCESSING_TIME = 987;

  private ComputeEngineQueueMonitor underTest = new ComputeEngineQueueMonitor(new DumbCEQueueStatus(), mock(CeQueueImpl.class));

  @Test
  public void name_is_ComputeEngineQueue() {
    assertThat(underTest.name()).isEqualTo("ComputeEngineQueue");
  }

  @Test
  public void attributes_has_entry_for_each_get_method() {
    assertThat(underTest.attributes()).containsOnly(
      entry("Received", RECEIVED_COUNT),
      entry("Pending", PENDING_COUNT),
      entry("In progress", IN_PROGRESS_COUNT),
      entry("Successfully processed", SUCCESS_COUNT),
      entry("Processed with error", ERROR_COUNT),
      entry("Processing time", PROCESSING_TIME));
  }

  @Test
  public void get_methods_delegate_to_the_CEQueueStatus_instance() {
    assertThat(underTest.getReceivedCount()).isEqualTo(RECEIVED_COUNT);
    assertThat(underTest.getPendingCount()).isEqualTo(PENDING_COUNT);
    assertThat(underTest.getInProgressCount()).isEqualTo(IN_PROGRESS_COUNT);
    assertThat(underTest.getErrorCount()).isEqualTo(ERROR_COUNT);
    assertThat(underTest.getSuccessCount()).isEqualTo(SUCCESS_COUNT);
    assertThat(underTest.getProcessingTime()).isEqualTo(PROCESSING_TIME);
  }

  /**
   * Dumb implementation of CEQueueStatus which returns constant values for get methods and throws UnsupportedOperationException
   * for other methods.
   */
  private static class DumbCEQueueStatus implements CEQueueStatus {

    @Override
    public long addReceived() {
      return methodNotImplemented();
    }

    private long methodNotImplemented() {
      throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public long getReceivedCount() {
      return RECEIVED_COUNT;
    }

    @Override
    public long initPendingCount(long initialPendingCount) {
      return methodNotImplemented();
    }

    @Override
    public long getPendingCount() {
      return PENDING_COUNT;
    }

    @Override
    public long addInProgress() {
      return methodNotImplemented();
    }

    @Override
    public long getInProgressCount() {
      return IN_PROGRESS_COUNT;
    }

    @Override
    public long addError(long processingTime) {
      return methodNotImplemented();
    }

    @Override
    public long getErrorCount() {
      return ERROR_COUNT;
    }

    @Override
    public long addSuccess(long processingTime) {
      return methodNotImplemented();
    }

    @Override
    public long getSuccessCount() {
      return SUCCESS_COUNT;
    }

    @Override
    public long getProcessingTime() {
      return PROCESSING_TIME;
    }
  }
}
