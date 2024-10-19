/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.core.platform;

import org.junit.Test;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubeVersionTest {

  @Test
  public void verify_methods() {
    var version = Version.create(9, 5);
    SonarQubeVersion underTest = new SonarQubeVersion(version);
    assertThat(underTest).extracting(SonarQubeVersion::toString, SonarQubeVersion::get)
      .containsExactly("9.5", version);

    var otherVersion = Version.create(8, 5);
    assertThat(underTest.isGreaterThanOrEqual(otherVersion)).isTrue();
  }
}
