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
package org.sonar.scanner.externalissue.sarif;

import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.sarif.pojo.ReportingDescriptor;
import org.sonar.sarif.pojo.Result;

import static java.lang.String.join;

@ScannerSide
public class RuleMapper {

  private final SensorContext sensorContext;

  public RuleMapper(SensorContext sensorContext) {
    this.sensorContext = sensorContext;
  }

  NewAdHocRule mapRule(ReportingDescriptor rule, String driverName, @Nullable Result.Level ruleSeverity, @Nullable Result.Level ruleSeverityForNewTaxonomy) {

    NewAdHocRule newAdHocRule = sensorContext.newAdHocRule()
      .severity(ResultMapper.toSonarQubeSeverity(ruleSeverity))
      .type(ResultMapper.DEFAULT_TYPE)
      .ruleId(rule.getId())
      .engineId(driverName)
      .cleanCodeAttribute(ResultMapper.DEFAULT_CLEAN_CODE_ATTRIBUTE)
      .addDefaultImpact(ResultMapper.DEFAULT_SOFTWARE_QUALITY, ResultMapper.toSonarQubeImpactSeverity(ruleSeverityForNewTaxonomy));

    if (rule.getShortDescription() != null && !StringUtils.isBlank(rule.getShortDescription().getText())) {
      newAdHocRule.name(rule.getShortDescription().getText());
    } else {
      newAdHocRule.name(join(":", driverName, rule.getId()));
    }

    if (rule.getFullDescription() != null && !StringUtils.isBlank(rule.getFullDescription().getText())) {
      newAdHocRule.description(rule.getFullDescription().getText());
    }

    return newAdHocRule;
  }
}
