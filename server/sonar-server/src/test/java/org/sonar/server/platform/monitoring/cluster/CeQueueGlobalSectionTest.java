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
package org.sonar.server.platform.monitoring.cluster;

import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.ce.configuration.WorkerCountProvider;
import org.sonar.db.DbClient;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.property.InternalProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

public class CeQueueGlobalSectionTest {

  private DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  private WorkerCountProvider workerCountProvider = mock(WorkerCountProvider.class);

  @Test
  public void test_queue_state_with_default_settings() {
    when(dbClient.ceQueueDao().countByStatus(any(), eq(CeQueueDto.Status.PENDING))).thenReturn(10);
    when(dbClient.ceQueueDao().countByStatus(any(), eq(CeQueueDto.Status.IN_PROGRESS))).thenReturn(1);

    CeQueueGlobalSection underTest = new CeQueueGlobalSection(dbClient);
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "Total Pending", 10);
    assertThatAttributeIs(section, "Total In Progress", 1);
    assertThatAttributeIs(section, "Max Workers per Node", 1);
  }

  @Test
  public void test_queue_state_with_overridden_settings() {
    when(dbClient.ceQueueDao().countByStatus(any(), eq(CeQueueDto.Status.PENDING))).thenReturn(10);
    when(dbClient.ceQueueDao().countByStatus(any(), eq(CeQueueDto.Status.IN_PROGRESS))).thenReturn(2);
    when(workerCountProvider.get()).thenReturn(5);

    CeQueueGlobalSection underTest = new CeQueueGlobalSection(dbClient, workerCountProvider);
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "Total Pending", 10);
    assertThatAttributeIs(section, "Total In Progress", 2);
    assertThatAttributeIs(section, "Max Workers per Node", 5);
  }

  @Test
  public void test_workers_not_paused() {
    CeQueueGlobalSection underTest = new CeQueueGlobalSection(dbClient, workerCountProvider);
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "Workers Paused", false);
  }

  @Test
  public void test_workers_paused() {
    when(dbClient.internalPropertiesDao().selectByKey(any(), eq(InternalProperties.COMPUTE_ENGINE_PAUSE))).thenReturn(Optional.of("true"));

    CeQueueGlobalSection underTest = new CeQueueGlobalSection(dbClient, workerCountProvider);
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    assertThatAttributeIs(section, "Workers Paused", true);
  }
}
