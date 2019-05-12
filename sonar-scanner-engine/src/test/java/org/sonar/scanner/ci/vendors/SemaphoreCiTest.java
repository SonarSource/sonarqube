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

public class SemaphoreCiTest {

  private System2 system = mock(System2.class);
  private CiVendor underTest = new SemaphoreCi(system);

  @Test
  public void getName() {
    assertThat(underTest.getName()).isEqualTo("SemaphoreCI");
  }

  @Test
  public void isDetected() {
    setEnvVariable("SEMAPHORE", "true");
    setEnvVariable("SEMAPHORE_PROJECT_ID", "d782a107-aaf9-494d-8153-206b1a6bc8e9");
    assertThat(underTest.isDetected()).isTrue();

    setEnvVariable("SEMAPHORE", "true");
    setEnvVariable("SEMAPHORE_PROJECT_ID", null);
    assertThat(underTest.isDetected()).isFalse();

    setEnvVariable("SEMAPHORE", null);
    setEnvVariable("SEMAPHORE_PROJECT_ID", "d782a107-aaf9-494d-8153-206b1a6bc8e9");
    assertThat(underTest.isDetected()).isFalse();

    setEnvVariable("SEMAPHORE", "foo");
    setEnvVariable("SEMAPHORE_PROJECT_ID", "d782a107-aaf9-494d-8153-206b1a6bc8e9");
    assertThat(underTest.isDetected()).isFalse();
  }

  @Test
  public void loadConfiguration() {
    setEnvVariable("SEMAPHORE", "true");
    setEnvVariable("SEMAPHORE_PROJECT_ID", "d782a107-aaf9-494d-8153-206b1a6bc8e9");
    setEnvVariable("SEMAPHORE_GIT_SHA", "abd12fc");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("abd12fc");
  }

  private void setEnvVariable(String key, @Nullable String value) {
    when(system.envVariable(key)).thenReturn(value);
  }
}
