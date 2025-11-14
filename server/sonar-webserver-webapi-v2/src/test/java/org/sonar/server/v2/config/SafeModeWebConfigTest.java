/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.config;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.server.common.platform.LivenessChecker;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.v2.api.system.controller.DefaultLivenessController;
import org.sonar.server.v2.api.system.controller.HealthController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class SafeModeWebConfigTest {

  private static final SafeModeWebConfig safeModeWebConfig = new SafeModeWebConfig();

  private static Stream<Arguments> components() {
    return Stream.of(
      arguments(safeModeWebConfig.livenessController(mock(LivenessChecker.class), mock(SystemPasscode.class)), DefaultLivenessController.class),
      arguments(safeModeWebConfig.healthController(mock(HealthChecker.class), mock(SystemPasscode.class)), HealthController.class));
  }

  @ParameterizedTest
  @MethodSource("components")
  void custom_components_shouldBeInjectedInSafeModeWebConfig(Object component, Class<?> instanceClass) {
    assertThat(component).isNotNull().isInstanceOf(instanceClass);
  }
}
