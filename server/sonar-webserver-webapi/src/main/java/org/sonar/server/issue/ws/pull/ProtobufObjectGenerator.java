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
package org.sonar.server.issue.ws.pull;

import com.google.protobuf.AbstractMessageLite;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonarqube.ws.Issues;

public interface ProtobufObjectGenerator {
  AbstractMessageLite generateTimestampMessage(long timestamp);

  AbstractMessageLite generateIssueMessage(IssueDto issueDto, RuleDto ruleDto);

  AbstractMessageLite generateClosedIssueMessage(String uuid);

  default Issues.TextRange buildTextRange(DbIssues.Locations mainLocation) {
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
