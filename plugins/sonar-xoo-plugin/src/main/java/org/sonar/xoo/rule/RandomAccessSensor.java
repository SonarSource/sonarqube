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

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

public class RandomAccessSensor implements Sensor {

  private static final String SONAR_XOO_RANDOM_ACCESS_ISSUE_PATHS = "sonar.xoo.randomAccessIssue.paths";
  public static final String RULE_KEY = "RandomAccessIssue";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("One Issue Per File with Random Access")
      .onlyOnLanguages(Xoo.KEY)
      .createIssuesForRuleRepositories(XooRulesDefinition.XOO_REPOSITORY)
      .requireProperty(SONAR_XOO_RANDOM_ACCESS_ISSUE_PATHS);
  }

  @Override
  public void execute(SensorContext context) {
    File f = new File(context.settings().getString(SONAR_XOO_RANDOM_ACCESS_ISSUE_PATHS));
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    try {
      for (String path : FileUtils.readLines(f)) {
        createIssues(fs.inputFile(p.and(p.hasPath(path), p.hasType(Type.MAIN), p.hasLanguage(Xoo.KEY))), context);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void createIssues(InputFile file, SensorContext context) {
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY);
    NewIssue newIssue = context.newIssue();
    newIssue
      .forRule(ruleKey)
      .at(newIssue.newLocation()
        .on(file)
        .at(file.selectLine(1))
        .message("This issue is generated on each file"))
      .save();
  }
}
