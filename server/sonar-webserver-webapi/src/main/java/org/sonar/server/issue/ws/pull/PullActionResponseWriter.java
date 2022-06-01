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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.db.issue.IssueDto;
import org.sonarqube.ws.Issues;

@ServerSide
public class PullActionResponseWriter {

  private final System2 system2;
  private final PullActionProtobufObjectGenerator pullActionProtobufObjectGenerator;

  public PullActionResponseWriter(System2 system2, PullActionProtobufObjectGenerator pullActionProtobufObjectGenerator) {
    this.system2 = system2;
    this.pullActionProtobufObjectGenerator = pullActionProtobufObjectGenerator;
  }

  public void appendTimestampToResponse(OutputStream outputStream) throws IOException {
    Issues.IssuesPullQueryTimestamp issuesPullQueryTimestamp = pullActionProtobufObjectGenerator.generateTimestampMessage(system2.now());
    issuesPullQueryTimestamp.writeDelimitedTo(outputStream);
  }

  public void appendIssuesToResponse(List<IssueDto> issueDtos, OutputStream outputStream) {
    try {
      for (IssueDto issueDto : issueDtos) {
        Issues.IssueLite issueLite = pullActionProtobufObjectGenerator.generateIssueMessage(issueDto);
        issueLite.writeDelimitedTo(outputStream);
      }
      outputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void appendClosedIssuesUuidsToResponse(List<String> closedIssuesUuids,
    OutputStream outputStream) throws IOException {
    for (String uuid : closedIssuesUuids) {
      Issues.IssueLite issueLite = pullActionProtobufObjectGenerator.generateClosedIssueMessage(uuid);
      issueLite.writeDelimitedTo(outputStream);
    }
  }

}
