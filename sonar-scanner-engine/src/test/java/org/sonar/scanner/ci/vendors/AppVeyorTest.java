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
package org.sonar.scanner.ci.vendors;

import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.scanner.ci.CiVendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppVeyorTest {

  private System2 system = mock(System2.class);
  private CiVendor underTest = new AppVeyor(system);

  @Test
  public void getName() {
    assertThat(underTest.getName()).isEqualTo("AppVeyor");
  }

  @Test
  public void isDetected() {
    setEnvVariable("CI", "true");
    setEnvVariable("APPVEYOR", "true");
    assertThat(underTest.isDetected()).isTrue();

    // on Windows
    setEnvVariable("CI", "True");
    setEnvVariable("APPVEYOR", "True");
    assertThat(underTest.isDetected()).isTrue();

    setEnvVariable("CI", "true");
    setEnvVariable("SEMAPHORE", "true");
    setEnvVariable("APPVEYOR", null);
    assertThat(underTest.isDetected()).isFalse();

    setEnvVariable("CI", "false");
    setEnvVariable("APPVEYOR", "true");
    assertThat(underTest.isDetected()).isFalse();
  }

  @Test
  public void loadConfiguration() {
    setEnvVariable("CI", "true");
    setEnvVariable("APPVEYOR", "true");
    setEnvVariable("APPVEYOR_REPO_COMMIT", "abd12fc");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("abd12fc");
  }

  private void setEnvVariable(String key, @Nullable String value) {
    when(system.envVariable(key)).thenReturn(value);
  }
}
