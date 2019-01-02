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
package org.sonar.server.ui;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionFormatterTest {
  @Test
  public void format_technical_version() {
    assertThat(format("6.3")).isEqualTo("6.3");
    assertThat(format("6.3.2")).isEqualTo("6.3.2");
    assertThat(format("6.3.2.5498")).isEqualTo("6.3.2 (build 5498)");
    assertThat(format("6.3.0.5499")).isEqualTo("6.3 (build 5499)");
  }

  private static String format(String technicalVersion) {
    return VersionFormatter.format(technicalVersion);
  }
}
