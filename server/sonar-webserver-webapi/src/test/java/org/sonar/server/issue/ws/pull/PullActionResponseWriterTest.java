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
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.issue.IssueDto;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PullActionResponseWriterTest {

  private final System2 system2 = mock(System2.class);
  private final PullActionProtobufObjectGenerator pullActionProtobufObjectGenerator = new PullActionProtobufObjectGenerator();

  private final PullActionResponseWriter underTest = new PullActionResponseWriter(system2, pullActionProtobufObjectGenerator);

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
}
