/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.process.cluster.health;

import com.hazelcast.core.HazelcastInstanceNotActiveException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.process.LoggingRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;

class HealthStateRefresherTest {

  @RegisterExtension
  private final LoggingRule logging = new LoggingRule(HealthStateRefresher.class);

  private final Random random = new Random();
  private final NodeDetailsTestSupport testSupport = new NodeDetailsTestSupport(random);

  private final HealthStateRefresherExecutorService executorService = mock(HealthStateRefresherExecutorService.class);
  private final NodeHealthProvider nodeHealthProvider = mock(NodeHealthProvider.class);
  private final SharedHealthState sharedHealthState = mock(SharedHealthState.class);
  private final HealthStateRefresher underTest = new HealthStateRefresher(executorService, nodeHealthProvider, sharedHealthState);

  @Test
  void start_adds_runnable_with_10_second_delay_and_initial_delay_putting_NodeHealth_from_provider_into_SharedHealthState() {
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    NodeHealth[] nodeHealths = {
      testSupport.randomNodeHealth(),
      testSupport.randomNodeHealth(),
      testSupport.randomNodeHealth()
    };
    Error expected = new Error("Simulating exception raised by NodeHealthProvider");
    when(nodeHealthProvider.get())
      .thenReturn(nodeHealths[0])
      .thenReturn(nodeHealths[1])
      .thenReturn(nodeHealths[2])
      .thenThrow(expected);

    underTest.start();

    verify(executorService).scheduleWithFixedDelay(runnableCaptor.capture(), eq(1L), eq(10L), eq(TimeUnit.SECONDS));

    Runnable runnable = runnableCaptor.getValue();
    runnable.run();
    runnable.run();
    runnable.run();

    verify(sharedHealthState).writeMine(nodeHealths[0]);
    verify(sharedHealthState).writeMine(nodeHealths[1]);
    verify(sharedHealthState).writeMine(nodeHealths[2]);

    assertThatCode(runnable::run)
      .doesNotThrowAnyException();
  }

  @Test
  void stop_whenCalled_hasNoEffect() {
    underTest.stop();

    verify(sharedHealthState).clearMine();
    verifyNoInteractions(executorService, nodeHealthProvider);
  }

  @Test
  void stop_whenThrowHazelcastInactiveException_shouldSilenceError() {
    logging.setLevel(DEBUG);
    SharedHealthState sharedHealthStateMock = mock(SharedHealthState.class);
    doThrow(HazelcastInstanceNotActiveException.class).when(sharedHealthStateMock).clearMine();
    HealthStateRefresher underTest = new HealthStateRefresher(executorService, nodeHealthProvider, sharedHealthStateMock);
    underTest.stop();

    assertThat(logging.getLogs(ERROR)).isEmpty();
    assertThat(logging.hasLog(DEBUG, "Hazelcast is not active anymore")).isTrue();
  }

  @Test
  void start_whenHazelcastIsNotActive_shouldNotLogErrors() {
    logging.setLevel(DEBUG);
    doThrow(new HazelcastInstanceNotActiveException()).when(sharedHealthState).writeMine(any());

    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    underTest.start();

    verify(executorService).scheduleWithFixedDelay(runnableCaptor.capture(), eq(1L), eq(10L), eq(TimeUnit.SECONDS));
    Runnable runnable = runnableCaptor.getValue();
    runnable.run();

    assertThat(logging.getLogs(ERROR)).isEmpty();
    assertThat(logging.hasLog(DEBUG, "Hazelcast is not active anymore")).isTrue();
  }
}
