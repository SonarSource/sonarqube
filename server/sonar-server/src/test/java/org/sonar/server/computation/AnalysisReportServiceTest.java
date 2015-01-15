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
///*
// * SonarQube, open source software quality management tool.
// * Copyright (C) 2008-2014 SonarSource
// * mailto:contact AT sonarsource DOT com
// *
// * SonarQube is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 3 of the License, or (at your option) any later version.
// *
// * SonarQube is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
// */
//
//package org.sonar.server.computation;
//
//import com.google.common.collect.Lists;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.sonar.api.issue.internal.DefaultIssue;
//import org.sonar.api.rules.RuleFinder;
//import org.sonar.batch.protocol.output.component.ReportComponent;
//import org.sonar.core.component.ComponentDto;
//import org.sonar.core.computation.db.AnalysisReportDto;
//import org.sonar.core.issue.db.IssueStorage;
//import org.sonar.core.persistence.DbSession;
//import org.sonar.core.persistence.MyBatis;
//
//import java.io.File;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Matchers.any;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//public class AnalysisReportServiceTest {
//  private AnalysisReportService sut;
//
//  private IssueStorage issueStorage;
//
//  @Before
//  public void before() throws Exception {
//    issueStorage = new FakeIssueStorage();
//    ComputeEngineIssueStorageFactory issueStorageFactory = mock(ComputeEngineIssueStorageFactory.class);
//    when(issueStorageFactory.newComputeEngineIssueStorage(any(ComponentDto.class))).thenReturn(issueStorage);
//    sut = new AnalysisReportService(issueStorageFactory);
//  }
//
//  @Test
//  public void load_resources() throws Exception {
//    File dir = new File(getClass().getResource("/org/sonar/server/computation/AnalysisReportServiceTest/report-folder").getFile());
//    ComputationContext context = new ComputationContext(mock(AnalysisReportDto.class), mock(ComponentDto.class), dir);
//
//    sut.initComponents(context);
//
//    assertThat(context.getComponents()).hasSize(4);
//  }
//
//  @Test
//  @Ignore("Temporarily ignored")
//  public void save_issues() throws Exception {
//    File dir = new File(getClass().getResource("/org/sonar/server/computation/AnalysisReportServiceTest/report-folder").getFile());
//    ComputationContext context = new FakeComputationContext(dir);
//
//    sut.saveIssues(context);
//
//    assertThat(((FakeIssueStorage) issueStorage).issues).hasSize(6);
//  }
//
//  private static class FakeIssueStorage extends IssueStorage {
//
//    public List<DefaultIssue> issues = null;
//
//    protected FakeIssueStorage() {
//      super(mock(MyBatis.class), mock(RuleFinder.class));
//    }
//
//    @Override
//    public void save(Iterable<DefaultIssue> issues) {
//      this.issues = Lists.newArrayList(issues);
//    }
//
//    @Override
//    protected void doInsert(DbSession batchSession, long now, DefaultIssue issue) {
//
//    }
//
//    @Override
//    protected void doUpdate(DbSession batchSession, long now, DefaultIssue issue) {
//
//    }
//  }
//
//  private static class FakeComputationContext extends ComputationContext {
//
//    public FakeComputationContext(File reportDir) {
//      super(mock(AnalysisReportDto.class), mock(ComponentDto.class), reportDir);
//    }
//
//    @Override
//    public ReportComponent getComponentByBatchId(Long batchId) {
//      return new ReportComponent()
//        .setBatchId(123)
//        .setId(456);
//    }
//  }
//
//}
