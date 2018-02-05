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
package org.sonar.foo.rule;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.foo.Foo;

import static org.sonar.foo.rule.FooRulesDefinition.FOO_REPOSITORY;

public class RekeyingRulesSensor implements Sensor {
  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.createIssuesForRuleRepositories(FOO_REPOSITORY)
      .onlyOnLanguage(Foo.KEY)
      .name("Sensor generating one issue per Foo file for re-keyed rules");

  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile inputFile : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguage(Foo.KEY))) {
      NewIssue newIssue = context.newIssue();
      newIssue.at(newIssue.newLocation().on(inputFile))
        .forRule(RuleKey.of(FOO_REPOSITORY, "ToBeRenamed"))
        .save();
      newIssue = context.newIssue();
      newIssue.at(newIssue.newLocation().on(inputFile))
        .forRule(RuleKey.of(FOO_REPOSITORY, "ToBeRenamedAndMoved"))
        .save();
    }
  }
}
