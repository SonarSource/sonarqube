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
package org.sonar.server.v2.config;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.platform.Server;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.server.v2.api.analysis.controller.DefaultJresController;
import org.sonar.server.v2.api.analysis.controller.DefaultVersionController;
import org.sonar.server.v2.api.analysis.controller.DefaultScannerEngineController;
import org.sonar.server.v2.api.analysis.service.JresHandler;
import org.sonar.server.v2.api.analysis.service.JresHandlerImpl;
import org.sonar.server.v2.api.analysis.service.ScannerEngineHandler;
import org.sonar.server.v2.api.analysis.service.ScannerEngineHandlerImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class PlatformLevel4WebConfigTest {

  private static final PlatformLevel4WebConfig platformLevel4WebConfig = new PlatformLevel4WebConfig();

  private static Stream<Arguments> components() {
    return Stream.of(
      arguments(platformLevel4WebConfig.versionController(mock(Server.class)), DefaultVersionController.class),
      arguments(platformLevel4WebConfig.jresHandler(), JresHandlerImpl.class),
      arguments(platformLevel4WebConfig.jresController(mock(JresHandler.class)), DefaultJresController.class),
      arguments(platformLevel4WebConfig.scannerEngineHandler(mock(ServerFileSystem.class)), ScannerEngineHandlerImpl.class),
      arguments(platformLevel4WebConfig.scannerEngineController(mock(ScannerEngineHandler.class)), DefaultScannerEngineController.class)
    );
  }

  @ParameterizedTest
  @MethodSource("components")
  void components_shouldBeInjectedInPlatformLevel4WebConfig(Object component, Class<?> instanceClass) {
    assertThat(component).isNotNull().isInstanceOf(instanceClass);
  }
}
