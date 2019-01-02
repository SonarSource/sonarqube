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
package org.sonar.xoo.rule;

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.xoo.Xoo;

public class OnePredefinedAndAdHocRuleExternalIssuePerLineSensor implements Sensor {

  public static final String ACTIVATE = "sonar.onePredefinedAndAdHocRuleExternalIssuePerLine.activate";
  private static final String NAME = "One External Issue Per Line With A Predefined And An AdHoc Rule";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name(NAME)
      .onlyOnLanguages(Xoo.KEY)
      .onlyWhenConfiguration(c -> c.getBoolean(ACTIVATE).orElse(false));
  }

  @Override
  public void execute(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    for (InputFile file : fs.inputFiles(p.and(p.hasLanguages(Xoo.KEY), p.hasType(Type.MAIN)))) {
      createIssues(file, context);
    }
  }

  private static void createIssues(InputFile file, SensorContext context) {
    for (int line = 1; line <= file.lines(); line++) {
      NewExternalIssue newIssue = context.newExternalIssue();
      newIssue
        .engineId(OnePredefinedRuleExternalIssuePerLineSensor.ENGINE_ID)
        .ruleId(OnePredefinedRuleExternalIssuePerLineSensor.RULE_ID)
        .at(newIssue.newLocation()
          .on(file)
          .at(file.selectLine(line))
          .message("This issue is generated on each line and the rule is predefined"))
        .severity(Severity.valueOf(OnePredefinedRuleExternalIssuePerLineSensor.SEVERITY))
        .remediationEffortMinutes(OnePredefinedRuleExternalIssuePerLineSensor.EFFORT)
        .type(OnePredefinedRuleExternalIssuePerLineSensor.TYPE)
        .save();

      // Even if the issue is on a predefined rule, the sensor is declaring an adHoc rule => this info should be ignored
      context.newAdHocRule()
        .engineId(OnePredefinedRuleExternalIssuePerLineSensor.ENGINE_ID)
        .ruleId(OnePredefinedRuleExternalIssuePerLineSensor.RULE_ID)
        .name("An ad hoc rule")
        .description("blah blah")
        .severity(Severity.BLOCKER)
        .type(RuleType.BUG)
        .save();
    }
  }
}
