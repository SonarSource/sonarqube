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
package org.sonar.ce.taskprocessor;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.Test;
import org.sonar.ce.queue.InternalCeQueue;
import org.sonar.core.util.UuidFactoryImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CeWorkerFactoryImplTest {
  private int randomOrdinal = new Random().nextInt(20);
  private CeWorkerFactoryImpl underTest = new CeWorkerFactoryImpl(mock(InternalCeQueue.class),
    mock(CeTaskProcessorRepository.class), UuidFactoryImpl.INSTANCE, mock(EnabledCeWorkerController.class));

  @Test
  public void create_return_CeWorker_object_with_specified_ordinal() {
    CeWorker ceWorker = underTest.create(randomOrdinal);

    assertThat(ceWorker.getOrdinal()).isEqualTo(randomOrdinal);
  }

  @Test
  public void create_returns_CeWorker_with_listeners_passed_to_factory_constructor() throws NoSuchFieldException, IllegalAccessException {
    CeWorker.ExecutionListener executionListener1 = mock(CeWorker.ExecutionListener.class);
    CeWorker.ExecutionListener executionListener2 = mock(CeWorker.ExecutionListener.class);
    CeWorkerFactoryImpl underTest = new CeWorkerFactoryImpl(mock(InternalCeQueue.class),
      mock(CeTaskProcessorRepository.class), UuidFactoryImpl.INSTANCE, mock(EnabledCeWorkerController.class),
      new CeWorker.ExecutionListener[] {executionListener1, executionListener2});

    CeWorker ceWorker = underTest.create(randomOrdinal);

    Field f = CeWorkerImpl.class.getDeclaredField("listeners");
    f.setAccessible(true);
    assertThat((List<CeWorker.ExecutionListener>) f.get(ceWorker)).containsExactly(
      executionListener1, executionListener2);
  }

  @Test
  public void create_allows_multiple_calls_with_same_ordinal() {
    IntStream.range(0, new Random().nextInt(50)).forEach(ignored -> {
      CeWorker ceWorker = underTest.create(randomOrdinal);

      assertThat(ceWorker.getOrdinal()).isEqualTo(randomOrdinal);
    });
  }

  @Test
  public void each_call_must_return_a_new_ceworker_with_unique_uuid() {
    Set<CeWorker> ceWorkers = new HashSet<>();
    Set<String> ceWorkerUUIDs = new HashSet<>();

    for (int i = 0; i < 10; i++) {
      CeWorker ceWorker = underTest.create(i);
      ceWorkers.add(ceWorker);
      ceWorkerUUIDs.add(ceWorker.getUUID());
    }

    assertThat(ceWorkers).hasSize(10);
    assertThat(ceWorkerUUIDs).hasSize(10);
  }

  @Test
  public void ceworker_created_by_factory_must_contain_uuid() {
    CeWorker ceWorker = underTest.create(randomOrdinal);
    assertThat(ceWorker.getUUID()).isNotEmpty();
  }

  @Test
  public void CeWorkerFactory_has_an_empty_set_of_uuids_when_created() {
    assertThat(underTest.getWorkerUUIDs()).isEmpty();
  }

  @Test
  public void CeWorkerFactory_must_returns_the_uuids_of_worker() {
    Set<String> ceWorkerUUIDs = new HashSet<>();

    for (int i = 0; i < 10; i++) {
      ceWorkerUUIDs.add(underTest.create(i).getUUID());
    }

    assertThat(underTest.getWorkerUUIDs()).isEqualTo(ceWorkerUUIDs);
  }
}
