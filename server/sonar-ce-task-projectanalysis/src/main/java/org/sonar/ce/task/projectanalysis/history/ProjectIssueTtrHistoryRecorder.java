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

import org.sonar.ce.task.projectanalysis.issue.fixedissues.FixedIssueForHistoryRepository;
import org.sonarsource.history.server.service.IssueResolutionHistoryRecordingService;

public class ProjectIssueTtrHistoryRecorder implements IssueTtrHistoryRecorder {

  private final FixedIssueForHistoryRepository fixedIssueForHistoryRepository;
  private final IssueResolutionHistoryRecordingService issueResolutionHistoryRecordingService;

  public ProjectIssueTtrHistoryRecorder(FixedIssueForHistoryRepository fixedIssueForHistoryRepository,
    IssueResolutionHistoryRecordingService issueResolutionHistoryRecordingService) {
    this.fixedIssueForHistoryRepository = fixedIssueForHistoryRepository;
    this.issueResolutionHistoryRecordingService = issueResolutionHistoryRecordingService;
  }

  @Override
  public void recordTtrHistory(String entityUuid) {
    issueResolutionHistoryRecordingService.recordFixedIssues(fixedIssueForHistoryRepository.getFixedIssues());
  }
}
