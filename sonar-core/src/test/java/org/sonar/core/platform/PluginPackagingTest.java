/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import static org.assertj.core.api.Assertions.assertThat;

public class PluginPackagingTest {

  @Test
  public void getFileExtension_returnsTheExtensionOfEachPackaging() {
    assertThat(PluginPackaging.JAR.getFileExtension()).isEqualTo("jar");
    assertThat(PluginPackaging.ZIP.getFileExtension()).isEqualTo("zip");
  }

  @Test
  public void fileExtensions_returnsTheExtensionOfEveryPackaging() {
    assertThat(PluginPackaging.fileExtensions()).containsExactlyInAnyOrder("jar", "zip");
  }

  @Test
  public void fileExtensions_returnsAFreshArray_soCallersCannotMutateTheSharedState() {
    String[] first = PluginPackaging.fileExtensions();
    first[0] = "tampered";

    assertThat(PluginPackaging.fileExtensions()).containsExactlyInAnyOrder("jar", "zip");
  }
}
