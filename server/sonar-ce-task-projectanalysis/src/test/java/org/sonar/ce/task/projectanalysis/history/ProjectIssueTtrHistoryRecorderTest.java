/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.history;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.FixedIssueForHistoryRepository;
import org.sonarsource.history.model.FixedIssueForHistory;
import org.sonarsource.history.server.service.IssueTtrHistoryRecordingService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectIssueTtrHistoryRecorderTest {

  private final FixedIssueForHistoryRepository fixedIssueForHistoryRepository = mock();
  private final IssueTtrHistoryRecordingService issueTtrHistoryRecordingService = mock();

  private final ProjectIssueTtrHistoryRecorder underTest = new ProjectIssueTtrHistoryRecorder(fixedIssueForHistoryRepository, issueTtrHistoryRecordingService);

  @Test
  void recordTtrHistory_shouldPassFixedIssuesToHistoryRecordingService() {
    Set<FixedIssueForHistory> fixedIssues = Set.of(
      new FixedIssueForHistory(
        "issueUuid",
        "projectUuid",
        123L,
        456L,
        678L,
        "bug",
        "branchUuid",
        "issueSeverity",
        Map.of("SECURITY", "MAJOR"),
        "OPEN"
      )
    );
    when(fixedIssueForHistoryRepository.getFixedIssues()).thenReturn(fixedIssues);

    underTest.recordTtrHistory("entityUuid");

    verify(issueTtrHistoryRecordingService).recordFixedIssues(fixedIssues);
  }

}
