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
package org.sonar.server.platform.platformlevel;

import java.util.Properties;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.utils.System2;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.WebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


public class PlatformLevel1Test {

  private PlatformLevel1 underTest = new PlatformLevel1(mock(Platform.class), new Properties());

  @Test
  public void no_missing_dependencies_between_components() {
    underTest.configureLevel();

    assertThat(underTest.getAll(PropertyDefinition.class)).isNotEmpty();
    assertThat(underTest.getOptional(WebServer.class)).isPresent();
    assertThat(underTest.getOptional(System2.class)).isPresent();
  }
}
