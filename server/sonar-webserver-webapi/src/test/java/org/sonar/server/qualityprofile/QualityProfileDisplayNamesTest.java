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
    assertThat(QualityProfileDisplayNames.toDisplayName("Sonar way")).isEqualTo("Deep");
    assertThat(QualityProfileDisplayNames.toDisplayName("Sonar way essentials")).isEqualTo("Core");
    assertThat(QualityProfileDisplayNames.toDisplayName("Sonar way balanced")).isEqualTo("Extended");
  }

  @Test
  void toDisplayName_whenInternalNameIsNotMapped_shouldReturnItUnchanged() {
    assertThat(QualityProfileDisplayNames.toDisplayName("My Company Profile")).isEqualTo("My Company Profile");
  }

  @Test
  void toDisplayName_whenNull_shouldReturnNull() {
    assertThat(QualityProfileDisplayNames.toDisplayName(null)).isNull();
  }

  @Test
  void toInternalName_whenDisplayNameIsMapped_shouldReturnInternalName() {
    assertThat(QualityProfileDisplayNames.toInternalName("Deep")).isEqualTo("Sonar way");
    assertThat(QualityProfileDisplayNames.toInternalName("Core")).isEqualTo("Sonar way essentials");
    assertThat(QualityProfileDisplayNames.toInternalName("Extended")).isEqualTo("Sonar way balanced");
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
    for (String internalName : new String[] {"Sonar way", "Sonar way essentials", "Sonar way balanced"}) {
      assertThat(QualityProfileDisplayNames.toInternalName(QualityProfileDisplayNames.toDisplayName(internalName))).isEqualTo(internalName);
    }
  }
}
