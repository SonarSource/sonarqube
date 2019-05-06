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
package org.sonar.process;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class AbstractStopperThreadTest {
  private Monitored monitored = mock(Monitored.class);

  @Test
  public void stop_in_a_timely_fashion() {
    // max stop timeout is 5 seconds, but test fails after 3 seconds
    // -> guarantees that stop is immediate
    AbstractStopperThread stopper = new AbstractStopperThread("theThreadName", () -> monitored.hardStop(), 5000L){};
    stopper.start();

    verify(monitored, timeout(3000)).hardStop();
    assertThat(stopper.getName()).isEqualTo("theThreadName");
  }

  @Test
  public void stop_timeout() {
    doAnswer(invocationOnMock -> {
      await().atMost(10, TimeUnit.SECONDS).until(() -> false);
      return null;
    }).when(monitored).hardStop();

    // max stop timeout is 100 milliseconds
    AbstractStopperThread stopper = new AbstractStopperThread("theThreadName", () -> monitored.hardStop(), 5000L){};
    stopper.start();

    verify(monitored, timeout(3000)).hardStop();
    assertThat(stopper.getName()).isEqualTo("theThreadName");
  }

  @Test
  public void stopIt_interrupts_worker() {
    doAnswer(invocationOnMock -> {
      await().atMost(10, TimeUnit.SECONDS).until(() -> false);
      return null;
    }).when(monitored).hardStop();

    // max stop timeout is 100 milliseconds
    AbstractStopperThread stopper = new AbstractStopperThread("theThreadName", () -> monitored.hardStop(), 5000L){};
    stopper.start();

    verify(monitored, timeout(3_000)).hardStop();

    stopper.stopIt();
    await().atMost(3, TimeUnit.SECONDS).until(() -> !stopper.isAlive());
    assertThat(stopper.isAlive()).isFalse();
  }
}
