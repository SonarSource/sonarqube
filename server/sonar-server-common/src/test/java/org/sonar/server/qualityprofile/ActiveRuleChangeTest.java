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
package org.sonar.server.qualityprofile;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.util.Uuids;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;

public class ActiveRuleChangeTest {

  private static final String A_USER_UUID = "A_USER_UUID";

  @Test
  public void toDto() {
    QProfileDto profile = newQualityProfileDto();
    ActiveRuleKey key = ActiveRuleKey.of(profile, RuleKey.of("P1", "R1"));
    String ruleUuid = Uuids.createFast();
    ActiveRuleChange underTest = new ActiveRuleChange(ACTIVATED, key, new RuleDto().setUuid(ruleUuid));

    QProfileChangeDto result = underTest.toDto(A_USER_UUID);

    assertThat(result.getChangeType()).isEqualTo(ACTIVATED.name());
    assertThat(result.getRulesProfileUuid()).isEqualTo(profile.getRulesProfileUuid());
    assertThat(result.getUserUuid()).isEqualTo(A_USER_UUID);
    assertThat(result.getDataAsMap()).containsEntry("ruleUuid", ruleUuid);
  }
}
