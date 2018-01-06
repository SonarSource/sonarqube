/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.internal;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;


public class SonarRuntimeImplTest {

  private static final Version A_VERSION = Version.parse("6.0");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void sonarQube_environment() {
    SonarRuntime apiVersion = SonarRuntimeImpl.forSonarQube(A_VERSION, SonarQubeSide.SCANNER);
    assertThat(apiVersion.getApiVersion()).isEqualTo(A_VERSION);
    assertThat(apiVersion.getProduct()).isEqualTo(SonarProduct.SONARQUBE);
    assertThat(apiVersion.getSonarQubeSide()).isEqualTo(SonarQubeSide.SCANNER);
  }

  @Test
  public void sonarLint_environment() {
    SonarRuntime apiVersion = SonarRuntimeImpl.forSonarLint(A_VERSION);
    assertThat(apiVersion.getApiVersion()).isEqualTo(A_VERSION);
    assertThat(apiVersion.getProduct()).isEqualTo(SonarProduct.SONARLINT);
    try {
      apiVersion.getSonarQubeSide();
      Assertions.fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void sonarqube_requires_side() throws Exception {
    SonarRuntimeImpl.forSonarQube(A_VERSION, null);
  }



}
