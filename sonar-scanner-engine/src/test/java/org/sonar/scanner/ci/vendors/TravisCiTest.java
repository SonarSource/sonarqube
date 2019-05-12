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

public class TravisCiTest {

  private System2 system = mock(System2.class);
  private CiVendor underTest = new TravisCi(system);

  @Test
  public void getName() {
    assertThat(underTest.getName()).isEqualTo("TravisCI");
  }

  @Test
  public void isDetected() {
    setEnvVariable("CI", "true");
    setEnvVariable("TRAVIS", "true");
    assertThat(underTest.isDetected()).isTrue();

    setEnvVariable("CI", "true");
    setEnvVariable("DRONE", "true");
    setEnvVariable("TRAVIS", null);
    assertThat(underTest.isDetected()).isFalse();
  }

  @Test
  public void loadConfiguration_of_branch() {
    setEnvVariable("CI", "true");
    setEnvVariable("TRAVIS", "true");
    setEnvVariable("TRAVIS_COMMIT", "abd12fc");
    setEnvVariable("TRAVIS_PULL_REQUEST", "false");
    setEnvVariable("TRAVIS_PULL_REQUEST_SHA", "");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("abd12fc");
  }

  @Test
  public void loadConfiguration_of_pull_request() {
    setEnvVariable("CI", "true");
    setEnvVariable("TRAVIS", "true");
    setEnvVariable("TRAVIS_COMMIT", "abd12fc");
    setEnvVariable("TRAVIS_PULL_REQUEST", "1234");
    setEnvVariable("TRAVIS_PULL_REQUEST_SHA", "987fbd3");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("987fbd3");
  }

  private void setEnvVariable(String key, @Nullable String value) {
    when(system.envVariable(key)).thenReturn(value);
  }
}
