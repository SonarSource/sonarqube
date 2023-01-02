/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

public class AwsCodeBuildTest {

  private final System2 system = mock(System2.class);
  private final CiVendor underTest = new AwsCodeBuild(system);

  @Test
  public void getName() {
    assertThat(underTest.getName()).isEqualTo("AwsCodeBuild");
  }

  @Test
  public void isDetected() {
    assertThat(underTest.isDetected()).isFalse();

    setEnvVariable("CODEBUILD_BUILD_ID", "51");
    assertThat(underTest.isDetected()).isFalse();

    setEnvVariable("CODEBUILD_BUILD_ID", "52");
    setEnvVariable("CODEBUILD_START_TIME", "some-time");
    assertThat(underTest.isDetected()).isTrue();
  }

  @Test
  public void loadConfiguration() {
    setEnvVariable("CODEBUILD_BUILD_ID", "51");
    setEnvVariable("CODEBUILD_START_TIME", "some-time");

    assertThat(underTest.loadConfiguration().getScmRevision()).isEmpty();
  }

  private void setEnvVariable(String key, @Nullable String value) {
    when(system.envVariable(key)).thenReturn(value);
  }
}
