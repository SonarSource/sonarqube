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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.MessageFormatting;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssue.FlowType;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.NewMessageFormatting;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

import static org.sonar.api.batch.sensor.issue.NewIssue.FlowType.DATA;
import static org.sonar.api.batch.sensor.issue.NewIssue.FlowType.EXECUTION;
import static org.sonar.api.utils.Preconditions.checkState;

public class MultilineIssuesSensor implements Sensor {

  public static final String RULE_KEY = "MultilineIssue";

  private static final Pattern START_ISSUE_PATTERN = Pattern.compile("\\{xoo-start-issue:([0-9]+)\\}");
  private static final Pattern END_ISSUE_PATTERN = Pattern.compile("\\{xoo-end-issue:([0-9]+)\\}");

  private static final Pattern START_FLOW_PATTERN = Pattern.compile("\\{xoo-start-flow:([0-9]+):([0-9]+):([0-9]+)\\}");
  private static final Pattern END_FLOW_PATTERN = Pattern.compile("\\{xoo-end-flow:([0-9]+):([0-9]+):([0-9]+)\\}");

  private static final Pattern START_DATA_FLOW_PATTERN = Pattern.compile("\\{xoo-start-data-flow:([0-9]+):([0-9]+):([0-9]+)\\}");
  private static final Pattern END_DATA_FLOW_PATTERN = Pattern.compile("\\{xoo-end-data-flow:([0-9]+):([0-9]+):([0-9]+)\\}");

