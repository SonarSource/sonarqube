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

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

public class OneIssuePerTestFileSensor implements Sensor {

  public static final String RULE_KEY = "OneIssuePerTestFile";

  private final FileSystem fs;
  private final ActiveRules activeRules;

  public OneIssuePerTestFileSensor(FileSystem fs, ActiveRules activeRules) {
    this.fs = fs;
    this.activeRules = activeRules;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(Xoo.KEY)
      .onlyOnFileType(Type.TEST)
      .createIssuesForRuleRepository(XooRulesDefinition.XOO_REPOSITORY);
  }

  @Override
  public void execute(SensorContext context) {
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY);
    if (activeRules.find(ruleKey) == null) {
      return;
    }
    FilePredicates p = fs.predicates();
    for (InputFile inputFile : fs.inputFiles(p.and(p.hasLanguages(Xoo.KEY), p.hasType(Type.TEST)))) {
      processFile(inputFile, context, ruleKey);
    }
  }

  private void processFile(InputFile inputFile, SensorContext context, RuleKey ruleKey) {
    NewIssue newIssue = context.newIssue();
    newIssue
      .forRule(ruleKey)
      .at(newIssue.newLocation().message("This issue is generated on each test file")
        .on(inputFile))
      .save();
  }

}
