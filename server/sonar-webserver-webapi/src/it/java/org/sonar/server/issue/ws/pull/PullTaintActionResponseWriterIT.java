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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PullTaintActionResponseWriterIT {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final System2 system2 = mock(System2.class);
  private final PullTaintActionProtobufObjectGenerator protobufObjectGenerator = new PullTaintActionProtobufObjectGenerator(db.getDbClient(),
    userSession);

  private final PullActionResponseWriter underTest = new PullActionResponseWriter(system2, protobufObjectGenerator);

  @Before
  public void before() {
    when(system2.now()).thenReturn(1_000_000L);
  }

  @Test
  public void appendIssuesToResponse_outputStreamIsCalledAtLeastOnce() throws IOException {
    OutputStream outputStream = mock(OutputStream.class);
    IssueDto issueDto = new IssueDto();
    issueDto.setFilePath("filePath");
    issueDto.setKee("key");
    issueDto.setStatus("OPEN");
    issueDto.setRuleKey("repo", "rule");
    DbIssues.Locations locations = DbIssues.Locations.newBuilder()
      .setTextRange(range(2, 3))
      .addFlow(newFlow(newLocation(4, 5)))
      .addFlow(newFlow(newLocation(6, 7, "another-component")))
      .build();

    issueDto.setLocations(locations);

    underTest.appendIssuesToResponse(List.of(issueDto), emptyMap(), outputStream);

    verify(outputStream, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
  }

  @Test
  public void appendClosedIssuesToResponse_outputStreamIsCalledAtLeastOnce() throws IOException {
    OutputStream outputStream = mock(OutputStream.class);

    underTest.appendClosedIssuesUuidsToResponse(List.of("uuid", "uuid2"), outputStream);

    verify(outputStream, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
  }

  @Test
  public void appendTimestampToResponse_outputStreamIsCalledAtLeastOnce() throws IOException {
    OutputStream outputStream = mock(OutputStream.class);

    underTest.appendTimestampToResponse(outputStream);

    verify(outputStream, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
  }

  private static DbIssues.Location newLocation(int startLine, int endLine) {
    return DbIssues.Location.newBuilder().setTextRange(range(startLine, endLine)).build();
  }

  private static DbIssues.Location newLocation(int startLine, int endLine, String componentUuid) {
    return DbIssues.Location.newBuilder().setTextRange(range(startLine, endLine)).setComponentId(componentUuid).build();
  }


  private static org.sonar.db.protobuf.DbCommons.TextRange range(int startLine, int endLine) {
    return DbCommons.TextRange.newBuilder().setStartLine(startLine).setEndLine(endLine).build();
  }

  private static DbIssues.Flow newFlow(DbIssues.Location... locations) {
    DbIssues.Flow.Builder builder = DbIssues.Flow.newBuilder();
    Arrays.stream(locations).forEach(builder::addLocation);
    return builder.build();
  }
}
