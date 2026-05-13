/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.IssueResolution;
import org.sonar.api.batch.sensor.issue.NewIssueResolution;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

@Phase(name = Phase.Name.PRE)
public class IssueResolutionSensor implements Sensor {

  private static final String ISSUE_RESOLUTION_SENSOR_ACTIVATE = "sonar.issueresolutionsensor.activate";

  // Matches: //sonar-resolve [optional status] ruleKeys comment
  private static final Pattern SONAR_RESOLVE_PATTERN = Pattern.compile(
    "//\\s*sonar-resolve\\s+(?:\\[(\\w+)]\\s+)?([\\w:,]+)\\s+(.+)");

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(Xoo.KEY)
      .onlyWhenConfiguration(c -> c.getBoolean(ISSUE_RESOLUTION_SENSOR_ACTIVATE).orElse(false));
  }

  @Override
  public void execute(SensorContext context) {
    for (InputFile inputFile : context.fileSystem().inputFiles(context.fileSystem().predicates().hasLanguage(Xoo.KEY))) {
      processFile(context, inputFile);
    }
  }

  private static void processFile(SensorContext context, InputFile inputFile) {
    try {
      List<String> lines = inputFile.contents().lines().toList();
      for (int i = 0; i < lines.size(); i++) {
        Matcher matcher = SONAR_RESOLVE_PATTERN.matcher(lines.get(i));
        if (matcher.find()) {
          IssueResolution.Status status = parseStatus(matcher.group(1));
          Set<RuleKey> ruleKeys = parseRuleKeys(matcher.group(2));
          String comment = matcher.group(3).trim();
          int line = i + 1;

          NewIssueResolution resolution = context.newIssueResolution()
            .on(inputFile)
            .at(inputFile.selectLine(line))
            .status(status != null ? status : IssueResolution.Status.DEFAULT)
            .forRules(ruleKeys)
            .comment(comment);
          resolution.save();
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to process " + inputFile, e);
    }
  }

  private static IssueResolution.Status parseStatus(String statusGroup) {
    if (statusGroup == null) {
      return null;
    }
    return switch (statusGroup.toUpperCase()) {
      case "FP" -> IssueResolution.Status.FALSE_POSITIVE;
      case "ACCEPT" -> IssueResolution.Status.DEFAULT;
      default -> null;
    };
  }

  private static Set<RuleKey> parseRuleKeys(String ruleKeysStr) {
    return Arrays.stream(ruleKeysStr.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .map(RuleKey::parse)
      .collect(Collectors.toSet());
  }
}
