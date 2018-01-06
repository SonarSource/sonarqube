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
package org.sonar.plugins.backdating.rule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;

public class BackSensorV1 implements Sensor {

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.createIssuesForRuleRepositories(BackRulesDefinition.BACK_REPOSITORY)
      .onlyOnLanguage("xoo")
      .name("Back V1");
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile inputFile : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguage("xoo"))) {
      int lineNb = 0;
      try {
        BufferedReader reader = new BufferedReader(new StringReader(inputFile.contents()));
        String line;
        while ((line = reader.readLine()) != null) {
          lineNb++;
          if (line.contains("BACKV1")) {
            NewIssue newIssue = context.newIssue();
            newIssue.at(newIssue.newLocation().on(inputFile).at(inputFile.selectLine(lineNb)))
              .forRule(RuleKey.of(BackRulesDefinition.BACK_REPOSITORY, BackRulesDefinition.RULE_KEY))
              .save();
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

}
