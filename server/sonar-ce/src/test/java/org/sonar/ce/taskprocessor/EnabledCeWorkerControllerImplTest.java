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
package org.sonar.ce.taskprocessor;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.configuration.CeConfigurationRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnabledCeWorkerControllerImplTest {
  private Random random = new Random();
  /** 1 <= workerCount <= 5 */
  private int workerCount = 1 + random.nextInt(5);

  @Rule
  public CeConfigurationRule ceConfigurationRule = new CeConfigurationRule()
    .setWorkerCount(workerCount);

  private CeWorker ceWorker = mock(CeWorker.class);
  private EnabledCeWorkerControllerImpl underTest = new EnabledCeWorkerControllerImpl(ceConfigurationRule);

  @Test
  public void isEnabled_returns_true_if_worker_ordinal_is_less_than_CeConfiguration_workerCount() {
    int ordinal = workerCount + Math.min(-1, -random.nextInt(workerCount));
    when(ceWorker.getOrdinal()).thenReturn(ordinal);

    assertThat(underTest.isEnabled(ceWorker))
      .as("For ordinal " + ordinal + " and workerCount " + workerCount)
      .isTrue();
  }

  @Test
  public void isEnabled_returns_false_if_worker_ordinal_is_equal_to_CeConfiguration_workerCount() {
    when(ceWorker.getOrdinal()).thenReturn(workerCount);

    assertThat(underTest.isEnabled(ceWorker)).isFalse();
  }

  @Test
  public void isEnabled_returns_true_if_ordinal_is_invalid() {
    int ordinal = -1 - random.nextInt(3);
    when(ceWorker.getOrdinal()).thenReturn(ordinal);

    assertThat(underTest.isEnabled(ceWorker))
      .as("For invalid ordinal " + ordinal + " and workerCount " + workerCount)
      .isTrue();
  }

  @Test
  public void workerCount_is_loaded_in_constructor() {
    when(ceWorker.getOrdinal()).thenReturn(workerCount);
    assertThat(underTest.isEnabled(ceWorker)).isFalse();

    ceConfigurationRule.setWorkerCount(workerCount + 1);
    assertThat(underTest.isEnabled(ceWorker)).isFalse();
  }
}
