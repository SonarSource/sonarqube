/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonar.xoo.Xoo;

public class OneExternalIssuePerFoobarSensor implements Sensor {

  public static final String RULE_ID = "OneExternalIssuePerFoobar";
  public static final String TAG = "foobar";
  public static final String ENGINE_ID = "XooEngine";
  public static final String SEVERITY = "MAJOR";
  public static final Long EFFORT = 10L;
  public static final RuleType TYPE = RuleType.BUG;
  public static final String ACTIVATE = "sonar.oneExternalIssuePerFoobar.activate";
  private static final String NAME = "One External Issue Per Foobar Rule";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name(NAME)
      .onlyOnLanguages(Xoo.KEY)
      .onlyWhenConfiguration(c -> c.getBoolean(ACTIVATE).orElse(false));
  }

  @Override
  public void execute(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    for (InputFile file : fs.inputFiles(p.and(p.hasLanguages(Xoo.KEY), p.hasType(InputFile.Type.MAIN)))) {
      createIssues(file, context);
    }
  }

  private static void createIssues(InputFile file, SensorContext context) {
    try {
      List<String> lines = IOUtils.readLines(file.inputStream(), file.charset());
      for (int i = 0; i < lines.size(); i++) {
        var line = lines.get(i);
        if (line.contains(TAG) && !line.contains("//NOSONAR")) {
          NewExternalIssue newIssue = context.newExternalIssue();
          newIssue
            .engineId(ENGINE_ID)
            .ruleId(RULE_ID)
            .at(newIssue.newLocation()
              .on(file)
              .at(file.selectLine(i + 1))
              .message("This issue is generated on line containing foobar string"))
            .severity(Severity.valueOf(SEVERITY))
            .remediationEffortMinutes(EFFORT)
            .type(TYPE)
            .save();
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to process " + file, e);
    }
  }
}
