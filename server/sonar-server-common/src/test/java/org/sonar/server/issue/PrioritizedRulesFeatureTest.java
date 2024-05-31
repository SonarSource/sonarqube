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

package org.sonar.server.issue;

import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;

import static org.junit.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class PrioritizedRulesFeatureTest {
  SonarRuntime sonarRuntime = mock();
  private final PrioritizedRulesFeature underTest = new PrioritizedRulesFeature(sonarRuntime);

  @Test
  void isAvailable_shouldOnlyBeEnabledInEnterpriseEditionPlus() {
    testForEdition(SonarEdition.COMMUNITY, false);
    testForEdition(SonarEdition.DEVELOPER, false);
    testForEdition(SonarEdition.ENTERPRISE, true);
    testForEdition(SonarEdition.DATACENTER, true);
  }

  private void testForEdition(SonarEdition edition, boolean expectedResult) {
    doReturn(edition).when(sonarRuntime).getEdition();
    assertThat(underTest.isAvailable()).isEqualTo(expectedResult);
  }

  @Test
  void getName_ShouldReturn_Announcement() {
    assertEquals("prioritized-rules", underTest.getName());
  }

}
