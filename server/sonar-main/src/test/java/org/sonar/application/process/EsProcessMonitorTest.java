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
package org.sonar.application.process;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.discovery.MasterNotDiscoveredException;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.sonar.application.es.EsInstallation;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EsProcessMonitorTest {

  @Test
  public void isOperational_should_return_false_if_Elasticsearch_is_RED() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any())).thenReturn(ClusterHealthStatus.RED);
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), ProcessId.ELASTICSEARCH, getEsConfig(), esConnector);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  public void isOperational_should_return_true_if_Elasticsearch_is_YELLOW() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any())).thenReturn(ClusterHealthStatus.YELLOW);
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), ProcessId.ELASTICSEARCH, getEsConfig(), esConnector);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_return_true_if_Elasticsearch_is_GREEN() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any())).thenReturn(ClusterHealthStatus.GREEN);
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), ProcessId.ELASTICSEARCH, getEsConfig(), esConnector);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_return_true_if_Elasticsearch_was_GREEN_once() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any())).thenReturn(ClusterHealthStatus.GREEN);
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), ProcessId.ELASTICSEARCH, getEsConfig(), esConnector);
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
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), ProcessId.ELASTICSEARCH, getEsConfig(), esConnector);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_return_false_if_Elasticsearch_status_cannot_be_evaluated() throws Exception {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any()))
      .thenThrow(new RuntimeException("test"));
    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), ProcessId.ELASTICSEARCH, getEsConfig(), esConnector);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  public void isOperational_must_log_once_when_master_is_not_elected() throws Exception {
    MemoryAppender<ILoggingEvent> memoryAppender = new MemoryAppender<>();
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();
    memoryAppender.setContext(lc);
    memoryAppender.start();
    lc.getLogger(EsProcessMonitor.class).addAppender(memoryAppender);

    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus(any()))
      .thenThrow(new MasterNotDiscoveredException("Master not elected -test-"));

    EsProcessMonitor underTest = new EsProcessMonitor(mock(Process.class), ProcessId.ELASTICSEARCH, getEsConfig(), esConnector);
    assertThat(underTest.isOperational()).isFalse();
    assertThat(memoryAppender.events).isNotEmpty();
    assertThat(memoryAppender.events)
      .extracting(ILoggingEvent::getLevel, ILoggingEvent::getMessage)
      .containsOnlyOnce(
        tuple(Level.INFO, "Elasticsearch is waiting for a master to be elected. Did you start all the search nodes ?")
      );

    // Second call must not log another message
    assertThat(underTest.isOperational()).isFalse();
    assertThat(memoryAppender.events)
      .extracting(ILoggingEvent::getLevel, ILoggingEvent::getMessage)
      .containsOnlyOnce(
        tuple(Level.INFO, "Elasticsearch is waiting for a master to be elected. Did you start all the search nodes ?")
      );
  }

  private EsInstallation getEsConfig() throws IOException {
    Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName());
    Properties properties = new Properties();
    properties.setProperty("sonar.path.home", "/imaginary/path");
    properties.setProperty("sonar.path.data", "/imaginary/path");
    properties.setProperty("sonar.path.temp", "/imaginary/path");
    properties.setProperty("sonar.path.logs", "/imaginary/path");
    return new EsInstallation(new Props(properties))
      .setHost("localhost")
      .setPort(new Random().nextInt(40000));
  }

  private class MemoryAppender<E> extends AppenderBase<E> {
    private final List<E> events = new ArrayList();

    @Override
    protected void append(E eventObject) {
      events.add(eventObject);
    }
  }
}
