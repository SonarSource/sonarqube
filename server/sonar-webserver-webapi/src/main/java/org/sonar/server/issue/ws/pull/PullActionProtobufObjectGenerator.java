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
package org.sonar.server.issue.ws.pull;

import org.sonar.api.server.ServerSide;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonarqube.ws.Common;

import static org.sonarqube.ws.Issues.IssueLite;
import static org.sonarqube.ws.Issues.IssuesPullQueryTimestamp;
import static org.sonarqube.ws.Issues.Location;
import static org.sonarqube.ws.Issues.TextRange;

@ServerSide
public class PullActionProtobufObjectGenerator implements ProtobufObjectGenerator {

  @Override
  public IssuesPullQueryTimestamp generateTimestampMessage(long timestamp) {
    IssuesPullQueryTimestamp.Builder responseBuilder = IssuesPullQueryTimestamp.newBuilder();
    responseBuilder.setQueryTimestamp(timestamp);
    return responseBuilder.build();
  }

  @Override
  public IssueLite generateIssueMessage(IssueDto issueDto, RuleDto ruleDto) {
    IssueLite.Builder issueBuilder = IssueLite.newBuilder();
    DbIssues.Locations mainLocation = issueDto.parseLocations();

    Location.Builder locationBuilder = Location.newBuilder();
    if (issueDto.getMessage() != null) {
      locationBuilder.setMessage(issueDto.getMessage());
    }
    if (issueDto.getFilePath() != null) {
      locationBuilder.setFilePath(issueDto.getFilePath());
    }
    if (mainLocation != null) {
      TextRange textRange = buildTextRange(mainLocation);
      locationBuilder.setTextRange(textRange);
    }
    Location location = locationBuilder.build();

    issueBuilder.setKey(issueDto.getKey());
    if (issueDto.getIssueCreationTime() != null) {
      issueBuilder.setCreationDate(issueDto.getIssueCreationTime());
    }
    issueBuilder.setResolved(issueDto.getStatus().equals(org.sonar.api.issue.Issue.STATUS_RESOLVED));
    issueBuilder.setRuleKey(issueDto.getRuleKey().toString());
    if (issueDto.isManualSeverity() && issueDto.getSeverity() != null) {
      issueBuilder.setUserSeverity(Common.Severity.valueOf(issueDto.getSeverity()));
    }
    issueBuilder.setType(Common.RuleType.forNumber(issueDto.getType()));
    issueBuilder.setClosed(false);
    issueBuilder.setMainLocation(location);

    return issueBuilder.build();
  }

  @Override
  public IssueLite generateClosedIssueMessage(String uuid) {
    IssueLite.Builder issueBuilder = IssueLite.newBuilder();
    issueBuilder.setKey(uuid);
    issueBuilder.setClosed(true);
    return issueBuilder.build();
  }
}
