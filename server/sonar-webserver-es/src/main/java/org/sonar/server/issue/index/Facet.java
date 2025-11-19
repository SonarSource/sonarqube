/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue.index;

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttributeCategory;
import org.sonar.core.rule.RuleType;
import org.sonar.server.es.searchrequest.SimpleFieldTopAggregationDefinition;
import org.sonar.server.es.searchrequest.TopAggregationDefinition;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.NON_STICKY;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.STICKY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_FROM_SONAR_QUBE_UPDATE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CASA;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CODE_VARIANTS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CWE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FILE_PATH;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_IMPACTS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_LANGUAGE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_NEW_STATUS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_ASVS_40;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_MOBILE_TOP_10_2024;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_TOP_10;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_TOP_10_2021;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PCI_DSS_32;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PCI_DSS_40;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_RESOLUTION;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_RULE_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SANS_TOP_25;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SCOPE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SEVERITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SQ_SECURITY_CATEGORY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_STATUS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_STIG_ASD_V5R3;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_TAGS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_TYPE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_LINKED_TICKET_STATUS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_PRIORITIZED_RULE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_AUTHOR;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CASA;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CODE_VARIANTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CWE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FROM_SONAR_QUBE_UPDATE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SOFTWARE_QUALITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LINKED_TICKET_STATUS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_ASVS_40;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_MOBILE_TOP_10_2024;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10_2021;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PCI_DSS_32;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PCI_DSS_40;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PRIORITIZED_RULE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SANS_TOP_25;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SCOPES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SONARSOURCE_SECURITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STIG_ASD_V5R3;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPES;

