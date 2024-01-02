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
package org.sonar.server.hotspot.ws;

import org.sonar.api.server.ServerSide;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.ws.pull.ProtobufObjectGenerator;
import org.sonar.server.security.SecurityStandards;
import org.sonarqube.ws.Hotspots;

import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;

@ServerSide
public class PullHotspotsActionProtobufObjectGenerator implements ProtobufObjectGenerator {

  @Override
  public Hotspots.HotspotPullQueryTimestamp generateTimestampMessage(long timestamp) {
    Hotspots.HotspotPullQueryTimestamp.Builder responseBuilder = Hotspots.HotspotPullQueryTimestamp.newBuilder();
    responseBuilder.setQueryTimestamp(timestamp);
    return responseBuilder.build();
  }

  @Override
  public Hotspots.HotspotLite generateIssueMessage(IssueDto hotspotDto, RuleDto ruleDto) {
    Hotspots.HotspotLite.Builder builder = Hotspots.HotspotLite.newBuilder()
      .setKey(hotspotDto.getKey())
      .setFilePath(hotspotDto.getFilePath())
      .setStatus(hotspotDto.getStatus())
      .setRuleKey(hotspotDto.getRuleKey().toString())
      .setStatus(hotspotDto.getStatus())
      .setVulnerabilityProbability(getVulnerabilityProbability(ruleDto));

    if (hotspotDto.getIssueCreationTime() != null) {
      builder.setCreationDate(hotspotDto.getIssueCreationTime());
    }

    String resolution = hotspotDto.getResolution();
    if (resolution != null) {
      builder.setResolution(resolution);
    }

    String assigneeLogin = hotspotDto.getAssigneeLogin();
    if (assigneeLogin != null) {
      builder.setAssignee(assigneeLogin);
    }

    String message = hotspotDto.getMessage();
    if (message != null) {
      builder.setMessage(message);
    }

    DbIssues.Locations mainLocation = hotspotDto.parseLocations();
    if (mainLocation != null) {
      Hotspots.TextRange textRange = getTextRange(mainLocation);
      builder.setTextRange(textRange);
    }
    return builder.build();
  }

  private static String getVulnerabilityProbability(RuleDto ruleDto) {
    SecurityStandards.SQCategory sqCategory = fromSecurityStandards(ruleDto.getSecurityStandards()).getSqCategory();
    return sqCategory.getVulnerability().name();
  }

  private static Hotspots.TextRange getTextRange(DbIssues.Locations mainLocation) {
    int startLine = mainLocation.getTextRange().getStartLine();
    int endLine = mainLocation.getTextRange().getEndLine();
    int startOffset = mainLocation.getTextRange().getStartOffset();
    int endOffset = mainLocation.getTextRange().getEndOffset();

    return Hotspots.TextRange.newBuilder()
      .setHash(mainLocation.getChecksum())
      .setStartLine(startLine)
      .setEndLine(endLine)
      .setStartLineOffset(startOffset)
      .setEndLineOffset(endOffset)
      .build();
  }

  @Override
  public Hotspots.HotspotLite generateClosedIssueMessage(String uuid) {
    return Hotspots.HotspotLite.newBuilder()
      .setKey(uuid)
      .setClosed(true)
      .build();
  }
}
