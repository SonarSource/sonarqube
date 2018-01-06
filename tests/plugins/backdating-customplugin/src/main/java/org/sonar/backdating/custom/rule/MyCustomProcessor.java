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
package org.sonar.backdating.custom.rule;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.backdating.api.CustomProcessor;

public class MyCustomProcessor implements CustomProcessor {

  @Override
  public void process(String lineContent, SensorContext context, InputFile inputFile, int line) {
    if (lineContent.contains("BACKCUSTOM")) {
      NewIssue newIssue = context.newIssue();
      newIssue.at(newIssue.newLocation().on(inputFile).at(inputFile.selectLine(line)))
        .forRule(RuleKey.of(CustomRulesDefinition.BACK_REPOSITORY, CustomRulesDefinition.RULE_KEY))
        .save();
    }
  }

}