public enum Facet {
  SEVERITIES(PARAM_SEVERITIES, FIELD_ISSUE_SEVERITY, STICKY, Severity.ALL.size()),
  IMPACT_SOFTWARE_QUALITY(PARAM_IMPACT_SOFTWARE_QUALITIES, FIELD_ISSUE_IMPACTS, STICKY),
  IMPACT_SEVERITY(PARAM_IMPACT_SEVERITIES, FIELD_ISSUE_IMPACTS, STICKY),
  CLEAN_CODE_ATTRIBUTE_CATEGORY(PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES, FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY, STICKY,
    CleanCodeAttributeCategory.values().length),
  STATUSES(PARAM_STATUSES, FIELD_ISSUE_STATUS, STICKY, Issue.STATUSES.size()),
  // Resolutions facet returns one more element than the number of resolutions to take into account unresolved issues
  RESOLUTIONS(PARAM_RESOLUTIONS, FIELD_ISSUE_RESOLUTION, STICKY, Issue.RESOLUTIONS.size() + 1),
  ISSUE_STATUSES(PARAM_ISSUE_STATUSES, FIELD_ISSUE_NEW_STATUS, STICKY, IssueStatus.values().length),
  TYPES(PARAM_TYPES, FIELD_ISSUE_TYPE, STICKY, RuleType.values().length),
  SCOPES(PARAM_SCOPES, FIELD_ISSUE_SCOPE, STICKY, Facet.MAX_FACET_SIZE),
  LANGUAGES(PARAM_LANGUAGES, FIELD_ISSUE_LANGUAGE, STICKY, Facet.MAX_FACET_SIZE),
  RULES(PARAM_RULES, FIELD_ISSUE_RULE_UUID, STICKY, Facet.MAX_FACET_SIZE),
  TAGS(PARAM_TAGS, FIELD_ISSUE_TAGS, STICKY, Facet.MAX_FACET_SIZE),
  AUTHOR(PARAM_AUTHOR, FIELD_ISSUE_AUTHOR_LOGIN, STICKY, Facet.MAX_FACET_SIZE),
  PROJECT_UUIDS(Facet.FACET_PROJECTS, FIELD_ISSUE_PROJECT_UUID, STICKY, Facet.MAX_FACET_SIZE),
  FILES(PARAM_FILES, FIELD_ISSUE_FILE_PATH, STICKY, Facet.MAX_FACET_SIZE),
  DIRECTORIES(PARAM_DIRECTORIES, FIELD_ISSUE_DIRECTORY_PATH, STICKY, Facet.MAX_FACET_SIZE),
  ASSIGNEES(PARAM_ASSIGNEES, FIELD_ISSUE_ASSIGNEE_UUID, STICKY, Facet.MAX_FACET_SIZE),
  ASSIGNED_TO_ME(Facet.FACET_ASSIGNED_TO_ME, FIELD_ISSUE_ASSIGNEE_UUID, STICKY, 1),
  PCI_DSS_32(PARAM_PCI_DSS_32, FIELD_ISSUE_PCI_DSS_32, STICKY, Facet.DEFAULT_FACET_SIZE),
  PCI_DSS_40(PARAM_PCI_DSS_40, FIELD_ISSUE_PCI_DSS_40, STICKY, Facet.DEFAULT_FACET_SIZE),
  OWASP_ASVS_40(PARAM_OWASP_ASVS_40, FIELD_ISSUE_OWASP_ASVS_40, STICKY, Facet.DEFAULT_FACET_SIZE),
  OWASP_MOBILE_TOP_10_2024(PARAM_OWASP_MOBILE_TOP_10_2024, FIELD_ISSUE_OWASP_MOBILE_TOP_10_2024, STICKY, Facet.DEFAULT_FACET_SIZE),
  OWASP_TOP_10(PARAM_OWASP_TOP_10, FIELD_ISSUE_OWASP_TOP_10, STICKY, Facet.DEFAULT_FACET_SIZE),
  OWASP_TOP_10_2021(PARAM_OWASP_TOP_10_2021, FIELD_ISSUE_OWASP_TOP_10_2021, STICKY, Facet.DEFAULT_FACET_SIZE),
  STIG_ASD_V5R3(PARAM_STIG_ASD_V5R3, FIELD_ISSUE_STIG_ASD_V5R3, STICKY, Facet.DEFAULT_FACET_SIZE),
  CASA(PARAM_CASA, FIELD_ISSUE_CASA, STICKY, Facet.DEFAULT_FACET_SIZE),
  SANS_TOP_25(PARAM_SANS_TOP_25, FIELD_ISSUE_SANS_TOP_25, STICKY, Facet.DEFAULT_FACET_SIZE),
  CWE(PARAM_CWE, FIELD_ISSUE_CWE, STICKY, Facet.DEFAULT_FACET_SIZE),
  CREATED_AT(PARAM_CREATED_AT, FIELD_ISSUE_FUNC_CREATED_AT, NON_STICKY),
  SONARSOURCE_SECURITY(PARAM_SONARSOURCE_SECURITY, FIELD_ISSUE_SQ_SECURITY_CATEGORY, STICKY, Facet.DEFAULT_FACET_SIZE),
  CODE_VARIANTS(PARAM_CODE_VARIANTS, FIELD_ISSUE_CODE_VARIANTS, STICKY, Facet.MAX_FACET_SIZE),
  PRIORITIZED_RULE(PARAM_PRIORITIZED_RULE, FIELD_PRIORITIZED_RULE, STICKY, 2),
  FROM_SONAR_QUBE_UPDATE(PARAM_FROM_SONAR_QUBE_UPDATE, FIELD_FROM_SONAR_QUBE_UPDATE, STICKY, 2),
  LINKED_JIRA_WORK_ITEM(PARAM_LINKED_TICKET_STATUS, FIELD_LINKED_TICKET_STATUS, STICKY, 2);

  public static final String FACET_PROJECTS = "projects";
  public static final String FACET_ASSIGNED_TO_ME = "assigned_to_me";
  public static final int DEFAULT_FACET_SIZE = 15;
  public static final int MAX_FACET_SIZE = 100;

  private final String name;
  private final TopAggregationDefinition<TopAggregationDefinition.FilterScope> topAggregation;
  private final Integer numberOfTerms;

  Facet(String name, String fieldName, boolean sticky, int numberOfTerms) {
    this.name = name;
    this.topAggregation = new SimpleFieldTopAggregationDefinition(fieldName, sticky);
    this.numberOfTerms = numberOfTerms;
  }

  Facet(String name, String fieldName, boolean sticky) {
    this.name = name;
    this.topAggregation = new SimpleFieldTopAggregationDefinition(fieldName, sticky);
    this.numberOfTerms = null;
  }

  public String getName() {
    return name;
  }

  public String getFieldName() {
    return topAggregation.getFilterScope().getFieldName();
  }

  public TopAggregationDefinition.FilterScope getFilterScope() {
    return topAggregation.getFilterScope();
  }

  public TopAggregationDefinition<TopAggregationDefinition.FilterScope> getTopAggregationDef() {
    return topAggregation;
  }

  public int getNumberOfTerms() {
    checkState(numberOfTerms != null, "numberOfTerms should have been provided in constructor");

    return numberOfTerms;
  }
}