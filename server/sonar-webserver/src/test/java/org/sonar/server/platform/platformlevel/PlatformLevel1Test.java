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
package org.sonar.server.platform.platformlevel;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.sonar.server.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


class PlatformLevel1Test {

  private final PlatformLevel1 underTest = new PlatformLevel1(mock(Platform.class), new Properties());

  @Test
  void whenLoadingComponent_thenCoreExtensionsLoadAlongside() {
    underTest.configureLevel();

    assertThat(underTest.getContainer().context().getBeanDefinitionNames()).isNotEmpty()
      .anyMatch(beanName -> beanName.endsWith("TestCoreExtension.TestBean1"), "testBean1 should be loaded")
      .noneMatch(beanName -> beanName.endsWith("TestCoreExtension.TestBean2"), "testBean2 should not be loaded");
  }
}
