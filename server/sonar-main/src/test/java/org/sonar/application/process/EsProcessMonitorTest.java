/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.application.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.junit.Test;
import org.sonar.process.ProcessId;
import org.sonar.process.command.EsCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EsProcessMonitorTest {

  @Test
  public void isOperational_should_return_false_if_Elasticsearch_is_RED() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any())).thenReturn(ClusterHealthStatus.RED);
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), getEsCommand(), esConnector);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  public void isOperational_should_return_true_if_Elasticsearch_is_YELLOW() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any())).thenReturn(ClusterHealthStatus.YELLOW);
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), getEsCommand(), esConnector);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_return_true_if_Elasticsearch_is_GREEN() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any())).thenReturn(ClusterHealthStatus.GREEN);
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), getEsCommand(), esConnector);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_return_true_if_Elasticsearch_was_GREEN_once() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any())).thenReturn(ClusterHealthStatus.GREEN);
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), getEsCommand(), esConnector);
    assertThat(underTest.isOperational()).isTrue();

    when(esConnector.getClusterHealthStatus(any())).thenReturn(ClusterHealthStatus.RED);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_retry_if_Elasticsearch_is_unreachable() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any()))
      .thenThrow(new NoNodeAvailableException("test"))
      .thenReturn(ClusterHealthStatus.GREEN);
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), getEsCommand(), esConnector);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_return_false_if_Elasticsearch_status_cannot_be_evaluated() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any()))
      .thenThrow(new RuntimeException("test"));
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), getEsCommand(), esConnector);
    assertThat(underTest.isOperational()).isFalse();
  }

  private EsCommand getEsCommand() throws IOException {
    Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName());
    return new EsCommand(ProcessId.ELASTICSEARCH, tempDirectory.toFile())
      .setHost("localhost")
      .setPort(new Random().nextInt(40000));
  }
}
