/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.xoo.Xoo;

public class OneExternalIssueOnProjectSensor implements Sensor {
  public static final String ENGINE_ID = "XooEngine";
  public static final String SEVERITY = "MAJOR";
  public static final RuleType TYPE = RuleType.BUG;
  public static final String ACTIVATE = "sonar.oneExternalIssueOnProject.activate";
  public static final String RULE_ID = "OneExternalIssueOnProject";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("One External Issue At Project Level")
      .onlyOnLanguages(Xoo.KEY)
      .onlyWhenConfiguration(c -> c.getBoolean(ACTIVATE).orElse(false));
  }

  @Override
  public void execute(SensorContext context) {
    analyse(context);
  }

  private static void analyse(SensorContext context) {
    NewExternalIssue newIssue = context.newExternalIssue();
    newIssue
      .engineId(ENGINE_ID)
      .ruleId(RULE_ID)
      .at(newIssue.newLocation()
        .on(context.project())
        .message("This issue is generated at project level"))
      .severity(Severity.valueOf(SEVERITY))
      .type(TYPE)
      .save();
  }
}
