/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SonarUserHomeProviderTest {

  private final System2 system = mock(System2.class);
  private final SonarUserHomeProvider underTest = new SonarUserHomeProvider(system);

  @Test
  void createTempFolderFromDefaultUserHome(@TempDir Path userHome) {
    when(system.envVariable("SONAR_USER_HOME")).thenReturn(null);
    when(system.property("user.home")).thenReturn(userHome.toString());

    var sonarUserHome = underTest.provide(new ScannerProperties(Map.of()));

    assertThat(sonarUserHome.getPath()).isEqualTo(userHome.resolve(".sonar"));
  }

  @Test
  void should_consider_env_variable_over_user_home(@TempDir Path userHome, @TempDir Path sonarUserHomeFromEnv) {
    when(system.envVariable("SONAR_USER_HOME")).thenReturn(sonarUserHomeFromEnv.toString());
    when(system.property("user.home")).thenReturn(userHome.toString());

    var sonarUserHome = underTest.provide(new ScannerProperties(Map.of()));

    assertThat(sonarUserHome.getPath()).isEqualTo(sonarUserHomeFromEnv);
  }

  @Test
  void should_consider_scanner_property_over_env_and_user_home(@TempDir Path userHome, @TempDir Path sonarUserHomeFromEnv, @TempDir Path sonarUserHomeFromProps) {
    when(system.envVariable("SONAR_USER_HOME")).thenReturn(sonarUserHomeFromEnv.toString());
    when(system.property("user.home")).thenReturn(userHome.toString());

    var sonarUserHome = underTest.provide(new ScannerProperties(Map.of("sonar.userHome", sonarUserHomeFromProps.toString())));

    assertThat(sonarUserHome.getPath()).isEqualTo(sonarUserHomeFromProps);
  }

}
