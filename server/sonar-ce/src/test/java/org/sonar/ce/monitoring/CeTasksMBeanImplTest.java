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
package org.sonar.ce.monitoring;

import java.lang.management.ManagementFactory;
import javax.annotation.CheckForNull;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.junit.Test;
import org.sonar.ce.configuration.CeConfiguration;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class CeTasksMBeanImplTest {
  private static final long PENDING_COUNT = 2;
  private static final long IN_PROGRESS_COUNT = 5;
  private static final long ERROR_COUNT = 10;
  private static final long SUCCESS_COUNT = 13;
  private static final long PROCESSING_TIME = 987;
  private static final int WORKER_MAX_COUNT = 666;
  private static final int WORKER_COUNT = 56;

  private CeTasksMBeanImpl underTest = new CeTasksMBeanImpl(new DumbCEQueueStatus(), new DumbCeConfiguration());

  @Test
  public void register_and_unregister() throws Exception {
    assertThat(getMBean()).isNull();

    underTest.start();
    assertThat(getMBean()).isNotNull();

    underTest.stop();
    assertThat(getMBean()).isNull();
  }

  /**
   * Dumb implementation of CEQueueStatus which returns constant values for get methods and throws UnsupportedOperationException
   * for other methods.
   */
  @CheckForNull
  private ObjectInstance getMBean() throws Exception {
    try {
      return ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(CeTasksMBean.OBJECT_NAME));
    } catch (InstanceNotFoundException e) {
      return null;
    }
  }

  @Test
  public void get_methods_delegate_to_the_CEQueueStatus_instance() {
    assertThat(underTest.getPendingCount()).isEqualTo(PENDING_COUNT);
    assertThat(underTest.getInProgressCount()).isEqualTo(IN_PROGRESS_COUNT);
    assertThat(underTest.getErrorCount()).isEqualTo(ERROR_COUNT);
    assertThat(underTest.getSuccessCount()).isEqualTo(SUCCESS_COUNT);
    assertThat(underTest.getProcessingTime()).isEqualTo(PROCESSING_TIME);
  }

  @Test
  public void getWorkerCount_delegates_to_the_CEConfiguration_instance() {
    assertThat(underTest.getWorkerCount()).isEqualTo(WORKER_COUNT);
  }

  @Test
  public void getWorkerMaxCount_delegates_to_the_CEConfiguration_instance() {
    assertThat(underTest.getWorkerMaxCount()).isEqualTo(WORKER_MAX_COUNT);
  }

  @Test
  public void export_system_info() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThat(section.getName()).isEqualTo("Compute Engine Tasks");
    assertThat(section.getAttributesCount()).isEqualTo(7);
  }
  private static class DumbCEQueueStatus implements CEQueueStatus {


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
    private long methodNotImplemented() {
      throw new UnsupportedOperationException("Not Implemented");
    }

  }
  private static class DumbCeConfiguration implements CeConfiguration {

    @Override
    public void refresh() {
      throw new UnsupportedOperationException("Refresh is not implemented");
    }

    @Override
    public int getWorkerMaxCount() {
      return WORKER_MAX_COUNT;
    }

    @Override
    public int getWorkerCount() {
      return WORKER_COUNT;
    }

    @Override
    public long getQueuePollingDelay() {
      throw new UnsupportedOperationException("getQueuePollingDelay is not implemented");
    }

    @Override
    public long getCleanCeTasksInitialDelay() {
      throw new UnsupportedOperationException("getCleanCeTasksInitialDelay is not implemented");
    }

    @Override
    public long getCleanCeTasksDelay() {
      throw new UnsupportedOperationException("getCleanCeTasksDelay is not implemented");
    }

    @Override
    public int getGracefulStopTimeoutInMs() {
      return 40_000;
    }

  }
}
