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
package org.sonar.db.qualityprofile;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveRuleDtoTest {

  @Test
  void setImpacts_shouldStoreAsString() {
    ActiveRuleDto activeRuleDto = new ActiveRuleDto();
    Map<SoftwareQuality, Severity> map = new LinkedHashMap<>();
    map.put(SoftwareQuality.MAINTAINABILITY, Severity.INFO);
    map.put(SoftwareQuality.RELIABILITY, Severity.INFO);

    activeRuleDto.setImpacts(map);

    assertThat(activeRuleDto.getImpactsString()).isEqualTo("{\"MAINTAINABILITY\":\"INFO\",\"RELIABILITY\":\"INFO\"}");
    assertThat(activeRuleDto.getImpacts()).containsEntry(SoftwareQuality.MAINTAINABILITY, Severity.INFO)
      .containsEntry(SoftwareQuality.RELIABILITY, Severity.INFO);
  }

  @Test
  void setImpacts_shouldReturnEmpty() {
    ActiveRuleDto activeRuleDto = new ActiveRuleDto();
    Map<SoftwareQuality, Severity> map = new LinkedHashMap<>();
    activeRuleDto.setImpacts(map);

    assertThat(activeRuleDto.getImpactsString()).isNull();
    assertThat(activeRuleDto.getImpacts()).isEmpty();
  }

}
