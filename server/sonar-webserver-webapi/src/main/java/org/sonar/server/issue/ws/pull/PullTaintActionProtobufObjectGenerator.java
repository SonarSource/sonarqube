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
package org.sonar.server.issue.ws.pull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.server.ServerSide;
import org.sonar.core.rule.ImpactFormatter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.MessageFormattingUtils;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;

import static org.sonar.db.protobuf.DbIssues.Locations;
import static org.sonarqube.ws.Issues.TaintVulnerabilityLite;
import static org.sonarqube.ws.Issues.TaintVulnerabilityPullQueryTimestamp;

@ServerSide
public class PullTaintActionProtobufObjectGenerator implements ProtobufObjectGenerator {
  private final DbClient dbClient;
  private final UserSession userSession;
  private Map<String, ComponentDto> componentsMap;

  public PullTaintActionProtobufObjectGenerator(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public TaintVulnerabilityPullQueryTimestamp generateTimestampMessage(long timestamp) {
    refreshComponents();
    TaintVulnerabilityPullQueryTimestamp.Builder responseBuilder = TaintVulnerabilityPullQueryTimestamp.newBuilder();
    responseBuilder.setQueryTimestamp(timestamp);
    return responseBuilder.build();
  }

  @Override
  public TaintVulnerabilityLite generateIssueMessage(IssueDto issueDto, RuleDto ruleDto) {
    TaintVulnerabilityLite.Builder taintBuilder = TaintVulnerabilityLite.newBuilder();
    Locations locations = issueDto.parseLocations();

    if (componentsMap == null) {
      refreshComponents();
    }

    Issues.Location.Builder locationBuilder = Issues.Location.newBuilder();
    if (issueDto.getMessage() != null) {
      locationBuilder.setMessage(issueDto.getMessage());
      locationBuilder.addAllMessageFormattings(MessageFormattingUtils.dbMessageFormattingToWs(issueDto.parseMessageFormattings()));
    }
    if (issueDto.getFilePath() != null) {
      locationBuilder.setFilePath(issueDto.getFilePath());
    }
    if (locations != null) {
      Issues.TextRange textRange = buildTextRange(locations);
      locationBuilder.setTextRange(textRange);
      getFlows(taintBuilder, locations, issueDto);
    }

    taintBuilder.setAssignedToSubscribedUser(issueDto.getAssigneeUuid() != null &&
      issueDto.getAssigneeUuid().equals(userSession.getUuid()));

    taintBuilder.setKey(issueDto.getKey());
    if (issueDto.getIssueCreationTime() != null) {
      taintBuilder.setCreationDate(issueDto.getIssueCreationTime());
    }
    taintBuilder.setResolved(issueDto.getStatus().equals(org.sonar.api.issue.Issue.STATUS_RESOLVED));
    taintBuilder.setRuleKey(issueDto.getRuleKey().toString());
    if (issueDto.getSeverity() != null) {
      taintBuilder.setSeverity(Common.Severity.valueOf(issueDto.getSeverity()));
    }
    taintBuilder.setType(Common.RuleType.forNumber(issueDto.getType()));
    CleanCodeAttribute cleanCodeAttribute = issueDto.getEffectiveCleanCodeAttribute();
    String cleanCodeAttributeString = cleanCodeAttribute != null ? cleanCodeAttribute.name() : null;
    String cleanCodeAttributeCategoryString = cleanCodeAttribute != null ? cleanCodeAttribute.getAttributeCategory().name() : null;
    if (cleanCodeAttributeString != null) {
      taintBuilder.setCleanCodeAttribute(Common.CleanCodeAttribute.valueOf(cleanCodeAttributeString));
      taintBuilder.setCleanCodeAttributeCategory(Common.CleanCodeAttributeCategory.valueOf(cleanCodeAttributeCategoryString));
    }
    taintBuilder.addAllImpacts(issueDto.getEffectiveImpacts().entrySet()
      .stream().map(entry -> Common.Impact.newBuilder()
        .setSoftwareQuality(Common.SoftwareQuality.valueOf(entry.getKey().name()))
        .setSeverity(ImpactFormatter.mapImpactSeverity(entry.getValue()))
        .build())
      .toList());

    taintBuilder.setClosed(false);
    taintBuilder.setMainLocation(locationBuilder.build());
    issueDto.getOptionalRuleDescriptionContextKey().ifPresent(taintBuilder::setRuleDescriptionContextKey);

    return taintBuilder.build();
  }

  @Override
  public TaintVulnerabilityLite generateClosedIssueMessage(String uuid) {
    TaintVulnerabilityLite.Builder taintBuilder = TaintVulnerabilityLite.newBuilder();
    taintBuilder.setKey(uuid);
    taintBuilder.setClosed(true);
    return taintBuilder.build();
  }

  private void getFlows(TaintVulnerabilityLite.Builder taintBuilder, Locations locations, IssueDto issueDto) {
    List<Issues.Flow> flows = new ArrayList<>();

    for (DbIssues.Flow f : locations.getFlowList()) {
      Set<String> componentUuids = new HashSet<>();

      Issues.Flow.Builder builder = Issues.Flow.newBuilder();
      List<Issues.Location> flowLocations = new ArrayList<>();
      getComponentUuids(f, componentUuids);

      for (DbIssues.Location l : f.getLocationList()) {
        Issues.Location.Builder flowLocationBuilder = Issues.Location
          .newBuilder()
          .setMessage(l.getMsg())
          .setTextRange(buildTextRange(l));
        if (l.hasComponentId() && componentsMap.containsKey(l.getComponentId())) {
          flowLocationBuilder.setFilePath(componentsMap.get(l.getComponentId()).path());
        } else {
          flowLocationBuilder.setFilePath(issueDto.getFilePath());
        }
        flowLocations.add(flowLocationBuilder.build());
      }
      builder.addAllLocations(flowLocations);
      flows.add(builder.build());

      taintBuilder.addAllFlows(flows);
    }
  }

  private void getComponentUuids(DbIssues.Flow f, Set<String> componentUuids) {
    for (DbIssues.Location l : f.getLocationList()) {
      if (l.hasComponentId() && !componentsMap.containsKey(l.getComponentId())) {
        componentUuids.add(l.getComponentId());
      }
    }

    if (!componentUuids.isEmpty()) {
      componentsMap.putAll(getLocationComponents(componentUuids));
    }
  }

  private static Issues.TextRange buildTextRange(DbIssues.Location location) {
    int startLine = location.getTextRange().getStartLine();
    int endLine = location.getTextRange().getEndLine();
    int startOffset = location.getTextRange().getStartOffset();
    int endOffset = location.getTextRange().getEndOffset();

    return Issues.TextRange.newBuilder()
      .setHash(location.getChecksum())
      .setStartLine(startLine)
      .setEndLine(endLine)
      .setStartLineOffset(startOffset)
      .setEndLineOffset(endOffset).build();
  }

  private void refreshComponents() {
    componentsMap = new HashMap<>();
  }

  private Map<String, ComponentDto> getLocationComponents(Set<String> components) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.componentDao().selectByUuids(dbSession, components)
        .stream().collect(Collectors.toMap(ComponentDto::uuid, c -> c));
    }
  }
}
