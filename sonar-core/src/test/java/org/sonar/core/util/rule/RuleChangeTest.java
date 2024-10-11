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
package org.sonar.core.util.rule;

import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.util.ParamChange;

import static org.assertj.core.api.Assertions.assertThat;

class RuleChangeTest {

  @Test
  void valuesAreStoredAndReturnedCorrectly() {
    RuleChange ruleChange = new RuleChange();

    ruleChange.setKey("key");
    ruleChange.setTemplateKey("templateKey");
    ruleChange.setLanguage("language");
    ruleChange.setSeverity("severity");
    ruleChange.setParams(new ParamChange[]{new ParamChange("paramKey", "paramValue")});
    ruleChange.addImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH);

    assertThat(ruleChange).extracting(
        RuleChange::getKey,
        RuleChange::getTemplateKey,
        RuleChange::getLanguage,
        RuleChange::getSeverity)
      .containsExactly(
        "key",
        "templateKey",
        "language",
        "severity");

    assertThat(ruleChange.getParams()).hasSize(1);
    assertThat(ruleChange.getParams()[0])
      .extracting(ParamChange::getKey, ParamChange::getValue)
      .containsExactly("paramKey", "paramValue");

    assertThat(ruleChange.getImpacts()).hasSize(1);
    assertThat(ruleChange.getImpacts().get(0))
      .extracting(RuleChange.Impact::getSoftwareQuality, RuleChange.Impact::getSeverity)
      .containsExactly(SoftwareQuality.MAINTAINABILITY, Severity.HIGH);
  }
}
