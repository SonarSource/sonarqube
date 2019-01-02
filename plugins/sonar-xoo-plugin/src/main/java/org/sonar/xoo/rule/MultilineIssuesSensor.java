/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
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
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

public class MultilineIssuesSensor implements Sensor {

  public static final String RULE_KEY = "MultilineIssue";
  private static final Pattern START_ISSUE_PATTERN = Pattern.compile("\\{xoo-start-issue:([0-9]+)\\}");
  private static final Pattern END_ISSUE_PATTERN = Pattern.compile("\\{xoo-end-issue:([0-9]+)\\}");

  private static final Pattern START_FLOW_PATTERN = Pattern.compile("\\{xoo-start-flow:([0-9]+):([0-9]+):([0-9]+)\\}");
  private static final Pattern END_FLOW_PATTERN = Pattern.compile("\\{xoo-end-flow:([0-9]+):([0-9]+):([0-9]+)\\}");

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

  private static void createIssues(InputFile file, SensorContext context) {
    Map<Integer, TextPointer> startIssuesPositions = Maps.newHashMap();
    Map<Integer, TextPointer> endIssuesPositions = Maps.newHashMap();
    Map<Integer, Table<Integer, Integer, TextPointer>> startFlowsPositions = Maps.newHashMap();
    Map<Integer, Table<Integer, Integer, TextPointer>> endFlowsPositions = Maps.newHashMap();

    parseIssues(file, context, startIssuesPositions, endIssuesPositions);
    parseFlows(file, startFlowsPositions, endFlowsPositions);
    createIssues(file, context, startIssuesPositions, endIssuesPositions, startFlowsPositions, endFlowsPositions);
  }

  private static void parseFlows(InputFile file, Map<Integer, Table<Integer, Integer, TextPointer>> startFlowsPositions,
    Map<Integer, Table<Integer, Integer, TextPointer>> endFlowsPositions) {
    int currentLine = 0;
    try {
      for (String lineStr : Files.readAllLines(file.path(), file.charset())) {
        currentLine++;

        Matcher m = START_FLOW_PATTERN.matcher(lineStr);
        while (m.find()) {
          Integer issueId = Integer.parseInt(m.group(1));
          Integer issueFlowId = Integer.parseInt(m.group(2));
          Integer issueFlowNum = Integer.parseInt(m.group(3));
          TextPointer newPointer = file.newPointer(currentLine, m.end());
          if (!startFlowsPositions.containsKey(issueId)) {
            startFlowsPositions.put(issueId, HashBasedTable.create());
          }
          startFlowsPositions.get(issueId).row(issueFlowId).put(issueFlowNum, newPointer);
        }

        m = END_FLOW_PATTERN.matcher(lineStr);
        while (m.find()) {
          Integer issueId = Integer.parseInt(m.group(1));
          Integer issueFlowId = Integer.parseInt(m.group(2));
          Integer issueFlowNum = Integer.parseInt(m.group(3));
          TextPointer newPointer = file.newPointer(currentLine, m.start());
          if (!endFlowsPositions.containsKey(issueId)) {
            endFlowsPositions.put(issueId, HashBasedTable.create());
          }
          endFlowsPositions.get(issueId).row(issueFlowId).put(issueFlowNum, newPointer);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file", e);
    }
  }

  private static void createIssues(InputFile file, SensorContext context, Map<Integer, TextPointer> startPositions,
    Map<Integer, TextPointer> endPositions, Map<Integer, Table<Integer, Integer, TextPointer>> startFlowsPositions,
    Map<Integer, Table<Integer, Integer, TextPointer>> endFlowsPositions) {
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY);

    for (Map.Entry<Integer, TextPointer> entry : startPositions.entrySet()) {
      NewIssue newIssue = context.newIssue().forRule(ruleKey);
      Integer issueId = entry.getKey();
      NewIssueLocation primaryLocation = newIssue.newLocation()
        .on(file)
        .at(file.newRange(entry.getValue(), endPositions.get(issueId)));
      newIssue.at(primaryLocation.message("Primary location"));
      if (startFlowsPositions.containsKey(issueId)) {
        Table<Integer, Integer, TextPointer> flows = startFlowsPositions.get(issueId);
        for (Map.Entry<Integer, Map<Integer, TextPointer>> flowEntry : flows.rowMap().entrySet()) {
          Integer flowId = flowEntry.getKey();
          List<NewIssueLocation> flowLocations = Lists.newArrayList();
          List<Integer> flowNums = Lists.newArrayList(flowEntry.getValue().keySet());
          Collections.sort(flowNums);
          for (Integer flowNum : flowNums) {
            TextPointer start = flowEntry.getValue().get(flowNum);
            TextPointer end = endFlowsPositions.get(issueId).row(flowId).get(flowNum);
            NewIssueLocation newLocation = newIssue.newLocation()
              .on(file)
              .at(file.newRange(start, end))
              .message("Flow step #" + flowNum);
            flowLocations.add(newLocation);
          }
          if (flowLocations.size() == 1) {
            newIssue.addLocation(flowLocations.get(0));
          } else {
            newIssue.addFlow(flowLocations);
          }
        }
      }
      newIssue.save();
    }
  }

  private static void parseIssues(InputFile file, SensorContext context, Map<Integer, TextPointer> startPositions,
    Map<Integer, TextPointer> endPositions) {
    int currentLine = 0;
    try {
      for (String lineStr : Files.readAllLines(file.path(), file.charset())) {
        currentLine++;

        Matcher m = START_ISSUE_PATTERN.matcher(lineStr);
        while (m.find()) {
          Integer issueId = Integer.parseInt(m.group(1));
          TextPointer newPointer = file.newPointer(currentLine, m.end());
          startPositions.put(issueId, newPointer);
        }

        m = END_ISSUE_PATTERN.matcher(lineStr);
        while (m.find()) {
          Integer issueId = Integer.parseInt(m.group(1));
          TextPointer newPointer = file.newPointer(currentLine, m.start());
          endPositions.put(issueId, newPointer);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file", e);
    }
  }

}
