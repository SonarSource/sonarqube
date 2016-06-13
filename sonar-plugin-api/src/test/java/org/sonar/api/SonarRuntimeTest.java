/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarRuntimeTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void isGteInSQ() {
    Version version = Version.parse("1.2.3");
    SonarRuntime apiVersion = new SonarRuntime(version, SonarProduct.SONARQUBE, SonarQubeSide.SCANNER);
    assertThat(apiVersion.getApiVersion()).isEqualTo(version);
    assertThat(apiVersion.getProduct()).isEqualTo(SonarProduct.SONARQUBE);
    assertThat(apiVersion.getSonarQubeSide()).isEqualTo(SonarQubeSide.SCANNER);
    assertThat(apiVersion.isGreaterThanOrEqual(version)).isTrue();
    assertThat(apiVersion.isGreaterThanOrEqual(Version.parse("1.1"))).isTrue();
    assertThat(apiVersion.isGreaterThanOrEqual(Version.parse("1.3"))).isFalse();
  }

  @Test
  public void inSL() {
    Version version = Version.parse("1.2.3");
    SonarRuntime apiVersion = new SonarRuntime(version, SonarProduct.SONARLINT, null);
    assertThat(apiVersion.getApiVersion()).isEqualTo(version);
    assertThat(apiVersion.getProduct()).isEqualTo(SonarProduct.SONARLINT);
    assertThat(apiVersion.isGreaterThanOrEqual(version)).isTrue();
    assertThat(apiVersion.isGreaterThanOrEqual(Version.parse("1.1"))).isTrue();
    assertThat(apiVersion.isGreaterThanOrEqual(Version.parse("1.3"))).isFalse();
    try {
      apiVersion.getSonarQubeSide();
      Assertions.fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorMissSide() throws Exception {
    new SonarRuntime(Version.parse("1.2.3"), SonarProduct.SONARQUBE, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNoSideOnSonarLint() throws Exception {
    new SonarRuntime(Version.parse("1.2.3"), SonarProduct.SONARLINT, SonarQubeSide.COMPUTE_ENGINE);
  }
}
