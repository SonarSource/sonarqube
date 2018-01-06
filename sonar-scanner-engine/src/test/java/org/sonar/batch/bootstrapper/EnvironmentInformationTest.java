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
package org.sonar.batch.bootstrapper;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvironmentInformationTest {
  @Test
  public void test_bean() {
    EnvironmentInformation env = new EnvironmentInformation("Maven Plugin", "2.0");

    assertThat(env.getKey()).isEqualTo("Maven Plugin");
    assertThat(env.getVersion()).isEqualTo("2.0");
  }

  @Test
  public void test_toString() {
    EnvironmentInformation env = new EnvironmentInformation("Maven Plugin", "2.0");

    assertThat(env.toString()).isEqualTo("Maven Plugin/2.0");
  }
}
