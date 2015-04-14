/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

public class OneIssuePerLineSensor implements Sensor {

  public static final String RULE_KEY = "OneIssuePerLine";
  private static final String EFFORT_TO_FIX_PROPERTY = "sonar.oneIssuePerLine.effortToFix";
  public static final String FORCE_SEVERITY_PROPERTY = "sonar.oneIssuePerLine.forceSeverity";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("One Issue Per Line")
      .onlyOnLanguages(Xoo.KEY)
      .createIssuesForRuleRepositories(XooRulesDefinition.XOO_REPOSITORY);
  }

  @Override
  public void execute(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    for (InputFile file : fs.inputFiles(p.and(p.hasLanguages(Xoo.KEY), p.hasType(Type.MAIN)))) {
      createIssues(file, context);
    }
  }

  private void createIssues(InputFile file, SensorContext context) {
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY);
    String severity = context.settings().getString(FORCE_SEVERITY_PROPERTY);
    for (int line = 1; line <= file.lines(); line++) {
      context.newIssue()
        .forRule(ruleKey)
        .onFile(file)
        .atLine(line)
        .effortToFix(context.settings().getDouble(EFFORT_TO_FIX_PROPERTY))
        .overrideSeverity(severity != null ? Severity.valueOf(severity) : null)
        .message("This issue is generated on each line")
        .save();
    }
  }
}