  private static final Pattern START_EXECUTION_FLOW_PATTERN = Pattern.compile("\\{xoo-start-execution-flow:([0-9]+):([0-9]+):([0-9]+)\\}");
  private static final Pattern END_EXECUTION_FLOW_PATTERN = Pattern.compile("\\{xoo-end-execution-flow:([0-9]+):([0-9]+):([0-9]+)\\}");

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Multiline Issues").onlyOnLanguages(Xoo.KEY).createIssuesForRuleRepositories(XooRulesDefinition.XOO_REPOSITORY);
  }

  @Override
  public void execute(SensorContext context) {
    FileSystem fs = context.fileSystem();
    FilePredicates p = fs.predicates();
    for (InputFile file : fs.inputFiles(p.and(p.hasLanguages(Xoo.KEY), p.hasType(Type.MAIN)))) {
      createIssues(file, context);
    }
  }

  public String getRuleKey() {
    return RULE_KEY;
  }

  private void createIssues(InputFile file, SensorContext context) {
    Collection<ParsedIssue> issues = parseIssues(file);
    FlowIndex flowIndex = new FlowIndex();
    parseFlows(flowIndex, file, START_FLOW_PATTERN, END_FLOW_PATTERN, null);
    parseFlows(flowIndex, file, START_DATA_FLOW_PATTERN, END_DATA_FLOW_PATTERN, DATA);
    parseFlows(flowIndex, file, START_EXECUTION_FLOW_PATTERN, END_EXECUTION_FLOW_PATTERN, EXECUTION);
    createIssues(file, context, issues, flowIndex);
  }

  private void createIssues(InputFile file, SensorContext context, Collection<ParsedIssue> parsedIssues, FlowIndex flowIndex) {
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, getRuleKey());

    for (ParsedIssue parsedIssue : parsedIssues) {
      NewIssue newIssue = context.newIssue().forRule(ruleKey);
      NewIssueLocation primaryLocation = newIssue.newLocation();
      String message = "Primary location of the issue in xoo code";
      List<NewMessageFormatting> newMessageFormattings = formatIssueMessage(message, primaryLocation.newMessageFormatting());
      newIssue.at(primaryLocation.on(file)
        .at(file.newRange(parsedIssue.start, parsedIssue.end))
        .message(message, newMessageFormattings));

      for (ParsedFlow flow : flowIndex.getFlows(parsedIssue.issueId)) {
        List<NewIssueLocation> flowLocations = new LinkedList<>();

        for (ParsedFlowLocation flowLocation : flow.getLocations()) {
          String locationMessage = "Xoo code, flow step #" + flowLocation.flowLocationId;
          NewIssueLocation newIssueLocation = newIssue.newLocation();
          List<NewMessageFormatting> locationMessageFormattings = formatIssueMessage(locationMessage, newIssueLocation.newMessageFormatting());
          newIssueLocation
            .on(file)
            .at(file.newRange(flowLocation.start, flowLocation.end))
            .message(locationMessage, locationMessageFormattings);
          flowLocations.add(newIssueLocation);
        }

        if (flow.getType() != null) {
          newIssue.addFlow(flowLocations, flow.getType(), "flow #" + flow.getFlowId());
        } else {
          newIssue.addFlow(flowLocations);
        }
      }
      newIssue.save();
    }
  }

  private static List<NewMessageFormatting> formatIssueMessage(String message, NewMessageFormatting newMessageFormatting) {
    int startIndex = message.toLowerCase().indexOf("xoo");
    if(startIndex == -1) {
      return List.of();
    }
    int endIndex = startIndex + "xoo".length();
    return List.of(newMessageFormatting.start(startIndex).end(endIndex).type(MessageFormatting.Type.CODE));
  }

  private static Collection<ParsedIssue> parseIssues(InputFile file) {
    Map<Integer, ParsedIssue> issuesById = new HashMap<>();

    int currentLine = 0;
    try {
      for (String lineStr : file.contents().split("\\r?\\n")) {
        currentLine++;

        Matcher m = START_ISSUE_PATTERN.matcher(lineStr);
        while (m.find()) {
          Integer issueId = Integer.parseInt(m.group(1));
          issuesById.computeIfAbsent(issueId, ParsedIssue::new).start = file.newPointer(currentLine, m.end());
        }

        m = END_ISSUE_PATTERN.matcher(lineStr);
        while (m.find()) {
          Integer issueId = Integer.parseInt(m.group(1));
          issuesById.computeIfAbsent(issueId, ParsedIssue::new).end = file.newPointer(currentLine, m.start());
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file", e);
    }
    return issuesById.values();
  }

  private void parseFlows(FlowIndex flowIndex, InputFile file, Pattern flowStartPattern, Pattern flowEndPattern, @Nullable FlowType flowType) {
    int currentLine = 0;
    try {
      for (String lineStr : file.contents().split("\\r?\\n")) {
        currentLine++;

        Matcher m = flowStartPattern.matcher(lineStr);
        while (m.find()) {
          ParsedFlowLocation flowLocation = new ParsedFlowLocation(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
          flowLocation.start = file.newPointer(currentLine, m.end());
          flowIndex.addLocation(flowLocation, flowType);
        }

        m = flowEndPattern.matcher(lineStr);
        while (m.find()) {
          ParsedFlowLocation flowLocation = new ParsedFlowLocation(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
          flowLocation.end = file.newPointer(currentLine, m.start());
          flowIndex.addLocation(flowLocation, flowType);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file", e);
    }
  }

  private static class ParsedIssue {
    private final int issueId;

    private TextPointer start;
    private TextPointer end;

    private ParsedIssue(int issueId) {
      this.issueId = issueId;
    }
  }

  private static class FlowIndex {
    private final Map<Integer, IssueFlows> flowsByIssueId = new HashMap<>();

    public void addLocation(ParsedFlowLocation flowLocation, @Nullable FlowType type) {
      flowsByIssueId.computeIfAbsent(flowLocation.issueId, issueId -> new IssueFlows()).addLocation(flowLocation, type);
    }

    public Collection<ParsedFlow> getFlows(int issueId) {
      return Optional.ofNullable(flowsByIssueId.get(issueId)).map(IssueFlows::getFlows).orElse(Collections.emptyList());
    }
  }

  private static class IssueFlows {
    private final Map<Integer, ParsedFlow> flowById = new TreeMap<>();

    private void addLocation(ParsedFlowLocation flowLocation, @Nullable FlowType type) {
      flowById.computeIfAbsent(flowLocation.flowId, flowId -> new ParsedFlow(flowId, type)).addLocation(flowLocation);
    }

    Collection<ParsedFlow> getFlows() {
      return flowById.values();
    }
  }

  private static class ParsedFlow {
    private final int id;
    private final FlowType type;
    private final Map<Integer, ParsedFlowLocation> locationsById = new TreeMap<>();
    private String description;

    private ParsedFlow(int id, @Nullable FlowType type) {
      this.id = id;
      this.type = type;
    }

    private void addLocation(ParsedFlowLocation flowLocation) {
      if (locationsById.containsKey(flowLocation.flowLocationId)) {
        checkState(flowLocation.end != null, "Existing flow should be the end");
        locationsById.get(flowLocation.flowLocationId).end = flowLocation.end;
      } else {
        checkState(flowLocation.start != null, "New flow should be the start");
        locationsById.put(flowLocation.flowLocationId, flowLocation);
      }
    }

    public Collection<ParsedFlowLocation> getLocations() {
      return locationsById.values();

    }

    public void setDescription(@Nullable String description) {
      this.description = description;
    }

    @CheckForNull
    public String getDescription() {
      return description;
    }

    @CheckForNull
    FlowType getType() {
      return type;
    }

    int getFlowId() {
      return id;
    }
  }

  private static class ParsedFlowLocation {
    private final int issueId;
    private final int flowId;
    private final int flowLocationId;

    private TextPointer start;
    private TextPointer end;

    public ParsedFlowLocation(int issueId, int flowId, int flowLocationId) {
      this.issueId = issueId;
      this.flowId = flowId;
      this.flowLocationId = flowLocationId;
    }
  }
}
