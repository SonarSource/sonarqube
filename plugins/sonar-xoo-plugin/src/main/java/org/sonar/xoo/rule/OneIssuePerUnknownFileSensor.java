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
package org.sonar.xoo.rule;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;

public class OneIssuePerUnknownFileSensor implements Sensor {

  public static final String RULE_KEY = "OneIssuePerUnknownFile";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("One Issue Per Unknown File");
  }

  @Override
  public void execute(SensorContext context) {
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY);
    FilePredicate unknownFilesPredicate = context.fileSystem().predicates().matchesPathPattern("**/*.unknown");
    Iterable<InputFile> unknownFiles = context.fileSystem().inputFiles(unknownFilesPredicate);

    unknownFiles.forEach(inputFile -> {
      NewIssue newIssue = context.newIssue();
      newIssue
        .forRule(ruleKey)
        .at(newIssue.newLocation()
          .on(inputFile)
          .message("This issue is generated on each file with extension 'unknown'"))
        .save();
    });
  }

}
