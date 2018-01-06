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
package org.sonar.api;

import org.junit.Test;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubeVersionTest {

  @Test
  public void isGte() {
    Version version = Version.parse("1.2.3");
    SonarQubeVersion qubeVersion = new SonarQubeVersion(version);
    assertThat(qubeVersion.get()).isEqualTo(version);
    assertThat(qubeVersion.isGreaterThanOrEqual(version)).isTrue();
    assertThat(qubeVersion.isGreaterThanOrEqual(Version.parse("1.1"))).isTrue();
    assertThat(qubeVersion.isGreaterThanOrEqual(Version.parse("1.3"))).isFalse();
  }

}
