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

import org.junit.jupiter.api.Test;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.FixedIssueForHistoryRepository;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.FixedIssueVisitor;
import org.sonarsource.history.server.db.HistoryDbClient;
import org.sonarsource.history.server.db.repository.IssueCountDimensionsRepository;
import org.sonarsource.history.server.db.repository.IssueCountHistoryRepository;
import org.sonarsource.history.server.db.repository.IssueTtrHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureKeyMappingRepository;
import org.sonarsource.history.server.service.HistoryPurgeService;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;
import org.sonarsource.history.server.service.IssueTtrHistoryRecordingService;
import org.sonarsource.history.server.service.MeasuresHistoryRecordingService;
import org.sonarsource.history.server.service.RetryableOrderedBatchWriter;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryReportAnalysisComponentProviderTest {

  @Test
  void getComponents_shouldReturnDelegateAndRecordingComponentsInAnyOrder() {
    assertThat(new HistoryReportAnalysisComponentProvider().getComponents())
      .containsExactlyInAnyOrder(
        RecordHistoryDelegateImpl.class,
        HistoryDbClient.class,
        IssueCountDimensionsRepository.class,
        IssueCountHistoryRepository.class,
        IssueTtrHistoryRepository.class,
        MeasureHistoryRepository.class,
        MeasureKeyMappingRepository.class,
        IssueCountHistoryRecordingService.class,
        IssueTtrHistoryRecordingService.class,
        MeasuresHistoryRecordingService.class,
        RetryableOrderedBatchWriter.class,
        HistoryPurgeService.class,
        FixedIssueVisitor.class,
        FixedIssueForHistoryRepository.class,
        ProjectIssueTtrHistoryRecorder.class);
  }
}
