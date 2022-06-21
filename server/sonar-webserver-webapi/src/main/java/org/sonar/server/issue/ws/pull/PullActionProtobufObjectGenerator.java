/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;

@ServerSide
public class PullActionProtobufObjectGenerator {

  Issues.IssuesPullQueryTimestamp generateTimestampMessage(long timestamp) {
    Issues.IssuesPullQueryTimestamp.Builder responseBuilder = Issues.IssuesPullQueryTimestamp.newBuilder();
    responseBuilder.setQueryTimestamp(timestamp);
    return responseBuilder.build();
  }

  Issues.IssueLite generateIssueMessage(IssueDto issueDto) {
    Issues.IssueLite.Builder issueBuilder = Issues.IssueLite.newBuilder();
    DbIssues.Locations mainLocation = issueDto.parseLocations();

    Issues.Location.Builder locationBuilder = Issues.Location.newBuilder();
    if (issueDto.getMessage() != null) {
      locationBuilder.setMessage(issueDto.getMessage());
    }
    if (issueDto.getFilePath() != null) {
      locationBuilder.setFilePath(issueDto.getFilePath());
    }
    if (mainLocation != null) {
      Issues.TextRange textRange = buildTextRange(mainLocation);
      locationBuilder.setTextRange(textRange);
    }
    Issues.Location location = locationBuilder.build();

    issueBuilder.setKey(issueDto.getKey());
    issueBuilder.setCreationDate(issueDto.getCreatedAt());
    issueBuilder.setResolved(issueDto.getStatus().equals(org.sonar.api.issue.Issue.STATUS_RESOLVED));
    issueBuilder.setRuleKey(issueDto.getRuleKey().toString());
    if (issueDto.isManualSeverity() && issueDto.getSeverity() != null) {
      issueBuilder.setUserSeverity(issueDto.getSeverity());
    }
    issueBuilder.setType(Common.RuleType.forNumber(issueDto.getType()).name());
    issueBuilder.setClosed(false);
    issueBuilder.setMainLocation(location);

    return issueBuilder.build();
  }

  Issues.IssueLite generateClosedIssueMessage(String uuid) {
    Issues.IssueLite.Builder issueBuilder = Issues.IssueLite.newBuilder();
    issueBuilder.setKey(uuid);
    issueBuilder.setClosed(true);
    return issueBuilder.build();
  }

  private static Issues.TextRange buildTextRange(DbIssues.Locations mainLocation) {
    int startLine = mainLocation.getTextRange().getStartLine();
    int endLine = mainLocation.getTextRange().getEndLine();
    int startOffset = mainLocation.getTextRange().getStartOffset();
    int endOffset = mainLocation.getTextRange().getEndOffset();

    return Issues.TextRange.newBuilder()
      .setHash(mainLocation.getChecksum())
      .setStartLine(startLine)
      .setEndLine(endLine)
      .setStartLineOffset(startOffset)
      .setEndLineOffset(endOffset).build();
  }
}
