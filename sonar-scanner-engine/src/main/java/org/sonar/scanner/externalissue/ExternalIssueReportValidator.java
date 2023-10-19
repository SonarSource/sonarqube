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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.core.documentation.DocumentationLinkGenerator;

@ScannerSide
public class ExternalIssueReportValidator {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalIssueReportValidator.class);
  private static final String ISSUE_RULE_ID = "ruleId";
  private static final String SEVERITY = "severity";
  private static final String TYPE = "type";
  private static final String DOCUMENTATION_SUFFIX = "/analyzing-source-code/importing-external-issues/generic-issue-import-format/";
  private final DocumentationLinkGenerator documentationLinkGenerator;

  ExternalIssueReportValidator(DocumentationLinkGenerator documentationLinkGenerator) {
    this.documentationLinkGenerator = documentationLinkGenerator;
  }

  /**
   * <p>Since we are supporting deprecated format, we decide which format it is in order by:
   *   <ul>
   *     <li>if both 'rules' and 'issues' fields are present, we assume it is CCT format</li>
   *     <li>if only 'issues' field is present, we assume it is deprecated format</li>
   *     <li>otherwise we throw exception as an invalid report was detected</li>
   *   </ul>
   * </p>
   */
  public void validate(ExternalIssueReport report, Path reportPath) {
    if (report.rules != null && report.issues != null) {
      Set<String> ruleIds = validateRules(report.rules, reportPath);
      validateIssuesCctFormat(report.issues, ruleIds, reportPath);
    } else if (report.rules == null && report.issues != null) {
      String documentationLink = documentationLinkGenerator.getDocumentationLink(DOCUMENTATION_SUFFIX);
      LOGGER.warn("External issues were imported with a deprecated format which will be removed soon. " +
        "Please switch to the newest format to fully benefit from Clean Code: {}", documentationLink);
      validateIssuesDeprecatedFormat(report.issues, reportPath);
    } else {
      throw new IllegalStateException(String.format("Failed to parse report '%s': invalid report detected.", reportPath));
    }
  }

  private static void validateIssuesCctFormat(ExternalIssueReport.Issue[] issues, Set<String> ruleIds, Path reportPath) {
    for (ExternalIssueReport.Issue issue : issues) {
      mandatoryField(issue.ruleId, ISSUE_RULE_ID, reportPath);
      checkRuleExistsInReport(ruleIds, issue, reportPath);
      checkNoField(issue.severity, SEVERITY, reportPath);
      checkNoField(issue.type, TYPE, reportPath);
      validateAlwaysRequiredIssueFields(issue, reportPath);
    }
  }

  private static void validateIssuesDeprecatedFormat(ExternalIssueReport.Issue[] issues, Path reportPath) {
    for (ExternalIssueReport.Issue issue : issues) {
      mandatoryField(issue.ruleId, ISSUE_RULE_ID, reportPath);
      mandatoryField(issue.severity, SEVERITY, reportPath);
      mandatoryField(issue.type, TYPE, reportPath);
      mandatoryField(issue.engineId, "engineId", reportPath);
      validateAlwaysRequiredIssueFields(issue, reportPath);
    }
  }

  private static Set<String> validateRules(ExternalIssueReport.Rule[] rules, Path reportPath) {
    Set<String> ruleIds = new HashSet<>();
    for (ExternalIssueReport.Rule rule : rules) {
      mandatoryField(rule.id, "id", reportPath);
      mandatoryField(rule.name, "name", reportPath);
      mandatoryField(rule.engineId, "engineId", reportPath);
      mandatoryField(rule.cleanCodeAttribute, "cleanCodeAttribute", reportPath);
      checkImpactsArray(rule.impacts, reportPath);

      if (!ruleIds.add(rule.id)) {
        throw new IllegalStateException(String.format("Failed to parse report '%s': found duplicate rule ID '%s'.", reportPath, rule.id));
      }
    }

    return ruleIds;
  }

  private static void checkNoField(@Nullable Object value, String fieldName, Path reportPath) {
    if (value != null) {
      throw new IllegalStateException(String.format("Deprecated '%s' field found in the following report: '%s'.", fieldName, reportPath));
    }
  }

  private static void validateAlwaysRequiredIssueFields(ExternalIssueReport.Issue issue, Path reportPath) {
    mandatoryField(issue.primaryLocation, "primaryLocation", reportPath);
    mandatoryFieldPrimaryLocation(issue.primaryLocation.filePath, "filePath", reportPath);
    mandatoryFieldPrimaryLocation(issue.primaryLocation.message, "message", reportPath);

    if (issue.primaryLocation.textRange != null) {
      mandatoryFieldPrimaryLocation(issue.primaryLocation.textRange.startLine, "startLine of the text range", reportPath);
    }

    if (issue.secondaryLocations != null) {
      for (ExternalIssueReport.Location l : issue.secondaryLocations) {
        mandatoryFieldSecondaryLocation(l.filePath, "filePath", reportPath);
        mandatoryFieldSecondaryLocation(l.textRange, "textRange", reportPath);
        mandatoryFieldSecondaryLocation(l.textRange.startLine, "startLine of the text range", reportPath);
      }
    }
  }

  private static void mandatoryFieldPrimaryLocation(@Nullable Object value, String fieldName, Path reportPath) {
    if (value == null) {
      throw new IllegalStateException(String.format("Failed to parse report '%s': missing mandatory field '%s' in the primary location of" +
        " the issue.", reportPath, fieldName));
    }
  }

  private static void mandatoryFieldSecondaryLocation(@Nullable Object value, String fieldName, Path reportPath) {
    if (value == null) {
      throw new IllegalStateException(String.format("Failed to parse report '%s': missing mandatory field '%s' in a secondary location of" +
        " the issue.", reportPath, fieldName));
    }
  }

  private static void mandatoryField(@Nullable Object value, String fieldName, Path reportPath) {
    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
      throw new IllegalStateException(String.format("Failed to parse report '%s': missing mandatory field '%s'.", reportPath, fieldName));
    }
  }

  private static void checkImpactsArray(@Nullable Object[] value, Path reportPath) {
    mandatoryField(value, "impacts", reportPath);
    if (value.length == 0) {
      throw new IllegalStateException(String.format("Failed to parse report '%s': mandatory array '%s' not populated.", reportPath,
        "impacts"));
    }
  }

  private static void checkRuleExistsInReport(Set<String> ruleIds, ExternalIssueReport.Issue issue, Path reportPath) {
    if (!ruleIds.contains(issue.ruleId)) {
      throw new IllegalStateException(String.format("Failed to parse report '%s': rule with '%s' not present.", reportPath, issue.ruleId));
    }
  }
}
