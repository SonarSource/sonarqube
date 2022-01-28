/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.rest.RestStatus;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.sonar.application.es.EsConnector;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EsManagedProcessTest {

  private final static int WAIT_FOR_UP_TIMEOUT = 1;
  private final static int WAIT_FOR_UP_TIMEOUT_LONG = 2;

  @Test
  public void isOperational_should_return_false_if_status_is_unknown() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus()).thenReturn(Optional.empty());
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  public void isOperational_should_return_false_if_Elasticsearch_is_RED() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus()).thenReturn(Optional.of(ClusterHealthStatus.RED));
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  public void isOperational_should_return_true_if_Elasticsearch_is_YELLOW() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus()).thenReturn(Optional.of(ClusterHealthStatus.YELLOW));
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_return_true_if_Elasticsearch_is_GREEN() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus()).thenReturn(Optional.of(ClusterHealthStatus.GREEN));
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_return_true_if_Elasticsearch_was_GREEN_once() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus()).thenReturn(Optional.of(ClusterHealthStatus.GREEN));
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isTrue();

    when(esConnector.getClusterHealthStatus()).thenReturn(Optional.of(ClusterHealthStatus.RED));
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_retry_if_Elasticsearch_is_unreachable() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus())
      .thenReturn(Optional.empty())
      .thenReturn(Optional.of(ClusterHealthStatus.GREEN));
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isTrue();
  }

  @Test
  public void isOperational_should_return_false_if_Elasticsearch_status_cannot_be_evaluated() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus())
      .thenThrow(new RuntimeException("test"));
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  public void isOperational_should_return_false_if_ElasticsearchException_with_connection_refused_thrown() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus())
      .thenThrow(new ElasticsearchException("Connection refused"));
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  public void isOperational_should_return_false_if_ElasticsearchException_with_connection_timeout_thrown() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus())
      .thenThrow(new ElasticsearchException(new ExecutionException(new ConnectException("Timeout connecting to [/127.0.0.1:9001]"))));
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT_LONG);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  public void isOperational_should_return_false_if_ElasticsearchException_thrown() {
    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus())
      .thenThrow(new ElasticsearchException("test"));
    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isFalse();
  }

  @Test
  public void isOperational_must_log_once_when_master_is_not_elected() {
    MemoryAppender<ILoggingEvent> memoryAppender = new MemoryAppender<>();
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();
    memoryAppender.setContext(lc);
    memoryAppender.start();
    lc.getLogger(EsManagedProcess.class).addAppender(memoryAppender);

    EsConnector esConnector = mock(EsConnector.class);
    when(esConnector.getClusterHealthStatus())
      .thenThrow(new ElasticsearchStatusException("foobar[type=master_not_discovered_exception,acme]...", RestStatus.SERVICE_UNAVAILABLE));

    EsManagedProcess underTest = new EsManagedProcess(mock(Process.class), ProcessId.ELASTICSEARCH, esConnector, WAIT_FOR_UP_TIMEOUT);
    assertThat(underTest.isOperational()).isFalse();
    assertThat(memoryAppender.events).isNotEmpty();
    assertThat(memoryAppender.events)
      .extracting(ILoggingEvent::getLevel, ILoggingEvent::getMessage)
      .containsOnlyOnce(
        tuple(Level.INFO, "Elasticsearch is waiting for a master to be elected. Did you start all the search nodes ?"));

    // Second call must not log another message
    assertThat(underTest.isOperational()).isFalse();
    assertThat(memoryAppender.events)
      .extracting(ILoggingEvent::getLevel, ILoggingEvent::getMessage)
      .containsOnlyOnce(
        tuple(Level.INFO, "Elasticsearch is waiting for a master to be elected. Did you start all the search nodes ?"));
  }

  private static class MemoryAppender<E> extends AppenderBase<E> {
    private final List<E> events = new ArrayList<>();

    @Override
    protected void append(E eventObject) {
      events.add(eventObject);
    }
  }
}
