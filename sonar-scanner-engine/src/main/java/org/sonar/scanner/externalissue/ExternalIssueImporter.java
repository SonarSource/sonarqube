/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.externalissue;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.internal.ImpactMapper;
import org.sonar.scanner.externalissue.ExternalIssueReport.Issue;
import org.sonar.scanner.externalissue.ExternalIssueReport.Location;
import org.sonar.scanner.externalissue.ExternalIssueReport.Rule;

public class ExternalIssueImporter {
  private static final Logger LOG = LoggerFactory.getLogger(ExternalIssueImporter.class);
  private static final int MAX_UNKNOWN_FILE_PATHS_TO_PRINT = 5;

  private final SensorContext context;
  private final ExternalIssueReport report;
  private final Set<String> unknownFiles = new LinkedHashSet<>();
  private final Set<String> knownFiles = new LinkedHashSet<>();

  public ExternalIssueImporter(SensorContext context, ExternalIssueReport report) {
    this.context = context;
    this.report = report;
  }

  public void execute() {
    if (report.rules != null) {
      importNewFormat();
    } else {
      importDeprecatedFormat();
    }
  }

  private void importDeprecatedFormat() {
    int issueCount = 0;
    for (Issue issue : report.issues) {
      if (importDeprecatedIssue(issue)) {
        issueCount++;
      }
    }
    logStatistics(issueCount, StringUtils.EMPTY);
  }

  private void importNewFormat() {
    Map<String, Rule> rulesMap = new HashMap<>();

    for (Rule rule : report.rules) {
      rulesMap.put(rule.id, rule);
      NewAdHocRule adHocRule = createAdHocRule(rule);
      adHocRule.save();
    }

    int issueCount = 0;
    for (Issue issue : report.issues) {
      if (importIssue(issue, rulesMap.get(issue.ruleId))) {
        issueCount++;
      }
    }
    logStatistics(issueCount, StringUtils.EMPTY);
  }

  private NewAdHocRule createAdHocRule(Rule rule) {
    NewAdHocRule adHocRule = context.newAdHocRule();
    adHocRule.ruleId(rule.id);
    adHocRule.name(rule.name);
    adHocRule.description(rule.description);
    adHocRule.engineId(rule.engineId);
    adHocRule.cleanCodeAttribute(CleanCodeAttribute.valueOf(rule.cleanCodeAttribute));
    adHocRule.severity(backmapSeverityFromImpact(rule));
    adHocRule.type(backmapTypeFromImpact(rule));
    for (ExternalIssueReport.Impact impact : rule.impacts) {
      adHocRule.addDefaultImpact(SoftwareQuality.valueOf(impact.softwareQuality),
        org.sonar.api.issue.impact.Severity.valueOf(impact.severity));
    }
    return adHocRule;
  }

  private static RuleType backmapTypeFromImpact(Rule rule) {
    return ImpactMapper.convertToRuleType(SoftwareQuality.valueOf(rule.impacts[0].softwareQuality));
  }

  private static Severity backmapSeverityFromImpact(Rule rule) {
    org.sonar.api.issue.impact.Severity impactSeverity = org.sonar.api.issue.impact.Severity.valueOf(rule.impacts[0].severity);
    return Severity.valueOf(ImpactMapper.convertToDeprecatedSeverity(impactSeverity));
  }

  private boolean populateCommonValues(Issue issue, NewExternalIssue externalIssue) {
    if (issue.effortMinutes != null) {
      externalIssue.remediationEffortMinutes(Long.valueOf(issue.effortMinutes));
    }

    NewIssueLocation primary = fillLocation(context, externalIssue.newLocation(), issue.primaryLocation);
    if (primary != null) {
      knownFiles.add(issue.primaryLocation.filePath);
      externalIssue.at(primary);
      if (issue.secondaryLocations != null) {
        for (Location l : issue.secondaryLocations) {
          NewIssueLocation secondary = fillLocation(context, externalIssue.newLocation(), l);
          if (secondary != null) {
            externalIssue.addLocation(secondary);
          }
        }
      }
      externalIssue.save();
      return true;
    } else {
      unknownFiles.add(issue.primaryLocation.filePath);
      return false;
    }
  }

  private boolean importDeprecatedIssue(Issue issue) {
    NewExternalIssue externalIssue = context.newExternalIssue()
      .engineId(issue.engineId)
      .ruleId(issue.ruleId)
      .severity(Severity.valueOf(issue.severity))
      .type(RuleType.valueOf(issue.type));

    return populateCommonValues(issue, externalIssue);
  }

  private boolean importIssue(Issue issue, ExternalIssueReport.Rule rule) {
    NewExternalIssue externalIssue = context.newExternalIssue()
      .engineId(rule.engineId)
      .ruleId(rule.id)
      .severity(backmapSeverityFromImpact(rule))
      .type(backmapTypeFromImpact(rule));

    return populateCommonValues(issue, externalIssue);
  }

  private void logStatistics(int issueCount, String additionalInformation) {
    String pluralizedIssues = pluralize("issue", issueCount);
    String pluralizedFiles = pluralize("file", knownFiles.size());
    LOG.info("Imported {} {} in {} {}{}", issueCount, pluralizedIssues, knownFiles.size(), pluralizedFiles, additionalInformation);
    int numberOfUnknownFiles = unknownFiles.size();
    if (numberOfUnknownFiles > 0) {
      String limitedUnknownFiles = this.unknownFiles.stream().limit(MAX_UNKNOWN_FILE_PATHS_TO_PRINT).collect(Collectors.joining(", "));
      LOG.info("External issues{} ignored for {} unknown files, including: {}", additionalInformation, numberOfUnknownFiles, limitedUnknownFiles);
    }
  }

  private static String pluralize(String msg, int count) {
    if (count == 1) {
      return msg;
    }
    return msg + "s";
  }

  @CheckForNull
  private static NewIssueLocation fillLocation(SensorContext context, NewIssueLocation newLocation, Location location) {
    InputFile file = findFile(context, location.filePath);
    if (file == null) {
      return null;
    }
    newLocation.on(file);

    if (location.message != null) {
      newLocation.message(location.message);
    }

    if (location.textRange != null) {
      if (location.textRange.startColumn != null) {
        TextPointer start = file.newPointer(location.textRange.startLine, location.textRange.startColumn);
        int endLine = (location.textRange.endLine != null) ? location.textRange.endLine : location.textRange.startLine;
        int endColumn;

        // assume it's until the last character of the end line
        endColumn = Objects.requireNonNullElseGet(location.textRange.endColumn, () -> file.selectLine(endLine).end().lineOffset());
        TextPointer end = file.newPointer(endLine, endColumn);
        newLocation.at(file.newRange(start, end));
      } else {
        newLocation.at(file.selectLine(location.textRange.startLine));
      }
    }
    return newLocation;
  }

  @CheckForNull
  private static InputFile findFile(SensorContext context, String filePath) {
    return context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(filePath));
  }

}
