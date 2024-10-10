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
package org.sonar.xoo.rule.telemetry;

import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.xoo.Xoo;

public class OneIssuePerUninitializedVariableForTelemetrySensor implements Sensor {

  protected static final String RULE_KEY = "OneIssuePerUninitializedVariableForTelemetry";
  protected static final String ENGINE_ID = "XooEngine";
  protected static final String NAME = "One External Issue Per Uninitialized Variable Rule For Telemetry";
  protected static final String ACTIVATE = "sonar.uninitializedVariableForTelemetrySensor.activate";
  protected static final Long EFFORT_MINUTES = 5L;

  private final SensorMetrics sensorMetrics;

  public OneIssuePerUninitializedVariableForTelemetrySensor(SensorMetrics sensorMetrics) {
    this.sensorMetrics = sensorMetrics;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name(NAME)
      .onlyOnLanguages(Xoo.KEY)
      .onlyWhenConfiguration(c -> c.getBoolean(ACTIVATE).orElse(false));
  }

  @Override
  public void execute(SensorContext context) {
    analyse(context);

    sensorMetrics.finalizeAndReportTelemetry();
  }

  private void analyse(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    for (InputFile file : fs.inputFiles(p.and(p.hasLanguages(Xoo.KEY), p.hasType(InputFile.Type.MAIN)))) {
      createIssues(file, context);
    }
  }

  private void createIssues(InputFile file, SensorContext context) {
    try {
      String code = IOUtils.toString(file.inputStream(), file.charset());

      List<String> lines = IOUtils.readLines(file.inputStream(), file.charset());
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i).trim();
        if (isVariableDeclaration(line) && !isVariableInitialized(line, code)) {
          NewExternalIssue newIssue = context.newExternalIssue();
          newIssue
            .engineId(ENGINE_ID)
            .ruleId(RULE_KEY)
            .at(newIssue.newLocation()
              .on(file)
              .at(file.selectLine(i + 1))
              .message("This issue is generated on line containing uninitialized variable"))
            .addImpact(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.LOW)
            .remediationEffortMinutes(EFFORT_MINUTES)
            .save();

          // Increment the number of issues found and add the remediation effort in minutes to telemetry
          sensorMetrics.incrementUninitializedVariableRuleIssueCounter();
          sensorMetrics.addUninitializedVariableRuleEffortInMinutes(EFFORT_MINUTES);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to process " + file, e);
    }
  }

  private boolean isVariableDeclaration(String line) {
    return line.matches("[a-zA-Z][a-zA-Z0-9_]*\\s*;");
  }

  private boolean isVariableInitialized(String declarationLine, String code) {
    String variableName = getVariableNameFromDeclaration(declarationLine);
    return code.contains(variableName + " =");
  }

  private String getVariableNameFromDeclaration(String line) {
    return line.replace(";", "").trim();
  }

}
