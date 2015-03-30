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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.process.ProcessProperties;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.WORKING;

@Category(DbTests.class)
public class ReportQueueTest {

  static final long NOW = 1_500_000_000_000L;

  @Rule
  public DbTester db = new DbTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Settings settings = new Settings();
  File dataDir;
  System2 system = mock(System2.class);
  ReportQueue sut;

  @Before
  public void setUp() throws Exception {
    dataDir = temp.newFolder();
    settings.setProperty(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());
    when(system.now()).thenReturn(NOW);

    DbClient dbClient = new DbClient(db.database(), db.myBatis(), new ComponentDao(), new AnalysisReportDao(system));
    sut = new ReportQueue(dbClient, settings);

    try (DbSession session = dbClient.openSession(false)) {
      dbClient.componentDao().insert(session, ComponentTesting.newProjectDto().setKey("P1"));
      dbClient.componentDao().insert(session, ComponentTesting.newProjectDto().setKey("P2"));
      dbClient.componentDao().insert(session, ComponentTesting.newProjectDto().setKey("P3"));
      session.commit();
    }
  }

  @Test
  public void add_report_to_queue() throws Exception {
    // must:
    // 1. insert metadata in db
    // 2. copy report content to directory /data/analysis
    ReportQueue.Item item = sut.add("P1", generateData());

    assertThat(item).isNotNull();
    assertThat(item.zipFile).isFile().exists().hasContent("some data").hasParent(new File(dataDir, "analysis"));
    assertThat(item.dto.getUuid()).isNotEmpty();
    assertThat(item.dto.getId()).isGreaterThan(0L);

    List<AnalysisReportDto> reports = sut.selectByProjectKey("P1");
    assertThat(reports).hasSize(1);
    AnalysisReportDto report = reports.get(0);

    assertThat(reports).hasSize(1);
    assertThat(report.getStatus()).isEqualTo(PENDING);
    assertThat(report.getProjectKey()).isEqualTo("P1");
    assertThat(report.getUuid()).isNotEmpty();
    assertThat(report.getId()).isGreaterThan(0L);
    assertThat(report.getCreatedAt()).isEqualTo(NOW);
    assertThat(report.getUpdatedAt()).isEqualTo(NOW);
    assertThat(report.getStartedAt()).isNull();
    assertThat(report.getFinishedAt()).isNull();

    assertThat(FileUtils.listFiles(analysisDir(), new String[] {"zip"}, false)).hasSize(1);
  }

  @Test
  public void find_by_project_key() throws Exception {
    sut.add("P1", generateData());
    assertThat(sut.selectByProjectKey("P1")).hasSize(1).extracting("projectKey").containsExactly("P1");
    assertThat(sut.selectByProjectKey("P2")).isEmpty();
  }

  @Test
  public void pop_pending_items_in_fifo_order() {
    sut.add("P1", generateData());
    sut.add("P2", generateData());
    sut.add("P3", generateData());

    ReportQueue.Item item = sut.pop();
    assertThat(item.dto.getProjectKey()).isEqualTo("P1");
    assertThat(item.zipFile).exists().isFile().hasExtension("zip");

    // status changed from PENDING to WORKING
    assertThat(item.dto.getStatus()).isEqualTo(WORKING);

    assertThat(sut.pop().dto.getProjectKey()).isEqualTo("P2");
    assertThat(sut.pop().dto.getProjectKey()).isEqualTo("P3");

    // queue is empty
    assertThat(sut.pop()).isNull();

    // items are still in db, but in WORKING status
    List<AnalysisReportDto> reports = sut.all();
    assertThat(reports).hasSize(3);
    assertThat(reports).extracting("status").containsOnly(WORKING);
  }

  @Test
  public void remove() {
    ReportQueue.Item item = sut.add("P1", generateData());
    assertThat(db.countRowsOfTable("analysis_reports")).isEqualTo(1);

    sut.remove(item);
    assertThat(db.countRowsOfTable("analysis_reports")).isEqualTo(0);
    assertThat(item.zipFile).doesNotExist();
  }

  @Test
  public void do_not_pop_corrupted_item() throws Exception {
    ReportQueue.Item item = sut.add("P1", generateData());

    // emulate corruption: file is missing on FS
    FileUtils.deleteQuietly(item.zipFile);

    assertThat(sut.pop()).isNull();

    // table sanitized
    assertThat(db.countRowsOfTable("analysis_reports")).isEqualTo(0);
  }

  @Test
  public void clear() throws Exception {
    sut.add("P1", generateData());
    sut.add("P2", generateData());
    assertThat(analysisDir()).exists().isDirectory();

    sut.clear();

    assertThat(db.countRowsOfTable("analysis_reports")).isEqualTo(0);
    assertThat(analysisDir()).doesNotExist();
  }

  @Test
  public void clear_do_not_fail_when_directory_do_not_exist() throws Exception {
    sut.clear();
    sut.clear();
  }

  @Test(expected = IllegalStateException.class)
  public void add_on_non_existent_project() throws Exception {
    sut.add("UNKNOWN_PROJECT_KEY", generateData());
  }

  @Test
  public void reset_to_pending_status() throws Exception {
    // 2 pending
    sut.add("P1", generateData());
    sut.add("P2", generateData());

    // pop 1 -> 1 pending and 1 working
    ReportQueue.Item workingItem = sut.pop();
    assertThat(workingItem.dto.getStatus()).isEqualTo(WORKING);
    assertThat(sut.all()).extracting("status").contains(PENDING, WORKING);

    sut.resetToPendingStatus();
    assertThat(sut.all()).extracting("status").containsOnly(PENDING).hasSize(2);

  }

  private InputStream generateData() {
    return IOUtils.toInputStream("some data");
  }

  private File analysisDir() {
    return new File(dataDir, "analysis");
  }
}
