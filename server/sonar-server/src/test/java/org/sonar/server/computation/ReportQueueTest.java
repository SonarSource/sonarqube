/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.compute.AnalysisReportDto;
import org.sonar.process.ProcessProperties;
import org.sonar.server.computation.monitoring.CEQueueStatus;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.compute.AnalysisReportDto.Status.PENDING;
import static org.sonar.db.compute.AnalysisReportDto.Status.WORKING;

@Category(DbTests.class)
public class ReportQueueTest {

  static final long NOW = 1_500_000_000_000L;

  System2 system = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DbClient dbClient = db.getDbClient();

  Settings settings = new Settings();
  CEQueueStatus queueStatus = mock(CEQueueStatus.class);
  File dataDir;
  ReportQueue underTest;

  @Before
  public void setUp() throws Exception {
    dataDir = temp.newFolder();
    settings.setProperty(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());
    when(system.now()).thenReturn(NOW);

    underTest = new ReportQueue(dbClient, settings, queueStatus);
  }

  @Test
  public void starts_initializes_count_of_pending_reports() {
    underTest.add("P1", "Project 1", generateData());
    underTest.add("P2", "Project 2", generateData());
    underTest.add("P3", "Project 3", generateData());

    underTest.start();

    verify(queueStatus).initPendingCount(3);
    verifyNoMoreInteractions(queueStatus);
  }

  @Test
  public void add_report_to_queue() {
    // must:
    // 1. insert metadata in db
    // 2. copy report content to directory /data/analysis
    ReportQueue.Item item = underTest.add("P1", "Project 1", generateData());

    assertThat(item).isNotNull();
    assertThat(item.zipFile).isFile().exists().hasContent("some data").hasParent(new File(dataDir, "analysis"));
    assertThat(item.dto.getUuid()).isNotEmpty();
    assertThat(item.dto.getId()).isGreaterThan(0L);

    List<AnalysisReportDto> reports = dbClient.analysisReportDao().selectByProjectKey(db.getSession(), "P1");
    assertThat(reports).hasSize(1);
    AnalysisReportDto report = reports.get(0);

    assertThat(reports).hasSize(1);
    assertThat(report.getStatus()).isEqualTo(PENDING);
    assertThat(report.getProjectKey()).isEqualTo("P1");
    assertThat(report.getProjectName()).isEqualTo("Project 1");
    assertThat(report.getUuid()).isNotEmpty();
    assertThat(report.getId()).isGreaterThan(0L);
    assertThat(report.getCreatedAt()).isEqualTo(NOW);
    assertThat(report.getUpdatedAt()).isEqualTo(NOW);
    assertThat(report.getStartedAt()).isNull();
    assertThat(report.getFinishedAt()).isNull();

    assertThat(FileUtils.listFiles(analysisDir(), new String[]{"zip"}, false)).hasSize(1);
  }

  @Test
  public void pop_pending_items_in_fifo_order() {
    underTest.add("P1", "Project 1", generateData());
    underTest.add("P2", "Project 2", generateData());
    underTest.add("P3", "Project 3", generateData());

    ReportQueue.Item item = underTest.pop();
    assertThat(item.dto.getProjectKey()).isEqualTo("P1");
    assertThat(item.zipFile).exists().isFile().hasExtension("zip");

    // status changed from PENDING to WORKING
    assertThat(item.dto.getStatus()).isEqualTo(WORKING);

    assertThat(underTest.pop().dto.getProjectKey()).isEqualTo("P2");
    assertThat(underTest.pop().dto.getProjectKey()).isEqualTo("P3");

    // queue is empty
    assertThat(underTest.pop()).isNull();

    // items are still in db, but in WORKING status
    List<AnalysisReportDto> reports = underTest.all();
    assertThat(reports).hasSize(3);
    assertThat(reports).extracting("status").containsOnly(WORKING);
  }

  @Test
  public void remove() {
    ReportQueue.Item item = underTest.add("P1", "Project 1", generateData());
    assertThat(db.countRowsOfTable("analysis_reports")).isEqualTo(1);

    underTest.remove(item);
    assertThat(db.countRowsOfTable("analysis_reports")).isEqualTo(0);
    assertThat(item.zipFile).doesNotExist();
  }

  @Test
  public void do_not_pop_corrupted_item() {
    ReportQueue.Item item = underTest.add("P1", "Project 1", generateData());

    // emulate corruption: file is missing on FS
    FileUtils.deleteQuietly(item.zipFile);

    assertThat(underTest.pop()).isNull();

    // table sanitized
    assertThat(db.countRowsOfTable("analysis_reports")).isEqualTo(0);
  }

  @Test
  public void clear() {
    underTest.add("P1", "Project 1", generateData());
    underTest.add("P2", "Project 2", generateData());
    assertThat(analysisDir()).exists().isDirectory();

    underTest.clear();

    assertThat(db.countRowsOfTable("analysis_reports")).isEqualTo(0);
    assertThat(analysisDir()).doesNotExist();
  }

  @Test
  public void clear_do_not_fail_when_directory_do_not_exist() {
    underTest.clear();
    underTest.clear();
  }

  @Test
  public void reset_to_pending_status() {
    // 2 pending
    underTest.add("P1", "Project 1", generateData());
    underTest.add("P2", "Project 2", generateData());

    // pop 1 -> 1 pending and 1 working
    ReportQueue.Item workingItem = underTest.pop();
    assertThat(workingItem.dto.getStatus()).isEqualTo(WORKING);
    assertThat(underTest.all()).extracting("status").contains(PENDING, WORKING);

    underTest.resetToPendingStatus();
    assertThat(underTest.all()).extracting("status").containsOnly(PENDING).hasSize(2);

  }

  private InputStream generateData() {
    return IOUtils.toInputStream("some data");
  }

  private File analysisDir() {
    return new File(dataDir, "analysis");
  }
}
