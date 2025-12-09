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
import org.sonar.server.user.UserSession;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class PlatformLevel4WebConfigTest {

  private static final PlatformLevel4WebConfig platformLevel4WebConfig = new PlatformLevel4WebConfig();

  private static Stream<Arguments> components() {
    return Stream.of(
      arguments(platformLevel4WebConfig.requestMappingHandlerMapping(mock(UserSession.class)), RequestMappingHandlerMapping.class));
  }

  @ParameterizedTest
  @MethodSource("components")
  void custom_components_shouldBeInjectedInPlatformLevel4WebConfig(Object component, Class<?> instanceClass) {
    assertThat(component).isNotNull().isInstanceOf(instanceClass);
  }
}
