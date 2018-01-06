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
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.Xoo2;

public class OneIssuePerLineSensor implements Sensor {

  public static final String RULE_KEY = "OneIssuePerLine";
  public static final String EFFORT_TO_FIX_PROPERTY = "sonar.oneIssuePerLine.effortToFix";
  public static final String FORCE_SEVERITY_PROPERTY = "sonar.oneIssuePerLine.forceSeverity";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("One Issue Per Line")
      .onlyOnLanguages(Xoo.KEY, Xoo2.KEY)
      .createIssuesForRuleRepositories(XooRulesDefinition.XOO_REPOSITORY, XooRulesDefinition.XOO2_REPOSITORY);
  }

  @Override
  public void execute(SensorContext context) {
    analyse(context, Xoo.KEY, XooRulesDefinition.XOO_REPOSITORY);
    analyse(context, Xoo2.KEY, XooRulesDefinition.XOO2_REPOSITORY);
  }

  private void analyse(SensorContext context, String language, String repo) {
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    for (InputFile file : fs.inputFiles(p.and(p.hasLanguages(language), p.hasType(Type.MAIN)))) {
      createIssues(file, context, repo);
    }
  }

  private void createIssues(InputFile file, SensorContext context, String repo) {
    RuleKey ruleKey = RuleKey.of(repo, RULE_KEY);
    String severity = context.settings().getString(FORCE_SEVERITY_PROPERTY);
    for (int line = 1; line <= file.lines(); line++) {
      NewIssue newIssue = context.newIssue();
      newIssue
        .forRule(ruleKey)
        .at(newIssue.newLocation()
          .on(file)
          .at(file.selectLine(line))
          .message("This issue is generated on each line"))
        .overrideSeverity(severity != null ? Severity.valueOf(severity) : null);
      if (context.getSonarQubeVersion().isGreaterThanOrEqual(Version.create(5, 5))) {
        newIssue.gap(context.settings().getDouble(EFFORT_TO_FIX_PROPERTY));
      } else {
        newIssue.effortToFix(context.settings().getDouble(EFFORT_TO_FIX_PROPERTY));
      }
      newIssue.save();
    }
  }

}
