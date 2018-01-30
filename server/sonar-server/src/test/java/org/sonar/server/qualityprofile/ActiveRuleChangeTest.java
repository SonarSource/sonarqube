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
package org.sonar.server.qualityprofile;

import java.util.Random;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;

public class ActiveRuleChangeTest {

  private static final String A_LOGIN = "A_LOGIN";

  @Test
  public void toDto() {
    QProfileDto profile = newQualityProfileDto();
    ActiveRuleKey key = ActiveRuleKey.of(profile, RuleKey.of("P1", "R1"));
    int ruleId = new Random().nextInt(963);
    ActiveRuleChange underTest = new ActiveRuleChange(ACTIVATED, key, new RuleDefinitionDto().setId(ruleId));

    QProfileChangeDto result = underTest.toDto(A_LOGIN);

    assertThat(result.getChangeType()).isEqualTo(ACTIVATED.name());
    assertThat(result.getRulesProfileUuid()).isEqualTo(profile.getRulesProfileUuid());
    assertThat(result.getLogin()).isEqualTo(A_LOGIN);
    assertThat(result.getDataAsMap().get("ruleId")).isEqualTo(String.valueOf(ruleId));
  }
}
