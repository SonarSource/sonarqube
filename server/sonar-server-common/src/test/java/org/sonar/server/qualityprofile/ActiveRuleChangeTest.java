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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.util.Uuids;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleImpactChangeDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.qualityprofile.QualityProfileTesting.newQualityProfileDto;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.ACTIVATED;
import static org.sonar.server.qualityprofile.ActiveRuleChange.Type.UPDATED;

public class ActiveRuleChangeTest {

  private static final String A_USER_UUID = "A_USER_UUID";

  @Test
  public void toDto() {
    QProfileDto profile = newQualityProfileDto();
    ActiveRuleKey key = ActiveRuleKey.of(profile, RuleKey.of("P1", "R1"));
    String ruleUuid = Uuids.createFast();
    ActiveRuleChange underTest = new ActiveRuleChange(UPDATED, key, new RuleDto().setUuid(ruleUuid));
    underTest.setRule(
      new RuleDto().replaceAllDefaultImpacts(List.of(new ImpactDto(SoftwareQuality.RELIABILITY, Severity.LOW), new ImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.HIGH))));
    underTest.setOldImpacts(Map.of(SoftwareQuality.SECURITY, Severity.LOW, SoftwareQuality.MAINTAINABILITY, Severity.HIGH));
    underTest.setNewImpacts(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH, SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER));

    QProfileChangeDto result = underTest.toDto(A_USER_UUID);

    assertThat(result.getChangeType()).isEqualTo(UPDATED.name());
    assertThat(result.getRulesProfileUuid()).isEqualTo(profile.getRulesProfileUuid());
    assertThat(result.getUserUuid()).isEqualTo(A_USER_UUID);
    assertThat(result.getDataAsMap()).containsEntry("ruleUuid", ruleUuid);

    Set<RuleImpactChangeDto> ruleImpactChanges = result.getRuleChange().getRuleImpactChanges();

    assertThat(ruleImpactChanges)
      .hasSize(2)
      .containsExactlyInAnyOrder(
        new RuleImpactChangeDto(SoftwareQuality.MAINTAINABILITY, SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER, Severity.HIGH),
        new RuleImpactChangeDto(SoftwareQuality.RELIABILITY, SoftwareQuality.RELIABILITY, Severity.HIGH, Severity.LOW));
  }

  @Test
  public void toDto_whenIdenticalImpacts_shouldNotReturnImpactChanges() {
    QProfileDto profile = newQualityProfileDto();
    ActiveRuleKey key = ActiveRuleKey.of(profile, RuleKey.of("P1", "R1"));
    String ruleUuid = Uuids.createFast();
    ActiveRuleChange underTest = new ActiveRuleChange(UPDATED, key, new RuleDto().setUuid(ruleUuid));
    underTest.setRule(new RuleDto().replaceAllDefaultImpacts(List.of(new ImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.HIGH))));
    underTest.setOldImpacts(Map.of(SoftwareQuality.SECURITY, Severity.LOW, SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER));
    underTest.setNewImpacts(Map.of(SoftwareQuality.SECURITY, Severity.LOW, SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER));

    QProfileChangeDto result = underTest.toDto(A_USER_UUID);

    assertThat(result.getRuleChange()).isNull();

  }

  @Test
  public void toDto_whenRuleChangeDtoIsActivated_shouldNotReturnImpactChanges() {
    QProfileDto profile = newQualityProfileDto();
    ActiveRuleKey key = ActiveRuleKey.of(profile, RuleKey.of("P1", "R1"));
    String ruleUuid = Uuids.createFast();
    ActiveRuleChange underTest = new ActiveRuleChange(ACTIVATED, key, new RuleDto().setUuid(ruleUuid));
    underTest.setRule(new RuleDto().replaceAllDefaultImpacts(List.of(new ImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.HIGH))));
    underTest.setOldImpacts(Map.of(SoftwareQuality.SECURITY, Severity.LOW, SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER));
    underTest.setNewImpacts(Map.of(SoftwareQuality.SECURITY, Severity.LOW, SoftwareQuality.MAINTAINABILITY, Severity.INFO));

    QProfileChangeDto result = underTest.toDto(A_USER_UUID);

    assertThat(result.getRuleChange().getRuleImpactChanges())
      .hasSize(1)
      .containsExactlyInAnyOrder(
        new RuleImpactChangeDto(SoftwareQuality.MAINTAINABILITY, SoftwareQuality.MAINTAINABILITY, Severity.INFO, Severity.BLOCKER));
  }

  @Test
  public void toDto_whenRuleChangeDtoIsActivatedAndSameImpacts_shouldNotReturnImpactChanges() {
    QProfileDto profile = newQualityProfileDto();
    ActiveRuleKey key = ActiveRuleKey.of(profile, RuleKey.of("P1", "R1"));
    String ruleUuid = Uuids.createFast();
    ActiveRuleChange underTest = new ActiveRuleChange(ACTIVATED, key, new RuleDto().setUuid(ruleUuid));
    underTest.setRule(new RuleDto().replaceAllDefaultImpacts(List.of(new ImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.HIGH))));
    underTest.setOldImpacts(Map.of(SoftwareQuality.SECURITY, Severity.LOW, SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER));
    underTest.setNewImpacts(Map.of(SoftwareQuality.SECURITY, Severity.LOW, SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER));

    QProfileChangeDto result = underTest.toDto(A_USER_UUID);

    assertThat(result.getRuleChange()).isNull();

  }
}
