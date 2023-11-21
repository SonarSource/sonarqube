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

import java.util.List;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.CleanCodeAttributeCategory;
import org.sonar.api.rules.RuleType;
import org.sonar.db.issue.ImpactDto;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleForIndexingDtoTest {

  @Test
  public void fromRuleDto_whenCleanCodeAttributeSet_setCleanCodeCategory() {
    RuleDto ruleDto = RuleTesting.newRuleWithoutDescriptionSection();
    ruleDto.setCleanCodeAttribute(CleanCodeAttribute.FOCUSED);
    ImpactDto impactDto = new ImpactDto().setSeverity(Severity.HIGH).setSoftwareQuality(SoftwareQuality.SECURITY);
    ruleDto.replaceAllDefaultImpacts(List.of(impactDto));

    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);

    assertThat(ruleForIndexingDto.getCleanCodeAttributeCategory()).isEqualTo(CleanCodeAttributeCategory.ADAPTABLE.name());
    ImpactDto impact = ruleForIndexingDto.getImpacts().iterator().next();

    assertThat(impact.getSeverity()).isEqualTo(Severity.HIGH);
    assertThat(impact.getSoftwareQuality()).isEqualTo(SoftwareQuality.SECURITY);
  }

  @Test
  public void fromRuleDto_whenAdHocRule_setAdHocFields() {
    RuleDto ruleDto = RuleTesting.newRuleWithoutDescriptionSection();
    ruleDto.setIsAdHoc(true);
    ruleDto.setAdHocType(RuleType.BUG);

    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);

    assertThat(ruleForIndexingDto.isAdHoc()).isTrue();
    assertThat(ruleForIndexingDto.getAdHocType()).isEqualTo(RuleType.BUG.getDbConstant());
  }
}
