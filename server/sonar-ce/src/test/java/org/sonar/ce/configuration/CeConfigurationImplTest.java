/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.configuration;

import java.util.Random;
import org.junit.Test;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.MessageException;

import static java.lang.Math.abs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CeConfigurationImplTest {
  public static final ConfigurationBridge EMPTY_CONFIGURATION = new ConfigurationBridge(new MapSettings());

  private SimpleWorkerCountProvider workerCountProvider = new SimpleWorkerCountProvider();

  @Test
  public void getWorkerCount_returns_1_when_there_is_no_WorkerCountProvider() {
    assertThat(new CeConfigurationImpl(EMPTY_CONFIGURATION).getWorkerCount()).isOne();
  }

  @Test
  public void getWorkerMaxCount_returns_1_when_there_is_no_WorkerCountProvider() {
    assertThat(new CeConfigurationImpl(EMPTY_CONFIGURATION).getWorkerMaxCount()).isOne();
  }

  @Test
  public void getWorkerCount_returns_value_returned_by_WorkerCountProvider_when_available() {
    int value = randomValidWorkerCount();
    workerCountProvider.set(value);

    assertThat(new CeConfigurationImpl(EMPTY_CONFIGURATION, workerCountProvider).getWorkerCount()).isEqualTo(value);
  }

  @Test
  public void getWorkerMaxCount_returns_10_whichever_the_value_returned_by_WorkerCountProvider() {
    int value = randomValidWorkerCount();
    workerCountProvider.set(value);

    assertThat(new CeConfigurationImpl(EMPTY_CONFIGURATION, workerCountProvider).getWorkerMaxCount()).isEqualTo(10);
  }

  @Test
  public void constructor_throws_MessageException_when_WorkerCountProvider_returns_0() {
    workerCountProvider.set(0);

    assertThatThrownBy(() -> new CeConfigurationImpl(EMPTY_CONFIGURATION, workerCountProvider))
      .isInstanceOf(MessageException.class)
      .hasMessage("Worker count '0' is invalid. " +
        "It must be an integer strictly greater than 0 and less or equal to 10");
  }

  @Test
  public void constructor_throws_MessageException_when_WorkerCountProvider_returns_less_than_0() {
    int value = -1 - abs(new Random().nextInt());
    workerCountProvider.set(value);

    assertThatThrownBy(() -> new CeConfigurationImpl(EMPTY_CONFIGURATION, workerCountProvider))
      .isInstanceOf(MessageException.class)
      .hasMessage("Worker count '" + value + "' is invalid. " +
        "It must be an integer strictly greater than 0 and less or equal to 10");
  }

  @Test
  public void constructor_throws_MessageException_when_WorkerCountProvider_returns_more_then_10() {
    int value = 10 + abs(new Random().nextInt());
    workerCountProvider.set(value);

    assertThatThrownBy(() ->  new CeConfigurationImpl(EMPTY_CONFIGURATION, workerCountProvider))
      .isInstanceOf(MessageException.class)
      .hasMessage("Worker count '" + value + "' is invalid. " +
        "It must be an integer strictly greater than 0 and less or equal to 10");
  }

  @Test
  public void getCleanCeTasksInitialDelay_returns_0() {
    assertThat(new CeConfigurationImpl(EMPTY_CONFIGURATION).getCleanTasksInitialDelay())
      .isZero();
    workerCountProvider.set(1);
    assertThat(new CeConfigurationImpl(EMPTY_CONFIGURATION, workerCountProvider).getCleanTasksInitialDelay())
      .isZero();
  }

  @Test
  public void getCleanCeTasksDelay_returns_2() {
    assertThat(new CeConfigurationImpl(EMPTY_CONFIGURATION).getCleanTasksDelay())
      .isEqualTo(2L);
    workerCountProvider.set(1);
    assertThat(new CeConfigurationImpl(EMPTY_CONFIGURATION, workerCountProvider).getCleanTasksDelay())
      .isEqualTo(2L);
  }

  private static final class SimpleWorkerCountProvider implements WorkerCountProvider {
    private int value = 0;

    void set(int value) {
      this.value = value;
    }

    @Override
    public int get() {
      return value;
    }
  }

  private static int randomValidWorkerCount() {
    return 1 + Math.abs(new Random().nextInt(10));
  }
}
