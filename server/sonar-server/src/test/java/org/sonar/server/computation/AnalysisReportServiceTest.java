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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.protocol.output.resource.ReportComponent;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.db.DbClient;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AnalysisReportServiceTest {
  private AnalysisReportService sut;

  private IssueStorage issueStorage;
  private DbClient dbClient;

  @Before
  public void before() throws Exception {
    dbClient = mock(DbClient.class);
    issueStorage = new FakeIssueStorage();
    ComputeEngineIssueStorageFactory issueStorageFactory = mock(ComputeEngineIssueStorageFactory.class);
    when(issueStorageFactory.newComputeEngineIssueStorage(any(ComponentDto.class))).thenReturn(issueStorage);
    sut = new AnalysisReportService(dbClient, issueStorageFactory);
  }

  @Test
  public void call_dao_to_decompress_report() throws Exception {
    AnalysisReportDao dao = mock(AnalysisReportDao.class);
    when(dao.getDecompressedReport(any(DbSession.class), anyLong())).thenReturn(mock(File.class));
    when(dbClient.analysisReportDao()).thenReturn(dao);
    AnalysisReportDto report = AnalysisReportDto.newForTests(123L);
    ComputeEngineContext context = new ComputeEngineContext(report, mock(ComponentDto.class));

    sut.decompress(mock(DbSession.class), context);

    verify(dao).getDecompressedReport(any(DbSession.class), eq(123L));
  }

  @Test
  public void clean_null_directory_does_not_throw_any_exception() throws Exception {
    sut.deleteDirectory(null);
  }

  @Test
  public void clean_temp_folder() throws Exception {
    File origin = new File(getClass().getResource("/org/sonar/server/computation/AnalysisReportServiceTest/report-folder").getFile());
    File destination = new File("target/tmp/report-folder-to-delete");
    FileUtils.copyDirectory(origin, destination);
    assertThat(destination.exists()).isTrue();

    sut.deleteDirectory(destination);

    assertThat(destination.exists()).isFalse();
  }

  @Test
  public void load_resources() throws Exception {
    ComputeEngineContext context = new ComputeEngineContext(mock(AnalysisReportDto.class), mock(ComponentDto.class));
    context.setReportDirectory(new File(getClass().getResource("/org/sonar/server/computation/AnalysisReportServiceTest/report-folder").getFile()));

    sut.loadResources(context);

    assertThat(context.getComponents()).hasSize(4);
  }

  @Test
  public void save_issues() throws Exception {
    ComputeEngineContext context = new FakeComputeEngineContext();
    context.setReportDirectory(new File(getClass().getResource("/org/sonar/server/computation/AnalysisReportServiceTest/report-folder").getFile()));

    sut.saveIssues(context);

    assertThat(((FakeIssueStorage) issueStorage).issues).hasSize(6);
  }

  private static class FakeIssueStorage extends IssueStorage {

    public List<DefaultIssue> issues = null;

    protected FakeIssueStorage() {
      super(mock(MyBatis.class), mock(RuleFinder.class));
    }

    @Override
    public void save(Iterable<DefaultIssue> issues) {
      this.issues = Lists.newArrayList(issues);
    }

    @Override
    protected void doInsert(DbSession batchSession, long now, DefaultIssue issue) {

    }

    @Override
    protected void doUpdate(DbSession batchSession, long now, DefaultIssue issue) {

    }
  }

  private static class FakeComputeEngineContext extends ComputeEngineContext {

    public FakeComputeEngineContext() {
      super(mock(AnalysisReportDto.class), mock(ComponentDto.class));
    }

    @Override
    public ReportComponent getComponentByBatchId(Long batchId) {
      return new ReportComponent()
        .setBatchId(123)
        .setId(456);
    }
  }

}
