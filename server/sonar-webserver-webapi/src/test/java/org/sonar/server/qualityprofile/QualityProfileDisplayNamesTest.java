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
package org.sonar.server.qualityprofile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QualityProfileDisplayNamesTest {

  @Test
  void toDisplayName_whenInternalNameIsMapped_shouldReturnDisplayName() {
    assertThat(QualityProfileDisplayNames.toDisplayName("Sonar way")).isEqualTo("Sonar way comprehensive");
  }

  @Test
  void toDisplayName_whenInternalNameIsNotMapped_shouldReturnItUnchanged() {
    assertThat(QualityProfileDisplayNames.toDisplayName("My Company Profile")).isEqualTo("My Company Profile");
    // the derived variants are renamed at the source, not through this mapping
    assertThat(QualityProfileDisplayNames.toDisplayName("Sonar way core")).isEqualTo("Sonar way core");
    assertThat(QualityProfileDisplayNames.toDisplayName("Sonar way extended")).isEqualTo("Sonar way extended");
  }

  @Test
  void toDisplayName_whenNull_shouldReturnNull() {
    assertThat(QualityProfileDisplayNames.toDisplayName(null)).isNull();
  }

  @Test
  void toInternalName_whenDisplayNameIsMapped_shouldReturnInternalName() {
    assertThat(QualityProfileDisplayNames.toInternalName("Sonar way comprehensive")).isEqualTo("Sonar way");
  }

  @Test
  void toInternalName_whenDisplayNameIsNotMapped_shouldReturnItUnchanged() {
    assertThat(QualityProfileDisplayNames.toInternalName("My Company Profile")).isEqualTo("My Company Profile");
  }

  @Test
  void toInternalName_whenNull_shouldReturnNull() {
    assertThat(QualityProfileDisplayNames.toInternalName(null)).isNull();
  }

  @Test
  void toDisplayName_and_toInternalName_shouldRoundTrip() {
    assertThat(QualityProfileDisplayNames.toInternalName(QualityProfileDisplayNames.toDisplayName("Sonar way"))).isEqualTo("Sonar way");
  }
}
