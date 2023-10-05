/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.rule;

import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.RuleImpactChangeDto;
import org.sonarqube.ws.Common;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleChangeDaoIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final RuleChangeDao underTest = db.getDbClient().ruleChangeDao();

  @Test
  public void insert_shouldInsertRuleChangeWithNullableImpacts() {
    RuleChangeDto ruleChangeDto = new RuleChangeDto();
    ruleChangeDto.setNewCleanCodeAttribute(CleanCodeAttribute.CLEAR);
    ruleChangeDto.setOldCleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL);
    ruleChangeDto.setRuleUuid("ruleUuid");
    ruleChangeDto.setUuid("uuid");

    RuleImpactChangeDto ruleImpactChangeDto = new RuleImpactChangeDto();
    ruleImpactChangeDto.setNewSoftwareQuality(Common.SoftwareQuality.RELIABILITY.name());
    ruleImpactChangeDto.setOldSoftwareQuality(Common.SoftwareQuality.RELIABILITY.name());
    ruleImpactChangeDto.setNewSeverity(Severity.LOW.name());
    ruleImpactChangeDto.setOldSeverity(Severity.HIGH.name());

    RuleImpactChangeDto ruleImpactChangeDto2 = new RuleImpactChangeDto();
    ruleImpactChangeDto2.setNewSoftwareQuality(Common.SoftwareQuality.SECURITY.name());
    ruleImpactChangeDto2.setNewSeverity(Severity.MEDIUM.name());

    RuleImpactChangeDto ruleImpactChangeDto3 = new RuleImpactChangeDto();
    ruleImpactChangeDto2.setOldSoftwareQuality(Common.SoftwareQuality.MAINTAINABILITY.name());
    ruleImpactChangeDto2.setOldSeverity(Severity.MEDIUM.name());

    Set<RuleImpactChangeDto> impactChanges = Set.of(ruleImpactChangeDto, ruleImpactChangeDto2, ruleImpactChangeDto3);
    impactChanges.forEach(i -> i.setRuleChangeUuid(ruleChangeDto.getUuid()));

    ruleChangeDto.setRuleImpactChangeDtos(impactChanges);
    DbSession session = db.getSession();

    underTest.insert(session, ruleChangeDto);
    session.commit();

    assertThat(db.select("select * from rule_impact_changes")).hasSize(3);
    assertThat(db.select("select * from rule_changes")).hasSize(1);
  }
}
