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
package org.sonar.ce;

import com.google.common.collect.ImmutableSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.taskprocessor.CeWorkerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StandaloneCeDistributedInformationTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void broadcastWorkerUUIDs_must_retrieve_from_ceworkerfactory() {
    CeWorkerFactory ceWorkerFactory = mock(CeWorkerFactory.class);
    StandaloneCeDistributedInformation ceCluster = new StandaloneCeDistributedInformation(ceWorkerFactory);

    ceCluster.broadcastWorkerUUIDs();
    verify(ceWorkerFactory).getWorkerUUIDs();
  }

  @Test
  public void getWorkerUUIDs_must_be_retrieved_from_ceworkerfactory() {
    CeWorkerFactory ceWorkerFactory = mock(CeWorkerFactory.class);
    Set<String> workerUUIDs = ImmutableSet.of("1", "2", "3");
    when(ceWorkerFactory.getWorkerUUIDs()).thenReturn(workerUUIDs);
    StandaloneCeDistributedInformation ceCluster = new StandaloneCeDistributedInformation(ceWorkerFactory);

    ceCluster.broadcastWorkerUUIDs();
    assertThat(ceCluster.getWorkerUUIDs()).isEqualTo(workerUUIDs);
  }

  @Test
  public void when_broadcastWorkerUUIDs_is_not_called_getWorkerUUIDs_is_null() {
    CeWorkerFactory ceWorkerFactory = mock(CeWorkerFactory.class);
    Set<String> workerUUIDs = ImmutableSet.of("1", "2", "3");
    when(ceWorkerFactory.getWorkerUUIDs()).thenReturn(workerUUIDs);
    StandaloneCeDistributedInformation ceCluster = new StandaloneCeDistributedInformation(ceWorkerFactory);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Invalid call, broadcastWorkerUUIDs() must be called first.");

    ceCluster.getWorkerUUIDs();
  }

  @Test
  public void acquireCleanJobLock_returns_a_non_current_lock() {
    StandaloneCeDistributedInformation underTest = new StandaloneCeDistributedInformation(mock(CeWorkerFactory.class));

    Lock lock = underTest.acquireCleanJobLock();

    IntStream.range(0, 5 + Math.abs(new Random().nextInt(50)))
      .forEach(i -> {
        try {
          assertThat(lock.tryLock()).isTrue();
          assertThat(lock.tryLock(1, TimeUnit.MINUTES)).isTrue();
          lock.lock();
          lock.lockInterruptibly();
          lock.unlock();
        } catch (InterruptedException e) {
          fail("no InterruptedException should be thrown");
        }
        try {
          lock.newCondition();
          fail("a UnsupportedOperationException should have been thrown");
        } catch (UnsupportedOperationException e) {
          assertThat(e.getMessage()).isEqualTo("newCondition not supported");
        }
      });
  }
}
