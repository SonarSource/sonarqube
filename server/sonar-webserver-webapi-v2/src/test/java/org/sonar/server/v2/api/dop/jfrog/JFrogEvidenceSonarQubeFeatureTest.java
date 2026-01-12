/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.dop.jfrog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JFrogEvidenceSonarQubeFeatureTest {

  @Test
  void getName_returnsJfrogEvidence() {
    var feature = new JFrogEvidenceSonarQubeFeature(mock());

    assertThat(feature.getName()).isEqualTo("jfrog-evidence");
  }

  @ParameterizedTest
  @CsvSource({
    "COMMUNITY,false",
    "DEVELOPER,false",
    "ENTERPRISE,true",
    "DATACENTER,true"
  })
  void isAvailable_checksEdition(SonarEdition edition, boolean expected) {
    var runtime = mock(SonarRuntime.class);
    when(runtime.getEdition()).thenReturn(edition);
    var feature = new JFrogEvidenceSonarQubeFeature(runtime);

    assertThat(feature.isAvailable()).isEqualTo(expected);
  }

}
