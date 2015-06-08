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

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

public class MultilineIssuesSensor implements Sensor {

  public static final String RULE_KEY = "MultilineIssue";
  private static final String START_ISSUE_PATTERN = "\\{xoo-start-issue:([0-9]+):([0-9]+)\\}";
  private static final String END_ISSUE_PATTERN = "\\{xoo-end-issue:([0-9]+):([0-9]+)\\}";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Multiline Issues")
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
    Pattern startPattern = Pattern.compile(START_ISSUE_PATTERN);
    Pattern endPattern = Pattern.compile(END_ISSUE_PATTERN);
    Map<Integer, Map<Integer, TextPointer>> startPositions = new HashMap<>();
    Map<Integer, Map<Integer, TextPointer>> endPositions = new HashMap<>();

    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY);
    int currentLine = 0;
    try {
      for (String lineStr : Files.readAllLines(file.path(), context.fileSystem().encoding())) {
        currentLine++;

        Matcher m = startPattern.matcher(lineStr);
        while (m.find()) {
          Integer issueId = Integer.parseInt(m.group(1));
          Integer issueLocationId = Integer.parseInt(m.group(2));
          TextPointer newPointer = file.newPointer(currentLine, m.start());
          if (!startPositions.containsKey(issueId)) {
            startPositions.put(issueId, new HashMap<Integer, TextPointer>());
          }
          startPositions.get(issueId).put(issueLocationId, newPointer);
        }

        m = endPattern.matcher(lineStr);
        while (m.find()) {
          Integer issueId = Integer.parseInt(m.group(1));
          Integer issueLocationId = Integer.parseInt(m.group(2));
          TextPointer newPointer = file.newPointer(currentLine, m.start());
          if (!endPositions.containsKey(issueId)) {
            endPositions.put(issueId, new HashMap<Integer, TextPointer>());
          }
          endPositions.get(issueId).put(issueLocationId, newPointer);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file", e);
    }
    for (Map.Entry<Integer, Map<Integer, TextPointer>> entry : startPositions.entrySet()) {
      NewIssue newIssue = context.newIssue().forRule(ruleKey);
      for (Map.Entry<Integer, TextPointer> location : entry.getValue().entrySet()) {
        newIssue.addLocation(newIssue.newLocation()
          .onFile(file)
          .at(file.newRange(location.getValue(), endPositions.get(entry.getKey()).get(location.getKey())))
          .message("Multiline issue"));
      }
      newIssue.save();
    }
  }

}
