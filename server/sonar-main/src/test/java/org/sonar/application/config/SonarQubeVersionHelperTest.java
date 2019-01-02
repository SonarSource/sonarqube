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
package org.sonar.application.config;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubeVersionHelperTest {
  @Test
  public void getSonarQubeVersion_must_not_return_an_empty_string() {
    assertThat(SonarQubeVersionHelper.getSonarqubeVersion()).isNotEmpty();
  }

  @Test
  public void getSonarQubeVersion_must_always_return_same_value() {
    String sonarqubeVersion = SonarQubeVersionHelper.getSonarqubeVersion();
    for (int i = 0; i < 3; i++) {
      assertThat(SonarQubeVersionHelper.getSonarqubeVersion()).isEqualTo(sonarqubeVersion);
    }
  }
}
