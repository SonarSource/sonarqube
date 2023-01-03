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
package org.sonar.scanner.ci;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CiConfigurationImplTest {

  @Test
  public void getScmRevision() {
    assertThat(new CiConfigurationImpl(null, "test").getScmRevision()).isEmpty();
    assertThat(new CiConfigurationImpl("", "test").getScmRevision()).isEmpty();
    assertThat(new CiConfigurationImpl("   ", "test").getScmRevision()).isEmpty();
    assertThat(new CiConfigurationImpl("a7bdf2d", "test").getScmRevision()).hasValue("a7bdf2d");
  }

  @Test
  public void getNam_for_undetected_ci() {
    assertThat(new CiConfigurationProvider.EmptyCiConfiguration().getCiName()).isEqualTo("undetected");
  }

  @Test
  public void getName_for_detected_ci() {
    assertThat(new CiConfigurationImpl(null, "test").getCiName()).isEqualTo("test");
  }
}
