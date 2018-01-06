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
package org.sonar.xoo.rule;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

public class OneIssuePerModuleSensor implements Sensor {

  public static final String RULE_KEY = "OneIssuePerModule";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("One Issue Per Module")
      .onlyOnLanguages(Xoo.KEY)
      .createIssuesForRuleRepositories(XooRulesDefinition.XOO_REPOSITORY);
  }

  @Override
  public void execute(SensorContext context) {
    analyse(context, Xoo.KEY, XooRulesDefinition.XOO_REPOSITORY);
  }

  private void analyse(SensorContext context, String language, String repo) {
    RuleKey ruleKey = RuleKey.of(repo, RULE_KEY);
    NewIssue newIssue = context.newIssue();
    newIssue
      .forRule(ruleKey)
      .at(newIssue.newLocation()
        .on(context.module())
        .message("This issue is generated on each module"))
      .save();
  }

}
