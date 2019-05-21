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

public class AzureDevopsTest {

  private System2 system = mock(System2.class);
  private CiVendor underTest = new AzureDevops(system);

  @Test
  public void getName() {
    assertThat(underTest.getName()).isEqualTo("Azure DevOps");
  }

  @Test
  public void isDetected() {
    setEnvVariable("TF_BUILD", "True");
    assertThat(underTest.isDetected()).isTrue();

    setEnvVariable("TF_BUILD", "true");
    assertThat(underTest.isDetected()).isTrue();

    setEnvVariable("CI", "true");
    setEnvVariable("APPVEYOR", null);
    setEnvVariable("TF_BUILD", null);
    assertThat(underTest.isDetected()).isFalse();
  }

  @Test
  public void loadConfiguration_on_branch() {
    setEnvVariable("BUILD_SOURCEVERSION", "abd12fc");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("abd12fc");
  }

  @Test
  public void loadConfiguration_on_pull_request() {
    setEnvVariable("BUILD_SOURCEVERSION", "0e648ea");
    setEnvVariable("SYSTEM_PULLREQUEST_PULLREQUESTID", "12");
    setEnvVariable("SYSTEM_PULLREQUEST_PULLREQUESTITERATION", "5");
    setEnvVariable("SYSTEM_PULLREQUEST_SOURCEBRANCH", "refs/heads/azure-pipelines");
    setEnvVariable("SYSTEM_PULLREQUEST_SOURCECOMMITID", "a0e1e4c");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("a0e1e4c");
  }

  private void setEnvVariable(String key, @Nullable String value) {
    when(system.envVariable(key)).thenReturn(value);
  }

}
