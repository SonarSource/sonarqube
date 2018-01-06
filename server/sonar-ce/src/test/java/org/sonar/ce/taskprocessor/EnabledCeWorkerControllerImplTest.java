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

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.configuration.CeConfigurationRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnabledCeWorkerControllerImplTest {
  private Random random = new Random();
  /** 1 <= workerCount <= 5 */
  private int randomWorkerCount = 1 + random.nextInt(5);

  @Rule
  public CeConfigurationRule ceConfigurationRule = new CeConfigurationRule()
    .setWorkerCount(randomWorkerCount);
  @Rule
  public LogTester logTester = new LogTester();

  private CeWorker ceWorker = mock(CeWorker.class);
  private EnabledCeWorkerControllerImpl underTest = new EnabledCeWorkerControllerImpl(ceConfigurationRule);

  @Test
  public void isEnabled_returns_true_if_worker_ordinal_is_less_than_CeConfiguration_workerCount() {
    int ordinal = randomWorkerCount + Math.min(-1, -random.nextInt(randomWorkerCount));
    when(ceWorker.getOrdinal()).thenReturn(ordinal);

    assertThat(underTest.isEnabled(ceWorker))
      .as("For ordinal " + ordinal + " and workerCount " + randomWorkerCount)
      .isTrue();
  }

  @Test
  public void isEnabled_returns_false_if_worker_ordinal_is_equal_to_CeConfiguration_workerCount() {
    when(ceWorker.getOrdinal()).thenReturn(randomWorkerCount);

    assertThat(underTest.isEnabled(ceWorker)).isFalse();
  }

  @Test
  public void isEnabled_returns_true_if_ordinal_is_invalid() {
    int ordinal = -1 - random.nextInt(3);
    when(ceWorker.getOrdinal()).thenReturn(ordinal);

    assertThat(underTest.isEnabled(ceWorker))
      .as("For invalid ordinal " + ordinal + " and workerCount " + randomWorkerCount)
      .isTrue();
  }

  @Test
  public void constructor_writes_no_info_log_if_workerCount_is_1() {
    ceConfigurationRule.setWorkerCount(1);
    logTester.clear();

    new EnabledCeWorkerControllerImpl(ceConfigurationRule);

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void constructor_writes_info_log_if_workerCount_is_greater_than_1() {
    int newWorkerCount = randomWorkerCount + 1;
    ceConfigurationRule.setWorkerCount(newWorkerCount);
    logTester.clear();

    new EnabledCeWorkerControllerImpl(ceConfigurationRule);

    verifyInfoLog(newWorkerCount);
  }

  @Test
  public void workerCount_is_loaded_in_constructor() {
    when(ceWorker.getOrdinal()).thenReturn(randomWorkerCount);
    assertThat(underTest.isEnabled(ceWorker)).isFalse();

    ceConfigurationRule.setWorkerCount(randomWorkerCount + 1);
    assertThat(underTest.isEnabled(ceWorker)).isFalse();
  }

  @Test
  public void refresh_reloads_workerCount() {
    when(ceWorker.getOrdinal()).thenReturn(randomWorkerCount);
    assertThat(underTest.isEnabled(ceWorker)).isFalse();
    ceConfigurationRule.setRefreshCallHook((rule) -> rule.setWorkerCount(randomWorkerCount + 1));

    underTest.refresh();

    assertThat(underTest.isEnabled(ceWorker)).isTrue();
  }

  @Test
  public void refresh_writes_info_log_if_workerCount_is_greater_than_1() {
    logTester.clear();
    int newWorkerCount = randomWorkerCount + 1;
    ceConfigurationRule.setRefreshCallHook((rule) -> rule.setWorkerCount(newWorkerCount));

    underTest.refresh();

    verifyInfoLog(newWorkerCount);
  }

  @Test
  public void refresh_writes_no_info_log_if_workerCount_is_1() {
    logTester.clear();
    ceConfigurationRule.setRefreshCallHook((rule) -> rule.setWorkerCount(1));

    underTest.refresh();

    assertThat(logTester.logs()).isEmpty();
  }

  private void verifyInfoLog(int workerCount) {
    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.INFO))
      .containsOnly("Compute Engine will use " + workerCount + " concurrent workers to process tasks");
  }
}
