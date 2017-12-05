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

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.Xoo2;

public class OneBugIssuePerLineSensor implements Sensor {

  public static final String RULE_KEY = "OneBugIssuePerLine";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("One Bug Issue Per Line")
      .onlyOnLanguages(Xoo.KEY, Xoo2.KEY)
      .createIssuesForRuleRepositories(XooRulesDefinition.XOO_REPOSITORY, XooRulesDefinition.XOO2_REPOSITORY);
  }

  @Override
  public void execute(SensorContext context) {
    analyse(context, Xoo.KEY, XooRulesDefinition.XOO_REPOSITORY);
  }

  private void analyse(SensorContext context, String language, String repo) {
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    for (InputFile file : fs.inputFiles(p.and(p.hasLanguages(language), p.hasType(Type.MAIN)))) {
      createIssues(file, context, repo);
    }
  }

  private static void createIssues(InputFile file, SensorContext context, String repo) {
    RuleKey ruleKey = RuleKey.of(repo, RULE_KEY);
    for (int line = 1; line <= file.lines(); line++) {
      TextRange text = file.selectLine(line);
      // do not count empty lines, which can be a pain with end-of-file return
      if (text.end().lineOffset() == 0) {
        continue;
      }
      NewIssue newIssue = context.newIssue();
      newIssue
        .forRule(ruleKey)
        .at(newIssue.newLocation()
          .on(file)
          .at(text)
          .message("This bug issue is generated on each line"))
        .save();
    }
  }

}
