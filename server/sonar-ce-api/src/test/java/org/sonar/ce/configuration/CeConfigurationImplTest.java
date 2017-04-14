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
package org.sonar.ce.configuration;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;

import static java.lang.Math.abs;
import static org.assertj.core.api.Assertions.assertThat;

public class CeConfigurationImplTest {
  private static final String CE_WORKERS_COUNT_PROPERTY = "sonar.ce.workerCount";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Settings settings = new MapSettings();

  @Test
  public void getWorkerCount_returns_1_when_worker_property_is_not_defined() {
    assertThat(new CeConfigurationImpl(settings).getWorkerCount()).isEqualTo(1);
  }

  @Test
  public void getWorkerCount_returns_1_when_worker_property_is_empty() {
    settings.setProperty(CE_WORKERS_COUNT_PROPERTY, "");

    assertThat(new CeConfigurationImpl(settings).getWorkerCount()).isEqualTo(1);
  }

  @Test
  public void getWorkerCount_returns_1_when_worker_property_is_space_chars() {
    settings.setProperty(CE_WORKERS_COUNT_PROPERTY, "  \n  ");

    assertThat(new CeConfigurationImpl(settings).getWorkerCount()).isEqualTo(1);
  }

  @Test
  public void getWorkerCount_returns_1_when_worker_property_is_1() {
    settings.setProperty(CE_WORKERS_COUNT_PROPERTY, 1);

    assertThat(new CeConfigurationImpl(settings).getWorkerCount()).isEqualTo(1);
  }

  @Test
  public void getWorkerCount_returns_value_when_worker_property_is_integer_greater_than_1() {
    int value = abs(new Random().nextInt()) + 2;
    settings.setProperty(CE_WORKERS_COUNT_PROPERTY, value);

    assertThat(new CeConfigurationImpl(settings).getWorkerCount()).isEqualTo(value);
  }

  @Test
  public void constructor_throws_MessageException_when_worker_property_is_0() {
    int value = 0;
    settings.setProperty(CE_WORKERS_COUNT_PROPERTY, String.valueOf(value));

    expectMessageException(value);

    new CeConfigurationImpl(settings);
  }

  @Test
  public void constructor_throws_MessageException_when_worker_property_is_less_than_0() {
    int value = -1 * abs(new Random().nextInt());
    settings.setProperty(CE_WORKERS_COUNT_PROPERTY, String.valueOf(value));

    expectMessageException(value);

    new CeConfigurationImpl(settings);
  }

  @Test
  public void constructor_throws_MessageException_when_worker_property_is_not_an_double() {
    double value = 3.5;
    settings.setProperty(CE_WORKERS_COUNT_PROPERTY, String.valueOf(value));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("value '" + value + "' of property " + CE_WORKERS_COUNT_PROPERTY + " is invalid. " +
      "It must an integer strictly greater than 0");

    new CeConfigurationImpl(settings);
  }

  private void expectMessageException(int value) {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage("value '" + value + "' of property " + CE_WORKERS_COUNT_PROPERTY + " is invalid. " +
      "It must an integer strictly greater than 0");
  }

  @Test
  public void getCleanCeTasksInitialDelay_returns_1() {
    assertThat(new CeConfigurationImpl(settings).getCleanCeTasksInitialDelay())
      .isEqualTo(1L);
  }

  @Test
  public void getCleanCeTasksDelay_returns_10() {
    assertThat(new CeConfigurationImpl(settings).getCleanCeTasksDelay())
      .isEqualTo(10L);
  }
}
