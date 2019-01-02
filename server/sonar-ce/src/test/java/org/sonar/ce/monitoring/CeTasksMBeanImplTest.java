/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.sonar.ce.configuration.CeConfiguration;
import org.sonar.ce.taskprocessor.CeWorker;
import org.sonar.ce.taskprocessor.CeWorkerController;
import org.sonar.ce.taskprocessor.CeWorkerFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CeTasksMBeanImplTest {
  private static final long PENDING_COUNT = 2;
  private static final long IN_PROGRESS_COUNT = 5;
  private static final long ERROR_COUNT = 10;
  private static final long SUCCESS_COUNT = 13;
  private static final long PROCESSING_TIME = 987;
  private static final int WORKER_MAX_COUNT = 666;
  private static final int WORKER_COUNT = 56;
  private static final Set<CeWorker> WORKERS = IntStream.range(0, 2 + new Random().nextInt(10))
    .mapToObj(i -> RandomStringUtils.randomAlphabetic(15))
    .map(uuid -> {
      CeWorker res = mock(CeWorker.class);
      when(res.getUUID()).thenReturn(uuid);
      return res;
    })
    .collect(MoreCollectors.toSet());

  private CeWorkerController ceWorkerController = mock(CeWorkerController.class);
  private CeTasksMBeanImpl underTest = new CeTasksMBeanImpl(new DumbCEQueueStatus(), new DumbCeConfiguration(), new DumbCeWorkerFactory(), ceWorkerController);

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
  public void getWorkerUuids_returns_ordered_list_of_uuids_of_worker_from_CeWorkerFactory_instance() {
    List<String> workerUuids = underTest.getWorkerUuids();

    assertThat(workerUuids).isEqualTo(WORKERS.stream().map(CeWorker::getUUID).sorted().collect(Collectors.toList()));
    // ImmutableSet can not be serialized
    assertThat(workerUuids).isNotInstanceOf(ImmutableSet.class);
  }

  @Test
  public void getEnabledWorkerUuids_returns_ordered_list_of_uuids_of_worker_from_CeWorkerFactory_instance_filtered_on_enabled_ones() {
    int enabledWorkerCount = new Random().nextInt(WORKERS.size());
    int i = 0;
    CeWorker[] enabledWorkers = new CeWorker[enabledWorkerCount];
    for (CeWorker worker : WORKERS) {
      if (i < enabledWorkerCount) {
        enabledWorkers[i] = worker;
        when(ceWorkerController.isEnabled(worker)).thenReturn(true);
      } else {
        when(ceWorkerController.isEnabled(worker)).thenReturn(false);
      }
      i++;
    }

    List<String> enabledWorkerUuids = underTest.getEnabledWorkerUuids();

    assertThat(enabledWorkerUuids).isEqualTo(Stream.of(enabledWorkers).map(CeWorker::getUUID).sorted().collect(Collectors.toList()));
    // ImmutableSet can not be serialized
    assertThat(enabledWorkerUuids).isNotInstanceOf(ImmutableSet.class);
  }

  @Test
  public void export_system_info() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThat(section.getName()).isEqualTo("Compute Engine Tasks");
    assertThat(section.getAttributesCount()).isEqualTo(8);
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

    @Override
    public boolean areWorkersPaused() {
      return false;
    }

    private long methodNotImplemented() {
      throw new UnsupportedOperationException("Not Implemented");
    }

  }
  private static class DumbCeConfiguration implements CeConfiguration {

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

  private static class DumbCeWorkerFactory implements CeWorkerFactory {
    @Override
    public CeWorker create(int ordinal) {
      throw new UnsupportedOperationException("create should not be called");
    }

    @Override
    public Set<CeWorker> getWorkers() {
      return WORKERS;
    }
  }
}
