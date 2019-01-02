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
package org.sonar.ce.taskprocessor;

import java.lang.reflect.Field;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTaskInterrupter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CeTaskInterrupterProviderTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings();
  private CeWorkerController ceWorkerController = mock(CeWorkerController.class);
  private System2 system2 = mock(System2.class);
  private CeTaskInterrupterProvider underTest = new CeTaskInterrupterProvider();

  @Test
  public void provide_returns_a_SimpleCeTaskInterrupter_instance_if_configuration_is_empty() {
    CeTaskInterrupter instance = underTest.provide(settings.asConfig(), ceWorkerController, system2);

    assertThat(instance)
      .isInstanceOf(SimpleCeTaskInterrupter.class);
  }

  @Test
  public void provide_always_return_the_same_SimpleCeTaskInterrupter_instance() {
    CeTaskInterrupter instance = underTest.provide(settings.asConfig(), ceWorkerController, system2);

    assertThat(instance)
      .isSameAs(underTest.provide(settings.asConfig(), ceWorkerController, system2))
      .isSameAs(underTest.provide(new MapSettings().asConfig(), ceWorkerController, system2));
  }

  @Test
  public void provide_returns_a_TimeoutCeTaskInterrupter_instance_if_property_taskTimeout_has_a_value() throws IllegalAccessException, NoSuchFieldException {
    int timeout = 1 + new Random().nextInt(2222);
    settings.setProperty("sonar.ce.task.timeoutSeconds", timeout);

    CeTaskInterrupter instance = underTest.provide(settings.asConfig(), ceWorkerController, system2);

    assertThat(instance)
      .isInstanceOf(TimeoutCeTaskInterrupter.class);

    assertThat(readField(instance, "taskTimeoutThreshold"))
      .isEqualTo(timeout * 1_000L);
    assertThat(readField(instance, "ceWorkerController"))
      .isSameAs(ceWorkerController);
    assertThat(readField(instance, "system2"))
      .isSameAs(system2);
  }

  @Test
  public void provide_fails_with_ISE_if_property_is_not_a_long() {
    settings.setProperty("sonar.ce.task.timeoutSeconds", "foo");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The property 'sonar.ce.task.timeoutSeconds' is not an long value: For input string: \"foo\"");

    underTest.provide(settings.asConfig(), ceWorkerController, system2);
  }

  @Test
  public void provide_fails_with_ISE_if_property_is_zero() {
    settings.setProperty("sonar.ce.task.timeoutSeconds", "0");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The property 'sonar.ce.task.timeoutSeconds' must be a long value >= 1. Got '0'");

    underTest.provide(settings.asConfig(), ceWorkerController, system2);
  }

  @Test
  public void provide_fails_with_ISE_if_property_is_less_than_zero() {
    int negativeValue = -(1 + new Random().nextInt(1_212));
    settings.setProperty("sonar.ce.task.timeoutSeconds", negativeValue);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("The property 'sonar.ce.task.timeoutSeconds' must be a long value >= 1. Got '" + negativeValue + "'");

    underTest.provide(settings.asConfig(), ceWorkerController, system2);
  }

  @Test
  public void provide_always_return_the_same_TimeoutCeTaskInterrupter_instance() {
    int timeout = 1 + new Random().nextInt(2222);
    settings.setProperty("sonar.ce.task.timeoutSeconds", timeout);

    CeTaskInterrupter instance = underTest.provide(settings.asConfig(), ceWorkerController, system2);

    assertThat(instance)
      .isSameAs(underTest.provide(settings.asConfig(), ceWorkerController, system2))
      .isSameAs(underTest.provide(new MapSettings().setProperty("sonar.ce.task.timeoutSeconds", 999).asConfig(), ceWorkerController, system2));
  }

  private static Object readField(CeTaskInterrupter instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
    Class<?> clazz = instance.getClass();
    Field timeoutField = clazz.getDeclaredField(fieldName);
    timeoutField.setAccessible(true);
    return timeoutField.get(instance);
  }
}
