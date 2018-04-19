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
package org.sonarqube.qa.bluegreen;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;

import static org.sonarqube.qa.bluegreen.RulesDefinitionV2.REPOSITORY_KEY;

public class SensorV2 implements Sensor {

  private final MetricFinder metricFinder;

  public SensorV2(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.createIssuesForRuleRepositories(REPOSITORY_KEY)
      .onlyOnLanguage("xoo")
      .name("BlueGreen V2");
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile inputFile : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguage("xoo"))) {
      saveIssue(context, inputFile, "b");
      saveIssue(context, inputFile, "c");
    }

    context.newMeasure()
      .forMetric(metricFinder.findByKey("bluegreen"))
      .on(context.module())
      .withValue(30)
      .save();
    context.newMeasure()
      .forMetric(metricFinder.findByKey("green"))
      .on(context.module())
      .withValue(40)
      .save();
  }

  private void saveIssue(SensorContext context, InputFile inputFile, String ruleKey) {
    NewIssue newIssue = context.newIssue();
    newIssue.at(newIssue.newLocation().on(inputFile).at(inputFile.selectLine(1)))
      .forRule(RuleKey.of(REPOSITORY_KEY, ruleKey))
      .save();
  }

}
