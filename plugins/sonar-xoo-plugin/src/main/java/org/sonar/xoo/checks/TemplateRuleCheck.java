/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.xoo.checks;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Cardinality;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

@Rule(key = TemplateRuleCheck.RULE_KEY, cardinality = Cardinality.MULTIPLE, name = "Template rule", description = "Sample template rule")
public class TemplateRuleCheck implements Check {

  public static final String RULE_KEY = "TemplateRule";

  @RuleProperty(key = "line")
  private int line;

  @Override
  public void execute(SensorContext sensorContext, InputFile file, RuleKey ruleKey) {
    NewIssue newIssue = sensorContext.newIssue();
    newIssue
      .forRule(ruleKey)
      .at(newIssue.newLocation()
        .on(file)
        .at(file.selectLine(line)))
      .save();
  }

}
