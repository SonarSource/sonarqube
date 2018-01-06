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
package org.sonar.process.cluster.health;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class HealthStateRefresherTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Random random = new Random();
  private NodeDetailsTestSupport testSupport = new NodeDetailsTestSupport(random);

  private HealthStateRefresherExecutorService executorService = mock(HealthStateRefresherExecutorService.class);
  private NodeHealthProvider nodeHealthProvider = mock(NodeHealthProvider.class);
  private SharedHealthState sharedHealthState = mock(SharedHealthState.class);
  private HealthStateRefresher underTest = new HealthStateRefresher(executorService, nodeHealthProvider, sharedHealthState);

  @Test
  public void start_adds_runnable_with_10_second_delay_and_initial_delay_putting_NodeHealth_from_provider_into_SharedHealthState() {
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

    try {
      runnable.run();
    } catch (IllegalStateException e) {
      fail("Runnable should catch any Throwable");
    }
  }

  @Test
  public void stop_has_no_effect() {
    underTest.stop();

    verify(sharedHealthState).clearMine();
    verifyZeroInteractions(executorService, nodeHealthProvider);
  }
}
